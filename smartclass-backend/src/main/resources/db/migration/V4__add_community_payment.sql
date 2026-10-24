-- Flyway Migration V4: 社区互动、收藏、付费模块表结构

-- 课程评论表
CREATE TABLE IF NOT EXISTS `course_comment` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `courseId` BIGINT NOT NULL COMMENT '课程ID',
    `userId` BIGINT NOT NULL COMMENT '评论者ID',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `parentId` BIGINT NULL COMMENT '父评论ID（用于回复嵌套）',
    `likeCount` INT DEFAULT 0 NOT NULL COMMENT '点赞数',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_courseId` (`courseId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_parentId` (`parentId`),
    INDEX `idx_createTime` (`createTime`),
    CONSTRAINT `fk_course_comment_user` FOREIGN KEY (`userId`) REFERENCES `user`(`id`),
    CONSTRAINT `fk_course_comment_course` FOREIGN KEY (`courseId`) REFERENCES `course`(`id`)
) COMMENT '课程评论' COLLATE = utf8mb4_unicode_ci;

-- 课程评论回复表
CREATE TABLE IF NOT EXISTS `course_comment_reply` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `courseCommentId` BIGINT NOT NULL COMMENT '课程评论ID',
    `userId` BIGINT NOT NULL COMMENT '回复者ID',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `likeCount` INT DEFAULT 0 NOT NULL COMMENT '点赞数',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_courseCommentId` (`courseCommentId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_createTime` (`createTime`),
    CONSTRAINT `fk_course_comment_reply_comment` FOREIGN KEY (`courseCommentId`) REFERENCES `course_comment`(`id`),
    CONSTRAINT `fk_course_comment_reply_user` FOREIGN KEY (`userId`) REFERENCES `user`(`id`)
) COMMENT '课程评论回复' COLLATE = utf8mb4_unicode_ci;

-- 课程收藏表
CREATE TABLE IF NOT EXISTS `course_favour` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `courseId` BIGINT NOT NULL COMMENT '课程ID',
    `userId` BIGINT NOT NULL COMMENT '收藏用户ID',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE INDEX `uk_course_user` (`courseId`, `userId`),
    INDEX `idx_courseId` (`courseId`),
    INDEX `idx_userId` (`userId`)
) COMMENT '课程收藏' COLLATE = utf8mb4_unicode_ci;

-- 用户收藏的课程学习记录表
CREATE TABLE IF NOT EXISTS `user_course_follow` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户ID',
    `courseId` BIGINT NOT NULL COMMENT '课程ID',
    `followStatus` VARCHAR(32) DEFAULT 'watching' COMMENT '关注状态：watching/completed/dropped',
    `lastLessonId` BIGINT NULL COMMENT '最后学习的节ID',
    `progressPercent` INT DEFAULT 0 NOT NULL COMMENT '学习进度百分比',
    `totalStudyTime` INT DEFAULT 0 NOT NULL COMMENT '累计学习时长(秒)',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_user_course` (`userId`, `courseId`),
    INDEX `idx_courseId` (`courseId`)
) COMMENT '用户收藏的课程学习记录' COLLATE = utf8mb4_unicode_ci;

-- 社区圈子表
CREATE TABLE IF NOT EXISTS `circle` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `title` VARCHAR(256) NULL COMMENT '标题',
    `coverImgUrl` VARCHAR(1024) NULL COMMENT '封面图URL',
    `desc` TEXT NULL COMMENT '描述',
    `type` VARCHAR(64) NULL COMMENT '圈子类型：study/life/tech/community',
    `creatorId` BIGINT NOT NULL COMMENT '创建者ID',
    `memberCount` INT DEFAULT 0 NOT NULL COMMENT '成员数',
    `postCount` INT DEFAULT 0 NOT NULL COMMENT '帖子数',
    `isPublic` TINYINT DEFAULT 1 NOT NULL COMMENT '是否公开：0-私有，1-公开',
    `isVerified` TINYINT DEFAULT 0 NOT NULL COMMENT '是否认证：0-未认证，1-已认证',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_creatorId` (`creatorId`),
    INDEX `idx_type` (`type`),
    INDEX `idx_isPublic` (`isPublic`),
    INDEX `idx_isVerified` (`isVerified`)
) COMMENT '社区圈子' COLLATE = utf8mb4_unicode_ci;

-- 用户圈子关注表
CREATE TABLE IF NOT EXISTS `user_circle_follow` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户ID',
    `circleId` BIGINT NOT NULL COMMENT '圈子ID',
    `joinTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '加入时间',
    `isMuted` TINYINT DEFAULT 0 NOT NULL COMMENT '是否静音：0-正常，1-已静音',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE INDEX `uk_user_circle` (`userId`, `circleId`),
    INDEX `idx_circleId` (`circleId`)
) COMMENT '用户圈子关注' COLLATE = utf8mb4_unicode_ci;

-- 圈子帖子表
CREATE TABLE IF NOT EXISTS `circle_post` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `circleId` BIGINT NOT NULL COMMENT '圈子ID',
    `title` VARCHAR(512) NULL COMMENT '标题',
    `content` TEXT NULL COMMENT '内容',
    `authorId` BIGINT NOT NULL COMMENT '作者ID',
    `thumbNum` INT DEFAULT 0 NOT NULL COMMENT '点赞数',
    `favourNum` INT DEFAULT 0 NOT NULL COMMENT '收藏数',
    `commentNum` INT DEFAULT 0 NOT NULL COMMENT '评论数',
    `isTop` TINYINT DEFAULT 0 NOT NULL COMMENT '是否置顶：0-否，1-是',
    `isRecommend` TINYINT DEFAULT 0 NOT NULL COMMENT '是否推荐：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_circleId` (`circleId`),
    INDEX `idx_authorId` (`authorId`),
    INDEX `idx_isTop` (`isTop`),
    INDEX `idx_isRecommend` (`isRecommend`)
) COMMENT '圈子帖子' COLLATE = utf8mb4_unicode_ci;

-- 课程学习记录关联表（多对多: 用户-课程-章节-节）
CREATE TABLE IF NOT EXISTS `user_course_progress_detail` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userCourseProgressId` BIGINT NOT NULL COMMENT '用户学习进度ID',
    `chapterId` BIGINT NULL COMMENT '章节ID',
    `lessonId` BIGINT NULL COMMENT '节ID',
    `lessonOrder` INT DEFAULT 0 NOT NULL COMMENT '节顺序',
    `isCompleted` TINYINT DEFAULT 0 NOT NULL COMMENT '是否完成：0-否，1-是',
    `completedTime` DATETIME NULL COMMENT '完成时间',
    `studyTime` INT DEFAULT 0 NOT NULL COMMENT '学习时长(秒)',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userCourseProgressId` (`userCourseProgressId`),
    INDEX `idx_chapterId` (`chapterId`),
    INDEX `idx_lessonId` (`lessonId`)
) COMMENT '课程学习记录明细' COLLATE = utf8mb4_unicode_ci;

-- 付费订单表
CREATE TABLE IF NOT EXISTS `payment_order` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `orderNo` VARCHAR(64) NOT NULL COMMENT '订单号',
    `userId` BIGINT NOT NULL COMMENT '买家ID',
    `courseId` BIGINT NULL COMMENT '课程ID（为null表示其他类型购买）',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额(元)',
    `currency` VARCHAR(16) DEFAULT 'CNY' COMMENT '货币类型',
    `paymentMethod` VARCHAR(32) NULL COMMENT '支付方式：alipay/wechat/unionpay/card',
    `paymentStatus` VARCHAR(32) DEFAULT 'pending' COMMENT '支付状态：pending/processing/paid/failed/refunded',
    `gatewayOrderId` VARCHAR(128) NULL COMMENT '网关返回的订单ID',
    `callbackTime` DATETIME NULL COMMENT '回调时间',
    `expireTime` DATETIME NULL COMMENT '过期时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `payTime` DATETIME NULL COMMENT '支付完成时间',
    `refundStatus` VARCHAR(32) DEFAULT 'none' COMMENT '退款状态：none/processing/refunded',
    `refundTime` DATETIME NULL COMMENT '退款时间',
    `refundReason` VARCHAR(256) NULL COMMENT '退款原因',
    INDEX `idx_orderNo` (`orderNo`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_courseId` (`courseId`),
    INDEX `idx_paymentStatus` (`paymentStatus`),
    INDEX `idx_createTime` (`createTime`)
) COMMENT '付费订单' COLLATE = utf8mb4_unicode_ci;

-- 退款记录表
CREATE TABLE IF NOT EXISTS `payment_refund` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `orderId` BIGINT NOT NULL COMMENT '关联订单ID',
    `refundNo` VARCHAR(64) NOT NULL COMMENT '退款单号',
    `amount` DECIMAL(10,2) NOT NULL COMMENT '退款金额(元)',
    `refundReason` VARCHAR(256) NULL COMMENT '退款原因',
    `refundStatus` VARCHAR(32) DEFAULT 'pending' COMMENT '退款状态：pending/processing/succeeded/failed',
    `gatewayRefundId` VARCHAR(128) NULL COMMENT '网关退款ID',
    `refundTime` DATETIME NULL COMMENT '退款处理时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    INDEX `idx_orderId` (`orderId`),
    INDEX `idx_refundNo` (`refundNo`),
    INDEX `idx_refundStatus` (`refundStatus`)
) COMMENT '退款记录' COLLATE = utf8mb4_unicode_ci;