package com.cgs.smartclassbackendmodel.model.dto.learnschedule;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 创建学习计划请求
 */
@Data
public class LearnScheduleCreateRequest implements Serializable {

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
     * 每日学习目标(分钟)
     */
    private Integer dailyMinutes;

    /**
     * 计划项列表
     */
    private List<LearnScheduleItemDTO> items;

    private static final long serialVersionUID = 1L;
}
