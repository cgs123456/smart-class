# P2-2: AI 个性化学习路径 + 知识图谱可视化设计文档

## 1. 业务目标

### 核心价值
| 用户痛点 | 解决方案 | 量化目标 |
|----------|----------|----------|
| 不知道学什么、按什么顺序学 | **AI 生成个性化学习路径** | 路径采纳率 > 60%，完课率提升 25% |
| 知识点孤立，缺乏关联认知 | **知识图谱可视化探索** | 图谱日活渗透 > 15%，关联点击率 > 30% |
| 学习进度无反馈，动力不足 | **动态难度调整 + 成就可视化** | 留存 D7/D30 提升 10%/15% |

---

## 2. 总体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      学习智能中台 (Learning Intelligence)        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  学习者画像  │  │  知识图谱    │  │  推荐/路径   │          │
│  │  引擎        │──│  构建/查询   │──│  生成引擎    │          │
│  └──────────────┘  └──────────────┘  └──────────────┘          │
│        │                 │                  │                   │
│        ▼                 ▼                  ▼                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    数据层 (统一存储)                       │  │
│  │  PostgreSQL (用户/进度) + Neo4j (图谱) + Milvus (向量)    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│  移动端 H5    │    │  管理后台     │    │  第三方对接   │
│  (学习路径/   │    │  (图谱编辑/   │    │  (API/SSO/   │
│   图谱探索)   │    │   课程映射)   │    │   LTI)        │
└───────────────┘    └───────────────┘    └───────────────┘
```

---

## 3. 核心模块设计

### 3.1 学习者画像引擎

#### 多维画像模型
```java
@Entity
@Table(name = "learner_profile")
public class LearnerProfile {
    
    @Id private Long userId;
    
    // 静态画像
    @Embedded private Demographics demographics;      // 年龄/学历/职业/目标岗位
    @Embedded private LearningGoals goals;            // 短期/长期目标、偏好方向
    
    // 动态画像 (实时更新)
    @Embedded private KnowledgeState knowledgeState;  // 概念掌握度向量 (0-1)
    @Embedded private BehaviorFeatures behavior;      // 学习时段/频次/设备/完成率
    @Embedded private CognitiveTraits cognitive;      // 工作记忆/注意力/偏好模态
    
    // 向量嵌入 (用于相似学习者检索)
    @Column(columnDefinition = "vector(256)") 
    private float[] profileEmbedding;
    
    private LocalDateTime updatedAt;
}
```

#### 画像更新流水线
```
事件流 (Kafka/Pulsar) → 流处理 (Flink/Spark Streaming) → 画像更新
├── 学习行为事件: start_lesson, complete_exercise, ask_ai, search, pause, replay
├── 评估事件: quiz_submit, exam_result, self_assessment
├── 社交事件: discuss, share, follow, help_other
└── 画像特征:
    ├── 掌握度: BKT (Bayesian Knowledge Tracing) / DKT (Deep Knowledge Tracing)
    ├── 遗忘曲线: Ebbinghaus 模型参数个性化拟合
    ├── 偏好模态: 视频/文本/练习/项目 完成率加权
    └── 认知负荷: 平均会话时长、错误率、求助频次
```

### 3.2 知识图谱构建

#### 图谱 Schema (Neo4j)
```cypher
// 节点类型
(:Concept {id, name, alias, description, difficulty: 1-5, 
           estimatedHours, prerequisites[], tags[], embedding})
(:Course {id, title, cover, level, duration, concepts[]})
(:Chapter {id, title, order, concepts[]})
(:Lesson {id, title, type: VIDEO/TEXT/LAB/QUIZ, concepts[], resources[]})
(:Skill {id, name, category, marketDemand, relatedConcepts[]})
(:CareerPath {id, title, description, requiredSkills[]})

// 关系类型
(:Concept)-[:PREREQUISITE {weight: 0.8, evidence: "curriculum"}]->(:Concept)
(:Concept)-[:RELATED_TO {weight: 0.5, type: "similar|part_of|application"}]->(:Concept)
(:Course)-[:COVERS {depth: "intro|core|advanced", order: 1}]->(:Concept)
(:Chapter)-[:INCLUDES]->(:Concept)
(:Lesson)-[:TEACHES {masteryTarget: 0.8}]->(:Concept)
(:Skill)-[:REQUIRES {importance: "must|nice"}]->(:Concept)
(:CareerPath)-[:INCLUDES_SKILL {priority: 1-5}]->(:Skill)
```

#### 图谱构建流水线
```java
@Service
public class KnowledgeGraphBuilder {
    
    // 1. 从课程大纲/教材自动抽取概念
    public void extractFromCurriculum(Course course) {
        // LLM + Prompt Engineering 抽取概念层级
        // 结构化输出 -> 入库 Neo4j
    }
    
    // 2. 从文档/视频字幕抽取细粒度概念
    public void extractFromContent(Document doc) {
        // NER + 关系抽取 (BERT-based) -> 概念候选 -> 专家审核/自动入库
    }
    
    // 3. 前置依赖推理
    public void inferPrerequisites() {
        // 基于课程顺序 + 专家规则 + 学习数据 (先修通过率相关性)
        // PageRank / Graph Neural Network 学习边权重
    }
    
    // 4. 嵌入向量生成 (用于语义检索/推荐)
    public void generateEmbeddings() {
        // GraphSAGE / Node2Vec / BGE-M3 对节点编码
        // 存入 Milvus + Neo4j 属性
    }
}
```

### 3.3 个性化学习路径生成

#### 算法框架
```
输入: LearnerProfile + KnowledgeGraph + 目标
     │
     ▼
┌────────────────────────────────────────┐
│  候选集生成                             │
│  - 目标技能/职业路径所需概念集合        │
│  - 减去已掌握概念 (knowledgeState > θ) │
│  - 剩余概念拓扑排序 (前置依赖约束)      │
└────────────────┬───────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────┐
│  路径规划 (多目标优化)                   │
│  目标函数:                              │
│   Max Σ 掌握度收益 * 权重                │
│   Min 总学习时长                         │
│   Min 认知跳跃 (前置依赖跨度)            │
│   Max 兴趣匹配度                         │
│                                         │
│  算法:                                  │
│   - 约束规划 (OR-Tools CP-SAT)          │
│   - 强化学习 (DQN/PPO, 状态=知识向量)   │
│   - 启发式: 贪心 + 局部搜索             │
└────────────────┬───────────────────────┘
                 │
                 ▼
┌────────────────────────────────────────┐
│  路径输出 & 解释                         │
│  - 节点: Concept + 推荐 Lesson/资源     │
│  - 边: 依赖关系 + 预估时长 + 难度        │
│  - 解释: "建议先学 X，因为它是 Y 的基础" │
└────────────────────────────────────────┘
```

#### API 契约
```typescript
// 前端消费接口
interface LearningPathRequest {
  userId: string;
  goal: LearningGoal;           // { type: "SKILL"|"CAREER"|"EXAM", targetId: string }
  constraints?: {
    maxHoursPerWeek: number;
    preferredModality: "VIDEO"|"TEXT"|"PRACTICE";
    avoidConcepts?: string[];   // 已知擅长/不想学
  };
}

interface LearningPathResponse {
  pathId: string;
  nodes: PathNode[];
  edges: PathEdge[];
  metadata: {
    estimatedTotalHours: number;
    difficultyCurve: number[];  // 难度随时间变化
    milestones: Milestone[];    // 关键检查点
    confidence: number;         // 0-1
  };
  explanation: string;          // 自然语言解释
}

interface PathNode {
  conceptId: string;
  conceptName: string;
  masteryTarget: number;        // 目标掌握度 0-1
  estimatedHours: number;
  recommendedResources: ResourceRef[];  // 具体课程/视频/练习
  prerequisites: string[];      // 本节点前置概念
  isMilestone: boolean;
}

interface PathEdge {
  from: string;
  to: string;
  relation: "PREREQUISITE" | "REINFORCE" | "EXTEND";
  weight: number;
}
```

### 3.4 知识图谱可视化前端

#### 技术选型
| 需求 | 方案 |
|------|------|
| 大规模节点渲染 (1000+) | **Graphin (基于 G6) / Cytoscape.js / Sigma.js** |
| 力导向/层次/同心圆布局 | Graphin 内置多布局切换 |
| 交互: 缩放/平移/搜索/高亮/钻取 | Graphin 内置 + 自定义插件 |
| 图谱编辑 (后台) | Graphin Editor / 自定义节点面板 |
| 3D 可视化 (可选) | **Three.js + Graphology** |

#### 核心交互设计
```vue
<!-- 组件: KnowledgeGraphExplorer.vue -->
<template>
  <div class="graph-container" ref="container">
    <Graphin
      :data="graphData"
      :layout="currentLayout"
      :renderer="renderer"
      :plugins="plugins"
      @node:click="onNodeClick"
      @edge:click="onEdgeClick"
      @viewport:change="onViewportChange"
    >
      <!-- 自定义节点: 概念卡片 -->
      <template #node="{ data }">
        <ConceptNode :data="data" :highlighted="highlightedIds.has(data.id)" />
      </template>
      
      <!-- 小地图导航 -->
      <Minimap />
      
      <!-- 侧边栏详情 -->
      <SidebarPanel v-if="selectedNode" :node="selectedNode" @close="selectedNode=null" />
    </Graphin>
    
    <!-- 控制面板 -->
    <GraphControls
      :layout="currentLayout"
      @layout-change="currentLayout = $event"
      @search="onSearch"
      @filter="onFilter"
    />
  </div>
</template>
```

---

## 4. 动态自适应调整

### 4.1 实时难度调整
```java
@Service
public class AdaptiveDifficultyEngine {
    
    public DifficultyAdjustment evaluate(LearningSession session) {
        // 实时特征
        double recentAccuracy = session.getRecentAccuracy(5); // 最近 5 题正确率
        double avgResponseTime = session.getAvgResponseTime();
        int hintCount = session.getHintCount();
        int retryCount = session.getRetryCount();
        
        // 规则引擎 + 模型预测
        if (recentAccuracy > 0.9 && avgResponseTime < threshold) {
            return DifficultyAdjustment.INCREASE;  // 太简单，加大难度
        }
        if (recentAccuracy < 0.5 || hintCount > 2) {
            return DifficultyAdjustment.DECREASE;  // 太难，降低难度/插入支架
        }
        if (retryCount > 3) {
            return DifficultyAdjustment.INSERT_SCAFFOLDING; // 插入微课/示例
        }
        return DifficultyAdjustment.MAINTAIN;
    }
}
```

### 4.2 遗忘干预
```java
@Scheduled(cron = "0 0 8 * * *") // 每天早 8 点
public void generateReviewPlan() {
    // 基于 Ebbinghaus 遗忘曲线 + 个性化参数
    // 为每用户生成今日复习清单
    // 推送到消息中心 / 学习首页 "今日复习" 区块
}
```

---

## 5. 管理后台功能

| 功能 | 说明 |
|------|------|
| **图谱编辑器** | 可视化增删改概念/关系、批量导入 (Excel/CSV)、版本管理 |
| **课程-概念映射** | 拖拽配置 Lesson 覆盖哪些 Concept、掌握度目标、前置依赖 |
| **路径模版管理** | 预置经典路径 (如 "Java 后端工程师进阶")、参数化模版 |
| **A/B 实验配置** | 算法版本分流、目标函数权重调整、灰度发布 |
| **效果看板** | 路径完成率、概念掌握度分布、图谱探索热力图、留存对比 |

---

## 6. 数据隐私与合规

- 学习画像数据属于敏感个人信息，**严格租户隔离 (P1-3)**
- 图谱结构去标识化，不包含用户 ID
- 模型训练数据脱敏、最小化采集
- 提供用户数据导出/删除接口 (GDPR/PIPL 合规)

---

## 7. 迁移计划

| 阶段 | 时间 | 交付物 |
|------|------|--------|
| **Phase 1** | Week 1-2 | Neo4j 集群部署、图谱 Schema 设计、种子数据导入 (500+ 概念) |
| **Phase 2** | Week 3-4 | 画像引擎 (BKT/DKT)、概念掌握度实时更新管道 |
| **Phase 3** | Week 5-6 | 路径生成算法 (OR-Tools + 启发式)、API 对接前端 |
| **Phase 4** | Week 7 | 图谱可视化前端 (Graphin)、交互钻取、搜索/筛选 |
| **Phase 5** | Week 8 | 自适应调整、遗忘干预、A/B 实验上线 |
| **Phase 6** | Week 9 | 管理后台、效果评估报告、文档交付 |

---

## 8. 工作量预估

| 模块 | 人天 |
|------|------|
| Neo4j 基建 + 种子数据 | 5 |
| 画像引擎 (流处理 + 模型) | 10 |
| 知识图谱构建管道 (LLM 抽取 + 依赖推理) | 10 |
| 路径生成算法 + API | 8 |
| 图谱可视化前端 (Graphin 定制) | 8 |
| 自适应调整 + 遗忘干预 | 5 |
| 管理后台 + 看板 | 5 |
| 评估 + A/B 测试 | 4 |
| **合计** | **~55 人天 (约 8-10 周, 3 人)** |

---

## 9. 依赖关系

| 依赖 | 说明 |
|------|------|
| **P1-1 Dubbo3** | 画像服务/图谱服务/路径服务作为微服务注册发现 |
| **P1-2 Gateway** | 统一鉴权、限流、租户透传 |
| **P1-3 多租户** | 图谱/画像/路径严格按租户隔离 |
| **P2-1 RAG** | 学习路径节点可关联 RAG 检索的知识片段 |
| **P0-2 Observability** | 全链路 Trace: 用户行为 → 画像更新 → 路径重算 → 前端渲染 |

---

## 10. 关键风险

| 风险 | 缓解 |
|------|------|
| **冷启动: 新用户无画像** | 基于人口统计/目标的默认画像 + 探索式推荐 (Thompson Sampling) |
| **图谱质量依赖专家** | 半自动化: LLM 抽取 → 专家审核 → 入库；建立激励机制 |
| **路径生成延迟高** | 预计算常见目标路径模版 + 实时微调；缓存热门路径 |
| **前端大图渲染卡顿** | 分层渲染、视口裁剪、WebWorker 计算布局、Canvas/WebGL |
| **算法黑盒不可解释** | 每步决策生成自然语言解释、前端展示"为什么推荐这个" |