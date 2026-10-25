package com.smartclass.smartclass.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * MyBatis-Plus 字段自动填充。
 * <p>
 * 实体普遍声明 {@code @TableField(fill = FieldFill.INSERT / INSERT_UPDATE)}，
 * 但工程一直没有注册 MetaObjectHandler，填充从未生效——插入时显式发送 NULL，
 * 连数据库列默认值也被覆盖（payment_order 的 createTime NOT NULL 直接违例）。
 */
@Configuration
public class MyBatisPlusFillConfig implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
