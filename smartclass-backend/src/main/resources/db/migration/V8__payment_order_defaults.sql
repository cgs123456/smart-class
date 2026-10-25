-- V8: payment_order 时间列补默认值——实体声明了 @TableField(fill=INSERT) 但工程没有 MetaObjectHandler，
-- 其它表靠列默认值 CURRENT_TIMESTAMP 兜底，V4 建的 payment_order 缺该默认导致插入失败
ALTER TABLE `payment_order`
    MODIFY COLUMN `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    MODIFY COLUMN `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
