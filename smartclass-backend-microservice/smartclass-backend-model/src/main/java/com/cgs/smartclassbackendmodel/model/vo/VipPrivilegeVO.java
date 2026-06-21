package com.cgs.smartclassbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * VIP权益视图
 */
@Data
public class VipPrivilegeVO implements Serializable {

    private Long id;
    private String level;
    private String featureKey;
    private String featureName;
    private Integer limitCount;
    private String description;

    private static final long serialVersionUID = 1L;
}
