package com.cgs.smartclassbackendintelligence.service;

import com.cgs.smartclassbackendmodel.model.entity.WrongQuestion;

import java.util.List;

/**
 * AI 习题生成服务
 */
public interface ExerciseGenerationService {

    /**
     * 根据课程内容生成 AI 习题
     *
     * @param courseId      课程ID
     * @param chapterId     章节ID（为空则基于整门课程生成）
     * @param questionCount 题目数量
     * @param operatorId    操作人ID
     * @return 实际生成的题目数量
     */
    int generateExercises(Long courseId, Long chapterId, int questionCount, Long operatorId);

    /**
     * 获取课程（或章节）下的 AI 习题列表
     *
     * @param courseId  课程ID
     * @param chapterId 章节ID（可选）
     * @return AI 习题列表
     */
    List<WrongQuestion> getCourseExercises(Long courseId, Long chapterId);
}
