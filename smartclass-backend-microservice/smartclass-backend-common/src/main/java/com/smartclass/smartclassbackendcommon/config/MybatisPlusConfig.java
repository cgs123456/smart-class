package com.smartclass.smartclassbackendcommon.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.smartclass.smartclassbackendcommon.tenant.TenantContextHolder;
import com.smartclass.smartclassbackendcommon.tenant.TenantLineHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 * 包含: 租户隔离插件、分页插件、防全表更新/删除
 *
 * <p>各服务模块在自己的启动类上声明 @MapperScan，
 * 逻辑删除由 MyBatis-Plus 3.5.x 自动注入，无需自定义 SqlInjector。</p>
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 租户行处理器
     * 自动为 SQL 添加 tenant_id 过滤条件
     */
    @Bean
    public TenantLineHandler tenantLineHandler() {
        return new TenantLineHandler();
    }

    /**
     * MyBatis-Plus 拦截器链
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 租户隔离插件 (最先执行，自动添加 tenant_id 条件)
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));

        // 2. 防全表更新/删除插件
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 3. 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }

    /**
     * 字段自动填充：实体普遍使用 @TableField(fill = INSERT/INSERT_UPDATE) 声明
     * createTime/updateTime（java.util.Date），缺少本 Handler 时 MyBatis-Plus 会
     * 显式插入 NULL，被数据库 NOT NULL 拒绝（依赖列默认值也无法兜底）。
     */
    @Bean
    public com.baomidou.mybatisplus.core.handlers.MetaObjectHandler metaObjectHandler() {
        return new com.baomidou.mybatisplus.core.handlers.MetaObjectHandler() {
            @Override
            public void insertFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", java.util.Date.class, new java.util.Date());
                this.strictInsertFill(metaObject, "updateTime", java.util.Date.class, new java.util.Date());
            }

            @Override
            public void updateFill(org.apache.ibatis.reflection.MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", java.util.Date.class, new java.util.Date());
            }
        };
    }

    /**
     * 全局配置
     */
    @Bean
    public GlobalConfig globalConfig() {
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false); // 关闭启动 banner
        return globalConfig;
    }
}
