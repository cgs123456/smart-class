package com.cgs.smartclass.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户VIP视图
 */
@Data
public class UserVipVO implements Serializable {

    /**
     * ID
     */
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
     * 是否在有效期
     */
    private Boolean isActive;

    /**
     * 权益列表
     */
    private List<VipPrivilegeVO> privileges;

    private static final long serialVersionUID = 1L;
}
