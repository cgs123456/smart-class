package com.cgs.smartclass.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.cgs.smartclass.esdao.CourseKnowledgeEsDao;
import com.cgs.smartclass.model.dto.course.CourseKnowledgeEsDTO;
import com.cgs.smartclass.model.entity.Course;
import com.cgs.smartclass.model.entity.CourseChapter;
import com.cgs.smartclass.model.entity.CourseSection;
import com.cgs.smartclass.service.CourseChapterService;
import com.cgs.smartclass.service.CourseRagService;
import com.cgs.smartclass.service.CourseSectionService;
import com.cgs.smartclass.service.CourseService;
import com.cgs.smartclass.service.EmbeddingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 课程知识 RAG 服务实现
 */
@Service
@Slf4j
public class CourseRagServiceImpl implements CourseRagService {

    /**
     * 批量处理大小
     */
    private static final int BATCH_SIZE = 100;

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseSectionService courseSectionService;

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
        if (!ragEnabled) {
            return "";
        }
        if (courseKnowledgeEsDao == null || elasticsearchOperations == null) {
            return "";
        }
        if (StrUtil.isBlank(query)) {
            return "";
        }
        try {
            SearchHits<CourseKnowledgeEsDTO> searchHits;
            float[] queryVector = embeddingService.isAvailable() ? embeddingService.embed(query) : null;
            if (queryVector != null) {
                // KNN 向量检索
                List<Float> vectorList = new ArrayList<>(queryVector.length);
                for (float f : queryVector) {
                    vectorList.add(f);
                }
                int candidates = Math.max(topK * 5, 50);
                NativeQuery nativeQuery = NativeQuery.builder()
                        .withQuery(q -> q.knn(k -> k
                                .field("embedding")
                                .queryVector(vectorList)
                                .numCandidates((long) candidates)))
                        .withPageable(PageRequest.of(0, topK))
                        .build();
                searchHits = elasticsearchOperations.search(nativeQuery, CourseKnowledgeEsDTO.class);
            } else {
                // 文本检索兜底
                NativeQuery nativeQuery = NativeQuery.builder()
                        .withQuery(q -> q.multiMatch(m -> m
                                .query(query)
                                .fields("title", "content")))
                        .withPageable(PageRequest.of(0, topK))
                        .build();
                searchHits = elasticsearchOperations.search(nativeQuery, CourseKnowledgeEsDTO.class);
            }
            return buildContextFromHits(searchHits);
        } catch (Exception e) {
            log.warn("RAG 检索失败: {}", e.getMessage(), e);
            return "";
        }
    }

    @Override
    public void syncAllCourseContent() {
        if (courseKnowledgeEsDao == null) {
            log.warn("Elasticsearch 不可用，跳过课程知识全量同步");
            return;
        }
        log.info("开始全量同步课程知识到 ES");

        // 同步课程
        List<Course> courseList = courseService.list();
        if (CollUtil.isNotEmpty(courseList)) {
            List<CourseKnowledgeEsDTO> dtoList = new ArrayList<>();
            for (Course course : courseList) {
                CourseKnowledgeEsDTO dto = CourseKnowledgeEsDTO.buildFromCourse(course);
                if (dto != null) {
                    dtoList.add(dto);
                }
            }
            log.info("课程知识：构建 {} 条课程 DTO", dtoList.size());
            batchEmbedAndSave(dtoList);
        }

        // 同步章节
        List<CourseChapter> chapterList = courseChapterService.list();
        if (CollUtil.isNotEmpty(chapterList)) {
            List<CourseKnowledgeEsDTO> dtoList = new ArrayList<>();
            for (CourseChapter chapter : chapterList) {
                CourseKnowledgeEsDTO dto = CourseKnowledgeEsDTO.buildFromChapter(chapter);
                if (dto != null) {
                    dtoList.add(dto);
                }
            }
            log.info("课程知识：构建 {} 条章节 DTO", dtoList.size());
            batchEmbedAndSave(dtoList);
        }

        // 同步小节
        List<CourseSection> sectionList = courseSectionService.list();
        if (CollUtil.isNotEmpty(sectionList)) {
            List<CourseKnowledgeEsDTO> dtoList = new ArrayList<>();
            for (CourseSection section : sectionList) {
                CourseKnowledgeEsDTO dto = CourseKnowledgeEsDTO.buildFromSection(section);
                if (dto != null) {
                    dtoList.add(dto);
                }
            }
            log.info("课程知识：构建 {} 条小节 DTO", dtoList.size());
            batchEmbedAndSave(dtoList);
        }

        log.info("全量同步课程知识到 ES 完成");
    }

    @Override
    public void syncCourseContent(Long courseId) {
        if (courseKnowledgeEsDao == null) {
            log.warn("Elasticsearch 不可用，跳过课程知识同步");
            return;
        }
        if (courseId == null) {
            return;
        }
        // 删除旧数据
        try {
            courseKnowledgeEsDao.deleteByCourseId(courseId);
        } catch (Exception e) {
            log.warn("删除课程 {} 旧知识数据异常: {}", courseId, e.getMessage());
        }

        List<CourseKnowledgeEsDTO> dtoList = new ArrayList<>();

        // 课程本身
        Course course = courseService.getById(courseId);
        if (course != null) {
            CourseKnowledgeEsDTO dto = CourseKnowledgeEsDTO.buildFromCourse(course);
            if (dto != null) {
                dtoList.add(dto);
            }
        }

        // 章节
        List<CourseChapter> chapterList = courseChapterService.getChaptersByCourseId(courseId);
        if (CollUtil.isNotEmpty(chapterList)) {
            for (CourseChapter chapter : chapterList) {
                CourseKnowledgeEsDTO dto = CourseKnowledgeEsDTO.buildFromChapter(chapter);
                if (dto != null) {
                    dtoList.add(dto);
                }
            }
        }

        // 小节
        List<CourseSection> sectionList = courseSectionService.getSectionsByCourseId(courseId);
        if (CollUtil.isNotEmpty(sectionList)) {
            for (CourseSection section : sectionList) {
                CourseKnowledgeEsDTO dto = CourseKnowledgeEsDTO.buildFromSection(section);
                if (dto != null) {
                    dtoList.add(dto);
                }
            }
        }

        batchEmbedAndSave(dtoList);
        log.info("同步课程 {} 知识完成，共 {} 条", courseId, dtoList.size());
    }

    /**
     * 分批向量化并保存
     */
    private void batchEmbedAndSave(List<CourseKnowledgeEsDTO> dtoList) {
        if (CollUtil.isEmpty(dtoList)) {
            return;
        }
        boolean embeddingAvailable = embeddingService.isAvailable();
        for (int i = 0; i < dtoList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, dtoList.size());
            List<CourseKnowledgeEsDTO> batch = dtoList.subList(i, end);
            if (embeddingAvailable) {
                List<String> texts = batch.stream()
                        .map(dto -> StrUtil.isNotBlank(dto.getTitle()) ? dto.getTitle() + " " + dto.getContent() : dto.getContent())
                        .collect(Collectors.toList());
                List<float[]> embeddings = embeddingService.embedBatch(texts);
                if (embeddings != null && embeddings.size() == batch.size()) {
                    for (int j = 0; j < batch.size(); j++) {
                        batch.get(j).setEmbedding(embeddings.get(j));
                    }
                } else {
                    log.warn("批量向量化失败或返回数量不匹配，本批次 embedding 将为 null");
                }
            }
            try {
                courseKnowledgeEsDao.saveAll(batch);
                log.info("课程知识批次 {}-{} 同步完成", i, end);
            } catch (Exception e) {
                log.warn("课程知识批次 {}-{} 保存异常: {}", i, end, e.getMessage());
            }
        }
    }

    /**
     * 将检索结果拼接为格式化上下文文本
     */
    private String buildContextFromHits(SearchHits<CourseKnowledgeEsDTO> searchHits) {
        if (searchHits == null || searchHits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (SearchHit<CourseKnowledgeEsDTO> hit : searchHits.getSearchHits()) {
            CourseKnowledgeEsDTO dto = hit.getContent();
            if (dto == null) {
                continue;
            }
            String typeLabel;
            switch (dto.getContentType() == null ? "" : dto.getContentType()) {
                case "course":
                    typeLabel = "课程";
                    break;
                case "chapter":
                    typeLabel = "章节";
                    break;
                case "section":
                    typeLabel = "小节";
                    break;
                default:
                    typeLabel = "内容";
            }
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("【").append(typeLabel).append("：")
                    .append(dto.getTitle() != null ? dto.getTitle() : "").append("】\n");
            if (StrUtil.isNotBlank(dto.getContent())) {
                sb.append(dto.getContent());
            }
        }
        String result = sb.toString();
        if (result.length() > maxContentLength) {
            result = result.substring(0, maxContentLength);
        }
        return result;
    }
}
