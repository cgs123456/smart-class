-- 支付订单表
CREATE TABLE IF NOT EXISTS `payment_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `orderNo` varchar(64) NOT NULL COMMENT '订单号',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `productId` bigint NOT NULL COMMENT '商品ID',
  `productName` varchar(255) NOT NULL COMMENT '商品名称',
  `amount` decimal(10,2) NOT NULL COMMENT '支付金额',
  `paymentChannel` varchar(32) NOT NULL COMMENT '支付渠道(alipay/wechat)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '订单状态(0-待支付 1-已支付 2-已取消 3-已退款)',
  `paymentTime` datetime DEFAULT NULL COMMENT '支付时间',
  `expireTime` datetime NOT NULL COMMENT '过期时间',
  `tradeNo` varchar(128) DEFAULT NULL COMMENT '第三方交易号',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orderNo` (`orderNo`),
  KEY `idx_userId` (`userId`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付订单';

-- 商品表
CREATE TABLE IF NOT EXISTS `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(255) NOT NULL COMMENT '商品名称',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `originalPrice` decimal(10,2) DEFAULT NULL COMMENT '原价',
  `type` varchar(32) NOT NULL COMMENT '商品类型(vip/course/material)',
  `durationDays` int DEFAULT NULL COMMENT '有效天数(VIP类型)',
  `level` varchar(32) DEFAULT NULL COMMENT 'VIP等级(month/quarter/year)',
  `courseId` bigint DEFAULT NULL COMMENT '关联课程ID',
  `icon` varchar(512) DEFAULT NULL COMMENT '图标',
  `sortOrder` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(0-下架 1-上架)',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品';

-- 用户VIP表
CREATE TABLE IF NOT EXISTS `user_vip` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `level` varchar(32) NOT NULL DEFAULT 'free' COMMENT 'VIP等级(free/vip/svip)',
  `expireTime` datetime DEFAULT NULL COMMENT '过期时间',
  `autoRenew` tinyint NOT NULL DEFAULT 0 COMMENT '是否自动续费',
  `paymentOrderId` bigint DEFAULT NULL COMMENT '关联订单ID',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户VIP';

-- VIP权益配置表
CREATE TABLE IF NOT EXISTS `vip_privilege` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `level` varchar(32) NOT NULL COMMENT 'VIP等级',
  `featureKey` varchar(64) NOT NULL COMMENT '功能标识',
  `featureName` varchar(128) NOT NULL COMMENT '功能名称',
  `limitCount` int DEFAULT -1 COMMENT '限制次数(-1表示不限)',
  `description` varchar(255) DEFAULT NULL COMMENT '描述',
  `isDelete` tinyint NOT NULL DEFAULT 0 COMMENT '是否删除',
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_level_feature` (`level`, `featureKey`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP权益配置';

-- 插入默认权益数据
INSERT INTO `vip_privilege` (`level`, `featureKey`, `featureName`, `limitCount`, `description`) VALUES
('free', 'ai_chat', 'AI对话', 10, '每日10次'),
('free', 'course_access', '课程访问', 3, '仅可访问免费课程'),
('free', 'word_learning', '单词学习', 20, '每日20个单词'),
('vip', 'ai_chat', 'AI对话', 100, '每日100次'),
('vip', 'course_access', '课程访问', -1, '全部课程'),
('vip', 'word_learning', '单词学习', -1, '不限'),
('vip', 'ad_free', '去广告', -1, '无广告'),
('vip', 'exclusive_content', '专属内容', -1, 'VIP专属文章'),
('svip', 'ai_chat', 'AI对话', -1, '不限'),
('svip', 'course_access', '课程访问', -1, '全部课程'),
('svip', 'word_learning', '单词学习', -1, '不限'),
('svip', 'ad_free', '去广告', -1, '无广告'),
('svip', 'exclusive_content', '专属内容', -1, 'VIP专属文章'),
('svip', 'priority_support', '优先客服', -1, 'SVIP专属客服'),
('svip', 'custom_avatar', '自定义AI分身', 5, '最多5个');
