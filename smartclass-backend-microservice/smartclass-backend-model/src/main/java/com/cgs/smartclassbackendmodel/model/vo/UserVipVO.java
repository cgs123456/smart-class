package com.cgs.smartclassbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 用户VIP视图
 */
@Data
public class UserVipVO implements Serializable {

    private Long id;
    private Long userId;
    private String level;
    private Date expireTime;
    private Integer autoRenew;
    private Boolean isActive;
    private List<VipPrivilegeVO> privileges;

    private static final long serialVersionUID = 1L;
}
