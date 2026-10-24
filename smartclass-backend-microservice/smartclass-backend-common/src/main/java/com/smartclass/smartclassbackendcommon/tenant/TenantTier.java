package com.smartclass.smartclassbackendcommon.tenant;

/**
 * 租户层级
 * 决定数据隔离级别和资源配额
 */
public enum TenantTier {
    /**
     * 共享数据库、共享 Schema (Row-Level Security)
     * 适用于: 免费/基础版 SaaS 租户、中小型客户
     * 优势: 资源利用率最高、运维成本最低
     */
    SHARED,

    /**
     * 共享数据库、独立 Schema
     * 适用于: 标准版/专业版 SaaS 租户、有合规要求的中型客户
     * 优势: Schema 级隔离、支持独立备份恢复
     */
    SCHEMA,

    /**
     * 独立数据库
     * 适用于: 旗舰版/大型机构、强合规要求、私有化部署
     * 优势: 物理隔离、数据主权、等保三级
     */
    DATABASE,

    /**
     * 完全独立部署
     * 适用于: 核心战略客户、白标合作伙伴
     * 优势: 代码级隔离、独立域名/SSL/品牌、完全定制
     */
    DEDICATED;
}