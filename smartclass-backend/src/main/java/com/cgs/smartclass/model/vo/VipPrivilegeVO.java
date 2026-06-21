package com.cgs.smartclass.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * VIP权益视图
 */
@Data
public class VipPrivilegeVO implements Serializable {

    /**
     * ID
     */
    private Long id;

    /**
     * VIP等级
     */
    private String level;

    /**
     * 功能标识
     */
    private String featureKey;

    /**
     * 功能名称
     */
    private String featureName;

    /**
     * 限制次数(-1表示不限)
     */
    private Integer limitCount;

    /**
     * 描述
     */
    private String description;

    private static final long serialVersionUID = 1L;
}
