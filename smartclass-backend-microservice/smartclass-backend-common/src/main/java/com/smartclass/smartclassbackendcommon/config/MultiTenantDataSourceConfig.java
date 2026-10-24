package com.smartclass.smartclassbackendcommon.config;

import com.smartclass.smartclassbackendcommon.tenant.TenantContextHolder;
import com.smartclass.smartclassbackendcommon.tenant.TenantInfo;
import com.smartclass.smartclassbackendcommon.tenant.TenantRegistry;
import com.smartclass.smartclassbackendcommon.tenant.TenantTier;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置
 * 支持多租户数据隔离:
 * - SHARED: 共享数据源 (默认)
 * - SCHEMA: 共享数据库、独立 Schema (通过 SET search_path 实现)
 * - DATABASE: 独立数据源
 * - DEDICATED: 完全独立部署 (独立数据源)
 *
 * <p>默认关闭，通过 {@code smartclass.multi-tenant.datasource.enabled=true} 启用。
 * 启用前需确保 tenant_info 表存在且已初始化租户数据。</p>
 *
 * <p>当前多租户起步模式为 Row-Level 隔离（见 {@link MybatisPlusConfig}），
 * 本配置用于后续 DATABASE/DEDICATED 层级的独立数据源扩展。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "smartclass.multi-tenant.datasource", name = "enabled", havingValue = "true")
public class MultiTenantDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantDataSourceConfig.class);

    private final TenantRegistry tenantRegistry;

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/smart_class}")
    private String sharedUrl;

    @Value("${spring.datasource.username:root}")
    private String sharedUsername;

    @Value("${spring.datasource.password:}")
    private String sharedPassword;

    @Value("${spring.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}")
    private String sharedDriverClassName;

    public MultiTenantDataSourceConfig(TenantRegistry tenantRegistry) {
        this.tenantRegistry = tenantRegistry;
    }

    /**
     * 共享数据源 (默认租户使用)
     */
    @Bean
    @Primary
    public DataSource sharedDataSource() {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(sharedUrl)
                .username(sharedUsername)
                .password(sharedPassword)
                .driverClassName(sharedDriverClassName)
                .build();
    }

    /**
     * 路由数据源
     * 根据租户层级路由到不同数据源
     */
    @Bean
    public DataSource routingDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("shared", sharedDataSource());

        // 动态添加租户独立数据源 (SCHEMA/DATABASE/DEDICATED 层级)
        tenantRegistry.getAllTenants().stream()
                .filter(t -> t.getTier() != TenantTier.SHARED && t.getDbUrl() != null)
                .forEach(tenant -> {
                    DataSource tenantDs = createTenantDataSource(tenant);
                    targetDataSources.put(tenant.getId(), tenantDs);
                    log.info("注册租户数据源: {} - {}", tenant.getId(), tenant.getTier());
                });

        AbstractRoutingDataSource routingDs = new AbstractRoutingDataSource() {
            @Override
            protected Object determineCurrentLookupKey() {
                String tenantId = TenantContextHolder.getTenantId();
                TenantTier tier = TenantContextHolder.getTenantTier();

                if (tenantId == null || "default".equals(tenantId) || TenantTier.SHARED == tier) {
                    return "shared";
                }

                // 根据租户层级决定使用哪个数据源
                return tenantId;
            }
        };

        routingDs.setTargetDataSources(targetDataSources);
        routingDs.setDefaultTargetDataSource(sharedDataSource());
        return routingDs;
    }

    private DataSource createTenantDataSource(TenantInfo tenant) {
        return DataSourceBuilder.create()
                .type(HikariDataSource.class)
                .url(tenant.getDbUrl())
                .username(tenant.getDbUsername())
                .password(tenant.getDbPassword())
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
}
