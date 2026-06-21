package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * VIP权益配置
 * @TableName vip_privilege
 */
@TableName(value = "vip_privilege")
@Data
public class VipPrivilege implements Serializable {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
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
