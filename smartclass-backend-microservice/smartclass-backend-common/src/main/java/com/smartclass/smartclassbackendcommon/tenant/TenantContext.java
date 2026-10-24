package com.smartclass.smartclassbackendcommon.tenant;

import java.util.Set;

/**
 * 租户上下文
 * 包含租户的基本信息和权限特性
 */
public class TenantContext {

    private String tenantId;
    private String name;
    private TenantTier tier;
    private TenantStatus status;
    private Set<String> enabledFeatures;
    private String dbUrl;
    private String adminId;

    public TenantContext() {}

    public TenantContext(String tenantId, String name, TenantTier tier, TenantStatus status) {
        this(tenantId, name, tier, status, null, null, null);
    }

    public TenantContext(String tenantId, String name, TenantTier tier, TenantStatus status,
                         Set<String> enabledFeatures, String dbUrl, String adminId) {
        this.tenantId = tenantId;
        this.name = name;
        this.tier = tier;
        this.status = status;
        this.enabledFeatures = enabledFeatures;
        this.dbUrl = dbUrl;
        this.adminId = adminId;
    }

    public static TenantContext defaultTenant() {
        TenantContext ctx = new TenantContext();
        ctx.tenantId = "default";
        ctx.name = "默认租户";
        ctx.tier = TenantTier.SHARED;
        ctx.status = TenantStatus.ACTIVE;
        ctx.enabledFeatures = java.util.Set.of("AI_CHAT", "RAG_SEARCH", "COURSE_MARKET", "CIRCLE");
        return ctx;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TenantTier getTier() { return tier; }
    public void setTier(TenantTier tier) { this.tier = tier; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public Set<String> getEnabledFeatures() { return enabledFeatures; }
    public void setEnabledFeatures(Set<String> enabledFeatures) { this.enabledFeatures = enabledFeatures; }

    public String getDbUrl() { return dbUrl; }
    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public boolean hasFeature(String feature) {
        return enabledFeatures != null && enabledFeatures.contains(feature);
    }

    public boolean isSharedDatabase() {
        return tier == TenantTier.SHARED;
    }

    public boolean isSchemaIsolation() {
        return tier == TenantTier.SCHEMA;
    }

    public boolean isDatabaseIsolation() {
        return tier == TenantTier.DATABASE || tier == TenantTier.DEDICATED;
    }
}