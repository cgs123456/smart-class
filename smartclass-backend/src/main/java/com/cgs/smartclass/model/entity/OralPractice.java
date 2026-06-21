package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 口语练习题目
 * @TableName oral_practice
 */
@TableName(value = "oral_practice")
@Data
public class OralPractice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String category;

    private Integer difficulty;

    private String referenceAnswer;

    private String keywords;

    private String audioUrl;

    private String imageUrl;

    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
