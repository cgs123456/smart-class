# P2-1: RAG 混合检索 + Rerank 重排序设计文档

## 1. 业务背景

### 当前痛点
| 现状 | 问题 |
|------|------|
| 纯向量检索 (Dify 内置) | 语义相似但非精确匹配 (如"Java" vs "JavaScript") |
| 无关键词精确召回 | 专业术语、代码片段、错误码、版本号召回率低 |
| 单一 Top-K 截断 | 相关文档被噪声挤出，答案质量不稳定 |
| 无重排序 | 检索分数 ≠ 答案相关性，LLM 易产生幻觉 |

### 目标效果
- **召回率提升**: 关键词精确匹配 + 语义模糊匹配双管齐下
- **精度提升**: Cross-Encoder Rerank 将 Top-20 → Top-5，噪声过滤 80%+
- **回答质量**: 引用片段更精准，幻觉率降低 50%+

---

## 2. 架构设计

### 2.1 总体流程
```
用户问题
    │
    ▼
┌─────────────────────────────────────┐
│     Query Rewriting (可选)          │
│  - 多查询扩展 (HyDE/Step-back)      │
│  - 实体识别/关键词提取              │
└──────────────┬──────────────────────┘
               │
    ┌──────────┴──────────┐
    ▼                     ▼
┌─────────┐         ┌─────────┐
│ Vector  │         │Keyword  │
│ Search  │         │ Search  │
│ (Dense) │         │ (Sparse)│
└────┬────┘         └────┬────┘
     │                   │
     └────────┬──────────┘
              ▼
    ┌─────────────────────┐
    │  Hybrid Fusion      │
    │  (RRF / Weighted)   │
    └──────────┬──────────┘
               ▼
    ┌─────────────────────┐
    │   Rerank (Cross-Enc)│
    │  Top-50 → Top-5     │
    └──────────┬──────────┘
               ▼
    ┌─────────────────────┐
    │   Context Builder   │
    │  - 片段去重/截断    │
    │  - 引用标记注入     │
    └──────────┬──────────┘
               ▼
         LLM 生成答案
```

### 2.2 组件选型

| 组件 | 方案 | 理由 |
|------|------|------|
| **向量数据库** | **Milvus 2.4+ / PostgreSQL + pgvector** | Milvus 企业级、过滤器支持；pgvector 运维简单、ACID |
| **稀疏检索** | **Elasticsearch 8+ (BM25)** | 已有 ES 集群、中文分词成熟、混合搜索原生支持 |
| **Embedding 模型** | **BGE-M3 / text-embedding-v3** | BGE-M3 支持稠密+稀疏+多向量、中文 SOTA |
| **Rerank 模型** | **BGE-Reranker-v2-m3 / Cohere Rerank 3.5** | Cross-Encoder 精度高、支持中英、可私有化部署 |
| **融合算法** | **RRF (Reciprocal Rank Fusion) + 权重** | 无需调参、理论保证、工业界标准 |
| **编排框架** | **LangChain4j / Spring AI / 自研 Pipeline** | Spring AI 原生集成、Java 生态友好 |

---

## 3. 核心实现细节

### 3.1 混合检索融合算法

```java
@Service
public class HybridRetriever {
    
    private final VectorStore vectorStore;      // Milvus/pgvector
    private final ElasticsearchClient esClient; // ES
    private final RerankClient rerankClient;    // BGE-Reranker
    
    // RRF 融合: score = Σ 1 / (k + rank_i)
    // k=60 经验值，论文推荐
    public List<Document> hybridSearch(String query, SearchOptions options) {
        // 1. 向量检索 (Top-K*3)
        List<Document> vectorDocs = vectorStore.similaritySearch(
            Query.builder()
                .queryText(query)
                .topK(options.getTopK() * 3)
                .filter(options.getFilter()) // 租户/权限过滤
                .build()
        );
        
        // 2. 关键词检索 (Top-K*3)
        List<Document> keywordDocs = keywordSearch(query, options.getTopK() * 3);
        
        // 3. RRF 融合
        Map<String, Document> fused = new LinkedHashMap<>();
        fuseWithRRF(vectorDocs, fused, 60);
        fuseWithRRF(keywordDocs, fused, 60);
        
        // 4. 取 Top-N 送 Rerank
        List<Document> candidates = fused.values().stream()
            .limit(options.getRerankTopN() ?: 50)
            .toList();
        
        // 5. Cross-Encoder Rerank
        return rerankClient.rerank(query, candidates, options.getTopK());
    }
    
    private void fuseWithRRF(List<Document> docs, Map<String, Document> fused, int k) {
        for (int i = 0; i < docs.size(); i++) {
            Document doc = docs.get(i);
            String key = doc.getId(); // 唯一标识
            double score = 1.0 / (k + i + 1);
            fused.compute(key, (k, existing) -> 
                existing == null ? doc.withScore(score) 
                                 : existing.withScore(existing.getScore() + score)
            );
        }
    }
}
```

### 3.2 Elasticsearch 混合搜索 (原生支持)

```json
// ES 8.9+ 原生混合搜索查询
POST /course_chunks/_search
{
  "size": 50,
  "query": {
    "hybrid": {
      "queries": [
        {
          "knn": {
            "field": "embedding",
            "query_vector": [...],
            "k": 100,
            "num_candidates": 500
          }
        },
        {
          "bool": {
            "must": [
              { "match": { "content": { "query": "Java 异常处理", "analyzer": "ik_smart" } } }
            ],
            "filter": [
              { "term": { "tenant_id": "edu-beijing" } },
              { "term": { "status": "published" } }
            ]
          }
        }
      ]
    }
  },
  "rank": {
    "rrf": { "window_size": 50, "rank_constant": 60 }
  }
}
```

### 3.3 Rerank 服务部署

```yaml
# docker-compose.rerank.yml
services:
  reranker:
    image: registry.smartclass.cn/bge-reranker-v2-m3:latest
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1
              capabilities: [gpu]
    environment:
      - MODEL_NAME=BAAI/bge-reranker-v2-m3
      - MAX_BATCH_SIZE=32
      - MAX_LENGTH=512
    ports:
      - "8001:8000"  # FastAPI /v1/rerank
```

```java
// Java 客户端
@Component
public class RerankClient {
    
    private final WebClient webClient;
    
    public List<Document> rerank(String query, List<Document> docs, int topK) {
        if (docs.isEmpty()) return List.of();
        
        RerankRequest request = RerankRequest.builder()
            .query(query)
            .documents(docs.stream().map(Document::getContent).toList())
            .topK(topK)
            .returnDocuments(true)
            .build();
        
        RerankResponse response = webClient.post()
            .uri("/v1/rerank")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(RerankResponse.class)
            .block();
        
        // 重新排序并附加分数
        return response.getResults().stream()
            .map(r -> docs.get(r.getIndex()).withScore(r.getRelevanceScore()))
            .toList();
    }
}
```

### 3.4 查询重写

```java
@Component
public class QueryRewriter {
    
    private final ChatModel chatModel;
    
    // HyDE: 让 LLM 生成假设文档，再用假设文档检索
    public List<String> hydeRewrite(String query) {
        String prompt = """
            请根据问题生成一个可能包含答案的假设文档片段：
            问题: {query}
            假设文档:
            """;
        String hypothetical = chatModel.call(prompt.replace("{query}", query));
        return List.of(query, hypothetical); // 原查询 + 假设文档双检索
    }
    
    // Step-back: 抽象出更通用的概念查询
    public List<String> stepBackRewrite(String query) {
        String prompt = """
            请将具体问题抽象为更通用的概念问题：
            具体问题: {query}
            通用问题:
            """;
        String general = chatModel.call(prompt.replace("{query}", query));
        return List.of(query, general);
    }
    
    // 关键词提取 (用于 ES 精确匹配 boost)
    public List<String> extractKeywords(String query) {
        // 可用 jieba / HanLP / LLM 提取
        return keywordExtractor.extract(query, 5);
    }
}
```

---

## 4. 数据建模与索引策略

### 4.1 文档分块策略

```java
public class ChunkingStrategy {
    
    // 语义分块: 保持段落完整性
    public List<Chunk> semanticChunk(String content, String docType) {
        switch (docType) {
            case "CODE" -> return codeChunker.chunk(content);      // 按函数/类分块
            case "MARKDOWN" -> return markdownChunker.chunk(content); // 按标题层级
            case "PDF" -> return layoutAwareChunker.chunk(content);   // 版面感知
            default -> return recursiveChunker.chunk(content);     // 递归字符分块
        }
    }
    
    // 重叠窗口: 保证上下文连贯
    public record Chunk(
        String id,
        String content,
        int tokenCount,
        Map<String, Object> metadata  // docId, title, section, page, tenantId
    ) {}
}
```

### 4.2 索引字段设计

| 字段 | 类型 | 用途 |
|------|------|------|
| `id` | keyword | 主键 |
| `tenant_id` | keyword | 租户隔离 (必选过滤) |
| `doc_id` | keyword | 文档归属 |
| `title` | text (ik_smart) | 标题检索 |
| `content` | text (ik_smart) | 正文检索 |
| `embedding` | dense_vector (1024) | 向量检索 |
| `sparse_embedding` | sparse_vector (可选) | BGE-M3 稀疏向量 |
| `section_path` | keyword | 章节路径 (用于层级过滤) |
| `doc_type` | keyword | 文档类型 (CODE/MD/PDF/PPT) |
| `created_at` | date | 时间过滤 |
| `tags` | keyword | 标签过滤 |

---

## 5. 评估体系

### 5.1 离线评估指标

| 指标 | 定义 | 目标 |
|------|------|------|
| **Recall@K** | 相关文档在 Top-K 中被召回的比例 | Recall@20 > 90% |
| **MRR** | 平均倒数排名 | MRR@10 > 0.7 |
| **NDCG@K** | 归一化折扣累积增益 | NDCG@10 > 0.8 |
| **Rerank Gain** | Rerack 前后 MRR 提升 | +15% ~ +30% |

### 5.2 测试集构建
```
测试集来源:
├── 历史用户真实问答日志 (脱敏采样 1000+)
├── 专家标注黄金集 (问题-相关文档对 500+)
├── 对抗样本 (同义词/缩写/错别字/多义词)
└── 边界案例 (超长上下文/多跳推理/表格/代码)
```

### 5.3 在线 A/B 测试
```
分流策略:
├── Control: 纯向量检索 (Dify 默认)
├── Treatment A: Hybrid (Vector + Keyword) + RRF
├── Treatment B: Hybrid + Rerank
└── Treatment C: Hybrid + Rerank + Query Rewriting

核心指标:
├── 回答采纳率 (用户点击"有用"/复制/继续追问)
├── 会话轮次 (越少越好，说明一次回答解决问题)
├── 负反馈率 (点击"无用"/重新提问)
└── Token 消耗 / 延迟
```

---

## 6. 运维与监控

### 6.1 关键指标大盘
```
检索层:
├── QPS / P50/P99 延迟 (向量/关键词/融合/重排各阶段)
├── 召回数量分布 (各阶段候选数)
├── Rerank 批处理吞吐 (batch size / GPU 利用率)

生成层:
├── 答案 Token 数 / 首 Token 延迟
├── 引用片段覆盖率 (答案中引用的片段占检索片段比例)
└── 幻觉检测率 (自动化/人工抽检)
```

### 6.2 熔断降级策略
```java
@Component
public class RetrievalFallback {
    
    // Rerank 服务不可用 -> 直接用融合分数 Top-K
    // ES 不可用 -> 纯向量检索
    // Milvus 不可用 -> 纯关键词检索
    // 全挂 -> 返回预设兜底回答 / 静态知识库
}
```

---

## 7. 迁移计划

| 阶段 | 时间 | 范围 |
|------|------|------|
| **Phase 1** | Week 1-2 | 基建：Milvus/pgvector 集群、ES 混合搜索索引、Rerank 模型部署 |
| **Phase 2** | Week 3 | 核心 Pipeline：Hybrid Search + RRF + Rerack 集成、单元测试 |
| **Phase 3** | Week 4 | 查询重写、分块优化、元数据过滤、租户隔离 |
| **Phase 4** | Week 5 | 离线评估跑分、A/B 测试上线、监控大盘 |
| **Phase 5** | Week 6 | 调优迭代、文档化、知识库迁移脚本 |

---

## 8. 工作量预估

| 模块 | 人天 |
|------|------|
| 向量/ES 基建部署 | 5 |
| Hybrid Retrieval Pipeline 开发 | 8 |
| Rerank 服务部署 + 客户端 | 5 |
| Query Rewriting + Chunking 优化 | 5 |
| 评估体系 + A/B 测试框架 | 5 |
| 监控/告警/熔断 | 3 |
| **合计** | **~31 人天 (约 5-6 周, 2 人)** |

---

## 9. 依赖关系

| 依赖 | 说明 |
|------|------|
| **P1-1 Dubbo3** | Rerank 服务可注册为微服务，走 Dubbo Triple 调用 |
| **P1-2 Gateway** | 网关层统一限流 Rerank QPS，透传租户 ID |
| **P1-3 多租户** | 检索必须强制过滤 `tenant_id`，防止跨租户数据泄露 |
| **P0-2 Observability** | 全链路 Trace: Query Rewriting → Hybrid Search → Rerank → LLM |

---

## 10. 替代方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **纯向量 + 大 Top-K** | 架构简单、无额外模型 | 噪声大、Token 浪费、精度上限低 | 数据量小、延迟敏感、无 GPU |
| **Hybrid (RRF) 无 Rerank** | 无需 Cross-Encoder、延迟低 | 精度受限于检索器打分质量 | 中等规模、GPU 资源受限 |
| **Hybrid + Rerank (推荐)** | 精度最高、工业界标准 | 需 GPU、增加 50-100ms 延迟 | 核心业务、高质量要求 |
| **Single Vector (BGE-M3 稀疏+稠密)** | 单模型双向量、索引简单 | 稀疏向量精度不如 BM25 | 追求极简架构 |