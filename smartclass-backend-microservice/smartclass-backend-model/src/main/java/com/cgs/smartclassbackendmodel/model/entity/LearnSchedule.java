package com.cgs.smartclassbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 学习计划
 * @TableName learn_schedule
 */
@TableName(value = "learn_schedule")
@Data
public class LearnSchedule implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

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
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
