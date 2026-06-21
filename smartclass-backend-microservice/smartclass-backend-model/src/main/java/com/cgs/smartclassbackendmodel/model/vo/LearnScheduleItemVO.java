package com.cgs.smartclassbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 学习计划项视图
 */
@Data
public class LearnScheduleItemVO implements Serializable {

    private Long id;
    private Long scheduleId;
    private Long courseId;
    private Integer dailyWordCount;
    private Integer dailyArticleCount;
    private Integer dayOfWeek;
    private String timeSlot;

    private static final long serialVersionUID = 1L;
}
