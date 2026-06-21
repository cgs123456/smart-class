package com.cgs.smartclassbackendmodel.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品
 * @TableName product
 */
@TableName(value = "product")
@Data
public class Product implements Serializable {

    /**
     * 商品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 原价
     */
    private BigDecimal originalPrice;

    /**
     * 商品类型(vip/course/material)
     */
    private String type;

    /**
     * 有效天数(VIP类型)
     */
    private Integer durationDays;

    /**
     * VIP等级(month/quarter/year)
     */
    private String level;

    /**
     * 关联课程ID
     */
    private Long courseId;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态(0-下架 1-上架)
     */
    private Integer status;

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
