package com.cgs.smartclassbackendintelligence.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cgs.smartclassbackendmodel.esdao.CourseKnowledgeEsDao;
import com.cgs.smartclassbackendmodel.model.dto.course.CourseKnowledgeEsDTO;
import com.cgs.smartclassbackendmodel.model.entity.Course;
import com.cgs.smartclassbackendmodel.model.entity.CourseChapter;
import com.cgs.smartclassbackendmodel.model.entity.CourseSection;
import com.cgs.smartclassbackendserviceclient.service.CourseFeignClient;
import com.cgs.smartclassbackendintelligence.service.CourseRagService;
import com.cgs.smartclassbackendintelligence.service.EmbeddingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 课程知识 RAG 服务实现
 * 负责课程内容的向量化存储和检索，为 AI 助手提供课程知识上下文
 */
@Service
@Slf4j
public class CourseRagServiceImpl implements CourseRagService {

    @Resource
    private CourseFeignClient courseFeignClient;

    @Resource
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private CourseKnowledgeEsDao courseKnowledgeEsDao;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Value("${rag.enabled:false}")
    private boolean ragEnabled;

    @Value("${rag.top-k:3}")
    private int topK;

    @Value("${rag.max-content-length:2000}")
    private int maxContentLength;

    @Override
    public String retrieveRelevantContent(String query, int topK) {
        if (!ragEnabled || courseKnowledgeEsDao == null || elasticsearchOperations == null || StrUtil.isBlank(query)) {
            return "";
        }
        try {
            List<CourseKnowledgeEsDTO> results = null;

            // 优先使用向量检索
            if (embeddingService.isAvailable()) {
                float[] queryEmbedding = embeddingService.embed(query);
                if (queryEmbedding != null) {
                    results = vectorSearch(queryEmbedding, topK);
                }
            }

            // 向量检索失败或无结果时，使用文本检索兜底
            if (results == null || results.isEmpty()) {
                results = textSearch(query, topK);
            }

            if (results == null || results.isEmpty()) {
                return "";
            }
            return formatResults(results);
        } catch (Exception e) {
            log.warn("RAG 检索失败: {}", e.getMessage());
            return "";
        }
    }

    @Override
    public void syncAllCourseContent() {
        if (!ragEnabled || courseKnowledgeEsDao == null) {
            log.warn("RAG 未启用或 ES 不可用，跳过全量同步");
            return;
        }
        try {
            List<Course> courses = courseFeignClient.listAllCourses();
            List<CourseChapter> chapters = courseFeignClient.listAllChapters();
            List<CourseSection> sections = courseFeignClient.listAllSections();

            List<CourseKnowledgeEsDTO> dtos = new ArrayList<>();
            if (courses != null) {
                for (Course course : courses) {
                    dtos.add(CourseKnowledgeEsDTO.buildFromCourse(course));
                }
            }
            if (chapters != null) {
                for (CourseChapter chapter : chapters) {
                    dtos.add(CourseKnowledgeEsDTO.buildFromChapter(chapter));
                }
            }
            if (sections != null) {
                for (CourseSection section : sections) {
                    dtos.add(CourseKnowledgeEsDTO.buildFromSection(section));
                }
            }

            batchEmbedAndSave(dtos);
            log.info("课程知识全量同步完成，共 {} 条", dtos.size());
        } catch (Exception e) {
            log.warn("课程知识全量同步失败: {}", e.getMessage());
        }
    }

    @Override
    public void syncCourseContent(Long courseId) {
        if (!ragEnabled || courseKnowledgeEsDao == null || courseId == null) {
            return;
        }
        try {
            // 删除旧数据
            courseKnowledgeEsDao.deleteByCourseId(courseId);

            Course course = courseFeignClient.getCourseById(courseId);
            List<CourseChapter> chapters = courseFeignClient.listChapters(courseId);
            List<CourseSection> sections = courseFeignClient.listSections(courseId);

            List<CourseKnowledgeEsDTO> dtos = new ArrayList<>();
            if (course != null) {
                dtos.add(CourseKnowledgeEsDTO.buildFromCourse(course));
            }
            if (chapters != null) {
                for (CourseChapter chapter : chapters) {
                    dtos.add(CourseKnowledgeEsDTO.buildFromChapter(chapter));
                }
            }
            if (sections != null) {
                for (CourseSection section : sections) {
                    dtos.add(CourseKnowledgeEsDTO.buildFromSection(section));
                }
            }

            batchEmbedAndSave(dtos);
            log.info("课程 {} 知识同步完成，共 {} 条", courseId, dtos.size());
        } catch (Exception e) {
            log.warn("课程 {} 知识同步失败: {}", courseId, e.getMessage());
        }
    }

    /**
     * 分批嵌入并保存到 ES
     */
    private void batchEmbedAndSave(List<CourseKnowledgeEsDTO> dtos) {
        if (dtos.isEmpty()) {
            return;
        }
        int batchSize = 100;
        for (int i = 0; i < dtos.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dtos.size());
            List<CourseKnowledgeEsDTO> batch = dtos.subList(i, end);

            // 批量生成向量嵌入
            if (embeddingService.isAvailable()) {
                List<String> texts = new ArrayList<>();
                for (CourseKnowledgeEsDTO dto : batch) {
                    texts.add(dto.getTitle() + " " + dto.getContent());
                }
                List<float[]> embeddings = embeddingService.embedBatch(texts);
                if (embeddings != null && embeddings.size() == batch.size()) {
                    for (int j = 0; j < batch.size(); j++) {
                        batch.get(j).setEmbedding(embeddings.get(j));
                    }
                }
            }

            courseKnowledgeEsDao.saveAll(batch);
        }
    }

    /**
     * 向量检索（基于 cosineSimilarity 的 script_score 查询）
     */
    private List<CourseKnowledgeEsDTO> vectorSearch(float[] queryEmbedding, int topK) {
        try {
            List<Float> vectorList = new ArrayList<>();
            for (float f : queryEmbedding) {
                vectorList.add(f);
            }

            Map<String, Object> params = new HashMap<>();
            params.put("query_vector", vectorList);

            Map<String, Object> script = new HashMap<>();
            script.put("source", "cosineSimilarity(params.query_vector, 'embedding') + 1.0");
            script.put("params", params);

            // 仅检索有 embedding 字段的文档
            Map<String, Object> existsFilter = new HashMap<>();
            existsFilter.put("field", "embedding");
            Map<String, Object> existsQuery = new HashMap<>();
            existsQuery.put("exists", existsFilter);

            Map<String, Object> boolQuery = new HashMap<>();
            boolQuery.put("must", Collections.singletonList(existsQuery));

            Map<String, Object> scriptScore = new HashMap<>();
            scriptScore.put("query", boolQuery);
            scriptScore.put("script", script);

            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("script_score", scriptScore);

            String queryJson = JSONUtil.toJsonStr(queryMap);
            StringQuery stringQuery = new StringQuery(queryJson);
            stringQuery.setPageable(PageRequest.of(0, topK));

            SearchHits<CourseKnowledgeEsDTO> hits = elasticsearchOperations.search(stringQuery, CourseKnowledgeEsDTO.class);
            return hits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("向量检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 文本检索（基于 multi_match 查询）
     */
    private List<CourseKnowledgeEsDTO> textSearch(String query, int topK) {
        try {
            Map<String, Object> multiMatch = new HashMap<>();
            multiMatch.put("query", query);
            multiMatch.put("fields", Arrays.asList("title", "content"));

            Map<String, Object> queryMap = new HashMap<>();
            queryMap.put("multi_match", multiMatch);

            String queryJson = JSONUtil.toJsonStr(queryMap);
            StringQuery stringQuery = new StringQuery(queryJson);
            stringQuery.setPageable(PageRequest.of(0, topK));

            SearchHits<CourseKnowledgeEsDTO> hits = elasticsearchOperations.search(stringQuery, CourseKnowledgeEsDTO.class);
            return hits.stream().map(SearchHit::getContent).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("文本检索失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 格式化检索结果
     */
    private String formatResults(List<CourseKnowledgeEsDTO> results) {
        StringBuilder sb = new StringBuilder();
        for (CourseKnowledgeEsDTO dto : results) {
            sb.append("【").append(dto.getContentType()).append("】");
            sb.append(dto.getTitle()).append("\n");
            if (StrUtil.isNotBlank(dto.getContent())) {
                sb.append(dto.getContent()).append("\n");
            }
            sb.append("\n");
        }
        String result = sb.toString().trim();
        if (result.length() > maxContentLength) {
            result = result.substring(0, maxContentLength);
        }
        return result;
    }
}
