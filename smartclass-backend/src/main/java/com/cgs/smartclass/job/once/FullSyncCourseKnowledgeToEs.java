package com.cgs.smartclass.job.once;

import com.cgs.smartclass.esdao.CourseKnowledgeEsDao;
import com.cgs.smartclass.service.CourseRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 全量同步课程知识到 ES（含向量化）
 */
@Component
@Slf4j
public class FullSyncCourseKnowledgeToEs implements CommandLineRunner {

    @Autowired(required = false)
    private CourseKnowledgeEsDao courseKnowledgeEsDao;

    @Autowired
    private CourseRagService courseRagService;

    @Value("${rag.enabled:false}")
    private boolean ragEnabled;

    @Override
    public void run(String... args) {
        if (courseKnowledgeEsDao == null) {
            log.info("Elasticsearch 不可用，跳过全量同步课程知识");
            return;
        }
        if (!ragEnabled) {
            log.info("RAG 未启用，跳过全量同步课程知识");
            return;
        }
        try {
            courseRagService.syncAllCourseContent();
        } catch (Exception e) {
            log.error("全量同步课程知识到 ES 异常: {}", e.getMessage(), e);
        }
    }
}
