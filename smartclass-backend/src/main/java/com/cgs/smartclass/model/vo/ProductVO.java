package com.cgs.smartclass.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品视图
 */
@Data
public class ProductVO implements Serializable {

    /**
     * 商品ID
     */
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

    private static final long serialVersionUID = 1L;
}
