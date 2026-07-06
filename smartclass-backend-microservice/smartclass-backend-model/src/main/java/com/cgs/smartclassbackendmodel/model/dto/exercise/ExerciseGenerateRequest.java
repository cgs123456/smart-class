package com.cgs.smartclassbackendmodel.model.dto.exercise;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 习题生成请求
 */
@Data
public class ExerciseGenerateRequest implements Serializable {

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 章节ID（可选，为空则基于整门课程生成）
     */
    private Long chapterId;

    /**
     * 生成题目数量
     */
    private Integer questionCount;

    private static final long serialVersionUID = 1L;
}
