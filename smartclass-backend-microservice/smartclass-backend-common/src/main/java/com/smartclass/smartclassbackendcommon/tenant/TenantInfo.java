package com.smartclass.smartclassbackendcommon.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 租户信息实体
 * 对应数据库 tenant_info 表
 */
@TableName(value = "tenant_info")
public class TenantInfo {

    @TableId(type = IdType.ASSIGN_ID)
    private String id;

    private String name;

    private String code;

    private TenantTier tier = TenantTier.SHARED;

    private TenantStatus status = TenantStatus.ACTIVE;

    private String adminId;

    // 数据库连接配置 (SCHEMA/DATABASE/DEDICATED 层级使用)
    private String dbUrl;

    private String dbUsername;

    private String dbPassword;

    // 功能开关
    private String enabledFeatures = "";

    // 品牌定制
    private String logoUrl;

    private String primaryColor;

    private String domain;

    // 资源配额
    private Integer maxUsers = 1000;

    private Long maxStorageMb = 10240L; // 10GB

    private Integer apiQpsLimit = 1000;

    // 订阅信息
    private java.time.LocalDateTime trialEndsAt;

    private LocalDateTime expiresAt;

    // 审计字段
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer isDelete = 0;

    // ===== Getters and Setters =====

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public TenantTier getTier() { return tier; }
    public void setTier(TenantTier tier) { this.tier = tier; }

    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }

    public String getAdminId() { return adminId; }
    public void setAdminId(String adminId) { this.adminId = adminId; }

    public String getDbUrl() { return dbUrl; }
    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getDbUsername() { return dbUsername; }
    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }

    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    public String getEnabledFeatures() { return enabledFeatures; }
    public void setEnabledFeatures(String enabledFeatures) { this.enabledFeatures = enabledFeatures; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public Long getMaxStorageMb() { return maxStorageMb; }
    public void setMaxStorageMb(Long maxStorageMb) { this.maxStorageMb = maxStorageMb; }

    public Integer getApiQpsLimit() { return apiQpsLimit; }
    public void setApiQpsLimit(Integer apiQpsLimit) { this.apiQpsLimit = apiQpsLimit; }

    public java.time.LocalDateTime getTrialEndsAt() { return trialEndsAt; }
    public void setTrialEndsAt(java.time.LocalDateTime trialEndsAt) { this.trialEndsAt = trialEndsAt; }

    public java.time.LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(java.time.LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Integer getIsDelete() { return isDelete; }
    public void setIsDelete(Integer isDelete) { this.isDelete = isDelete; }

    // ===== 便捷方法 =====

    /**
     * 转换为租户上下文
     */
    public TenantContext toContext() {
        return new TenantContext(
                this.getId(),
                this.getCode(),
                this.getTier(),
                this.getStatus()
        );
    }

    /**
     * 设置启用的功能集合
     */
    public void setEnabledFeaturesSet(java.util.Set<String> features) {
        if (features == null || features.isEmpty()) {
            this.enabledFeatures = "";
        } else {
            this.enabledFeatures = String.join(",", features);
        }
    }

    /**
     * 检查功能是否启用
     */
    public boolean hasFeature(String feature) {
        return getEnabledFeaturesSet().contains(feature);
    }

    /**
     * 获取启用的功能集合
     */
    public java.util.Set<String> getEnabledFeaturesSet() {
        if (enabledFeatures == null || enabledFeatures.isEmpty()) {
            return Collections.emptySet();
        }
        String[] arr = enabledFeatures.split(",");
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String s : arr) {
            set.add(s.trim());
        }
        return set;
    }

    /**
     * 是否激活
     */
    public boolean isActive() {
        return status == TenantStatus.ACTIVE;
    }

    /**
     * 是否过期
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(java.time.LocalDateTime.now());
    }

    /**
     * 是否试用期
     */
    public boolean isTrial() {
        return trialEndsAt != null && trialEndsAt.isAfter(java.time.LocalDateTime.now());
    }
}