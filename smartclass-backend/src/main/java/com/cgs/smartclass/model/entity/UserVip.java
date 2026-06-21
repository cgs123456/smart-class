package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户VIP
 * @TableName user_vip
 */
@TableName(value = "user_vip")
@Data
public class UserVip implements Serializable {

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * VIP等级(free/vip/svip)
     */
    private String level;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 是否自动续费
     */
    private Integer autoRenew;

    /**
     * 关联订单ID
     */
    private Long paymentOrderId;

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
