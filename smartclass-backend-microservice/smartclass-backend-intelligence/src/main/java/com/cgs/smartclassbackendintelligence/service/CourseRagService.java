package com.cgs.smartclassbackendintelligence.service;

/**
 * 课程知识 RAG 服务
 * 负责课程内容的向量化和检索，为 AI 助手提供课程知识上下文
 */
public interface CourseRagService {

    /**
     * 检索与查询相关的课程知识内容
     *
     * @param query 用户查询文本
     * @param topK  返回的最大文档数
     * @return 格式化的知识上下文文本
     */
    String retrieveRelevantContent(String query, int topK);

    /**
     * 同步所有课程内容到 ES
     */
    void syncAllCourseContent();

    /**
     * 同步指定课程内容到 ES
     *
     * @param courseId 课程ID
     */
    void syncCourseContent(Long courseId);
}
