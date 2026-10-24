package com.smartclass.smartclassbackendcommon.tenant;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 租户注册表
 * 提供租户信息的缓存访问，支持本地缓存 + 数据库回源 (使用 Guava Cache)
 */
@Service
public class TenantRegistry {

    private static final Logger log = LoggerFactory.getLogger(TenantRegistry.class);

    private final TenantMapper tenantMapper;

    // 本地缓存 (Guava Cache)
    private final Cache<String, TenantInfo> cache = CacheBuilder.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public TenantRegistry(TenantMapper tenantMapper) {
        this.tenantMapper = tenantMapper;
    }

    @PostConstruct
    public void warmup() {
        log.info("开始预热租户缓存...");
        List<TenantInfo> allTenants = tenantMapper.findAll();
        ConcurrentMap<String, TenantInfo> nativeCache = cache.asMap();
        allTenants.forEach(tenant -> {
            nativeCache.put(tenant.getId(), tenant);
            nativeCache.put("code:" + tenant.getCode(), tenant);
            if (tenant.getDomain() != null) {
                nativeCache.put("domain:" + tenant.getDomain(), tenant);
            }
        });
        log.info("租户缓存预热完成，共 {} 个租户", allTenants.size());
    }

    /**
     * 获取租户信息 (缓存优先，回源数据库)
     */
    public TenantInfo get(String tenantId) {
        try {
            return cache.get(tenantId, () -> tenantMapper.findById(tenantId)
                    .orElseGet(() -> tenantMapper.findByCode(tenantId).orElse(null)));
        } catch (Exception e) {
            log.warn("获取租户信息失败: {}", tenantId, e);
            return null;
        }
    }

    /**
     * 根据租户编码获取
     */
    public TenantInfo getByCode(String code) {
        String cacheKey = "code:" + code;
        try {
            return cache.get(cacheKey, () -> tenantMapper.findByCode(code).orElse(null));
        } catch (Exception e) {
            log.warn("获取租户信息失败 (code: {}): {}", code, e.getMessage());
            return null;
        }
    }

    /**
     * 根据域名获取
     */
    public TenantInfo getByDomain(String domain) {
        String cacheKey = "domain:" + domain;
        try {
            return cache.get(cacheKey, () -> tenantMapper.findByDomain(domain).orElse(null));
        } catch (Exception e) {
            log.warn("获取租户信息失败 (domain: {}): {}", domain, e.getMessage());
            return null;
        }
    }

    /**
     * 强制刷新缓存
     */
    public void refresh(String tenantId) {
        cache.invalidate(tenantId);
        get(tenantId); // 重新加载
    }

    /**
     * 批量刷新
     */
    public void refreshAll(List<String> tenantIds) {
        tenantIds.forEach(this::refresh);
    }

    /**
     * 使租户缓存失效 (用于租户信息更新后)
     */
    public void invalidate(String tenantId) {
        cache.invalidate(tenantId);
    }

    /**
     * 获取所有租户 (用于数据源初始化)
     */
    public List<TenantInfo> getAllTenants() {
        return tenantMapper.findAll();
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        try {
            long size = cache.size();
            long hitRate = (long) (cache.stats().hitRate() * 100);
            return String.format("缓存大小: %d, 命中率: %.2f%%", size, hitRate);
        } catch (Exception e) {
            return "缓存大小: 0, 命中率: 0.00%";
        }
    }
}