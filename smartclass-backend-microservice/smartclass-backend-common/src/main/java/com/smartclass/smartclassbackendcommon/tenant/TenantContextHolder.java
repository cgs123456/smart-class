package com.smartclass.smartclassbackendcommon.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 租户上下文持有者
 * 支持 ThreadLocal (传统 Servlet) 模式
 * Reactor Context 模式在响应式场景中由 TenantContextFilter 负责设置
 */
@Component
public class TenantContextHolder {

    private static final Logger log = LoggerFactory.getLogger(TenantContextHolder.class);

    private static final ThreadLocal<TenantContext> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置租户上下文
     */
    public static void set(TenantContext context) {
        THREAD_LOCAL.set(context);
    }

    /**
     * 获取租户上下文 (仅支持 ThreadLocal 模式)
     * Reactor Context 整合由过滤器通过 set() 方法处理
     */
    public static TenantContext get() {
        return THREAD_LOCAL.get();
    }

    /**
     * 获取租户上下文，如果不存在则返回默认租户
     */
    public static TenantContext getOrDefault() {
        TenantContext context = get();
        return context != null ? context : TenantContext.defaultTenant();
    }

    /**
     * 获取当前租户 ID
     */
    public static String getTenantId() {
        return getOrDefault().getTenantId();
    }

    /**
     * 获取当前租户层级
     */
    public static TenantTier getTenantTier() {
        return getOrDefault().getTier();
    }

    /**
     * 清理租户上下文
     */
    public static void clear() {
        THREAD_LOCAL.remove();
    }
}