package com.cgs.smartclassbackendmodel.model.dto.learnschedule;

import lombok.Data;

import java.io.Serializable;

/**
 * 学习计划项DTO
 */
@Data
public class LearnScheduleItemDTO implements Serializable {

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 每日单词数
     */
    private Integer dailyWordCount;

    /**
     * 每日文章数
     */
    private Integer dailyArticleCount;

    /**
     * 星期几(1-7)
     */
    private Integer dayOfWeek;

    /**
     * 时间段
     */
    private String timeSlot;

    private static final long serialVersionUID = 1L;
}
