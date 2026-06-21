package com.cgs.smartclassbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品视图
 */
@Data
public class ProductVO implements Serializable {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private String type;
    private Integer durationDays;
    private String level;
    private Long courseId;
    private String icon;
    private Integer sortOrder;
    private Integer status;

    private static final long serialVersionUID = 1L;
}
