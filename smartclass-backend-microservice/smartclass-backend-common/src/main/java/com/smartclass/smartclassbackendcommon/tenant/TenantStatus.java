package com.smartclass.smartclassbackendcommon.tenant;

/**
 * 租户状态
 */
public enum TenantStatus {
    /**
     * 正常激活
     */
    ACTIVE,

    /**
     * 暂停服务 (欠费/违规)
     */
    SUSPENDED,

    /**
     * 过期 (订阅到期)
     */
    EXPIRED,

    /**
     * 试用期
     */
    TRIAL
}