package com.cgs.smartclass.service;

import com.cgs.smartclass.model.entity.WrongQuestion;

import java.util.List;

/**
 * AI 习题生成服务
 */
public interface ExerciseGenerationService {

    /**
     * 为指定课程章节生成习题
     *
     * @param courseId      课程ID
     * @param chapterId     章节ID（可选，为 null 则为整个课程生成）
     * @param questionCount 题目数量
     * @param operatorId    操作人ID
     * @return 生成的习题数量
     */
    int generateExercises(Long courseId, Long chapterId, int questionCount, Long operatorId);

    /**
     * 获取课程下的 AI 生成习题
     *
     * @param courseId  课程ID
     * @param chapterId 章节ID（可选）
     * @return 习题列表
     */
    List<WrongQuestion> getCourseExercises(Long courseId, Long chapterId);
}
