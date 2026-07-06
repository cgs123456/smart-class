package com.cgs.smartclass.model.dto.exercise;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 习题生成请求
 */
@Data
public class ExerciseGenerateRequest implements Serializable {

    /**
     * 课程ID（必填）
     */
    private Long courseId;

    /**
     * 章节ID（可选，为空则为整个课程生成）
     */
    private Long chapterId;

    /**
     * 题目数量（可选，默认5）
     */
    private Integer questionCount;

    private static final long serialVersionUID = 1L;
}
