package com.cgs.smartclass.service;

/**
 * 课程知识 RAG 服务
 */
public interface CourseRagService {

    /**
     * 检索与用户问题相关的课程内容
     *
     * @param query 用户问题
     * @param topK  返回的最大结果数
     * @return 拼接好的上下文文本，如果无结果返回空字符串
     */
    String retrieveRelevantContent(String query, int topK);

    /**
     * 全量同步课程内容到 ES（含向量化）
     */
    void syncAllCourseContent();

    /**
     * 同步单个课程的内容到 ES
     *
     * @param courseId 课程ID
     */
    void syncCourseContent(Long courseId);
}
