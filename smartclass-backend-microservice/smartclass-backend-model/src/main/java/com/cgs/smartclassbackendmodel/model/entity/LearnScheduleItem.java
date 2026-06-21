package com.cgs.smartclassbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 学习计划项
 * @TableName learn_schedule_item
 */
@TableName(value = "learn_schedule_item")
@Data
public class LearnScheduleItem implements Serializable {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 计划ID
     */
    private Long scheduleId;

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
