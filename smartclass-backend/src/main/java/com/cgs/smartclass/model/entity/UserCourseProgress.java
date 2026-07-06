package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户课程学习进度
 * @TableName user_course_progress
 */
@TableName(value ="user_course_progress")
@Data
public class UserCourseProgress implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 小节id
     */
    private Long sectionId;

    /**
     * 学习进度(百分比)
     */
    private Integer progress;

    /**
     * 观看时长(秒)
     */
    private Integer watchDuration;

    /**
     * 上次观看位置(秒)
     */
    private Integer lastPosition;

    /**
     * 是否完成：0-否，1-是
     */
    private Integer isCompleted;

    /**
     * 完成时间
     */
    private Date completedTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
