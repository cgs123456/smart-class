-- V7: V6 遗漏表的列对齐（payment_order 等——V6 提取时语句合并漏表）
SET FOREIGN_KEY_CHECKS=0;
ALTER TABLE `payment_order` ADD COLUMN `productId` bigint NOT NULL COMMENT '商品ID';
ALTER TABLE `payment_order` ADD COLUMN `productName` varchar(255) NOT NULL COMMENT '商品名称';
ALTER TABLE `payment_order` ADD COLUMN `paymentChannel` varchar(32) NOT NULL COMMENT '支付渠道(alipay/wechat)';
ALTER TABLE `payment_order` ADD COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态(0-待支付 1-已支付 2-已取消 3-已退款)';
ALTER TABLE `payment_order` ADD COLUMN `paymentTime` datetime DEFAULT NULL COMMENT '支付时间';
ALTER TABLE `payment_order` ADD COLUMN `tradeNo` varchar(128) DEFAULT NULL COMMENT '第三方交易号';
ALTER TABLE `payment_order` ADD COLUMN `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除';
ALTER TABLE `payment_order` ADD COLUMN `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间';
SET FOREIGN_KEY_CHECKS=1;
