package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 错题本
 * @TableName wrong_question
 */
@TableName(value = "wrong_question")
@Data
public class WrongQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
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

    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
