package com.smartclass.smartclassbackendcommon.tenant;

// MP 接口与本类同名，使用全限定名实现，避免编译单元内命名冲突

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 租户行处理器
 * 实现 MyBatis-Plus TenantLineHandler 接口
 * 自动为 SQL 添加 tenant_id 过滤条件
 */
@Component
public class TenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {

    // 不需要租户隔离的表
    private static final Set<String> IGNORE_TABLES = Set.of(
            "tenant_info",           // 租户表本身
            "system_config",         // 系统配置表
            "dict_data",             // 字典数据表
            "sys_log",               // 系统日志表
            "quartz_job",            // 定时任务表
            "flyway_schema_history"  // Flyway 版本表
    );

    @Override
    public Expression getTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || "default".equals(tenantId)) {
            // 默认租户使用 0 表示共享数据
            return new LongValue(0);
        }
        try {
            return new LongValue(Long.parseLong(tenantId));
        } catch (NumberFormatException e) {
            // 非数字租户 ID 使用字符串比较
            return new net.sf.jsqlparser.expression.StringValue(tenantId);
        }
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 1. 忽略系统表
        if (IGNORE_TABLES.contains(tableName)) {
            return true;
        }

        // 2. 忽略 Flyway 相关表
        if (tableName.startsWith("flyway_")) {
            return true;
        }

        // 3. 可以扩展：从数据库元数据判断表是否有 tenant_id 列
        // 这里简化处理，实际项目建议缓存表结构信息
        return false;
    }
}