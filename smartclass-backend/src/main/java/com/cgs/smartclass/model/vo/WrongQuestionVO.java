package com.cgs.smartclass.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 错题视图
 */
@Data
public class WrongQuestionVO implements Serializable {

    private Long id;

    private Long userId;

    private String questionType;

    private String questionContent;

    private String correctAnswer;

    private String userAnswer;

    private String analysis;

    private String sourceType;

    private Long sourceId;

    private Integer masteryLevel;

    private Integer reviewCount;

    private Date lastReviewTime;

    private Date nextReviewTime;

    /**
     * 选择题选项，JSON数组格式
     */
    private String options;

    /**
     * 关联课程ID
     */
    private Long courseId;

    /**
     * 关联章节ID
     */
    private Long chapterId;

    /**
     * 关联小节ID
     */
    private Long sectionId;

    /**
     * 难度等级：1-简单，2-中等，3-困难
     */
    private Integer difficulty;

    /**
     * 是否AI生成：0-否，1-是
     */
    private Integer aiGenerated;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
