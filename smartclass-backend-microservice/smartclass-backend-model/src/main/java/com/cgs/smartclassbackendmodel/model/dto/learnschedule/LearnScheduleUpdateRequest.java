package com.cgs.smartclassbackendmodel.model.dto.learnschedule;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 更新学习计划请求
 */
@Data
public class LearnScheduleUpdateRequest implements Serializable {

    /**
     * 计划ID
     */
    private Long id;

    /**
     * 计划标题
     */
    private String title;

    /**
     * 计划描述
     */
    private String description;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

    /**
     * 状态(0-进行中 1-已完成 2-已放弃)
     */
    private Integer status;

    /**
     * 每日学习目标(分钟)
     */
    private Integer dailyMinutes;

    /**
     * 计划项列表
     */
    private List<LearnScheduleItemDTO> items;

    private static final long serialVersionUID = 1L;
}
