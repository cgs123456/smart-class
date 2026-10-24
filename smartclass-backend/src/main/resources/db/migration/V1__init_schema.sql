-- Flyway Migration V1: 初始核心表结构
-- 创建时间: 2024-01-01
-- 描述: 智云星课核心业务表结构 (用户、帖子、评论、AI对话等)

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `smart_class` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `smart_class`;

-- =============================================
-- 1. 用户核心表
-- =============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userAccount` VARCHAR(256) NOT NULL COMMENT '账号',
    `userPassword` VARCHAR(512) NOT NULL COMMENT '密码',
    `userGender` INT NULL COMMENT '性别 0-男 1-女 2-保密',
    `userPhone` VARCHAR(256) NULL COMMENT '手机号',
    `unionId` VARCHAR(256) NULL COMMENT '微信开放平台id',
    `mpOpenId` VARCHAR(256) NULL COMMENT '公众号openId',
    `userName` VARCHAR(256) NULL COMMENT '用户昵称',
    `userAvatar` VARCHAR(1024) NULL COMMENT '用户头像',
    `userProfile` VARCHAR(512) NULL COMMENT '用户简介',
    `userRole` VARCHAR(256) DEFAULT 'student' NOT NULL COMMENT '用户角色：student/teacher/admin/ban',
    `province` VARCHAR(100) NULL COMMENT '省份',
    `city` VARCHAR(100) NULL COMMENT '城市',
    `district` VARCHAR(100) NULL COMMENT '区县',
    `wechatId` VARCHAR(256) NULL COMMENT '微信号',
    `userEmail` VARCHAR(256) NULL COMMENT '邮箱',
    `birthday` DATETIME NULL COMMENT '生日',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE INDEX `idx_userAccount` (`userAccount`),
    INDEX `idx_unionId` (`unionId`),
    INDEX `idx_mpOpenId` (`mpOpenId`),
    INDEX `idx_userPhone` (`userPhone`)
) COMMENT '用户' COLLATE = utf8mb4_unicode_ci;

-- 用户等级表
CREATE TABLE IF NOT EXISTS `user_level` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `level` INT NOT NULL COMMENT '等级数值',
    `levelName` VARCHAR(64) NOT NULL COMMENT '等级名称',
    `iconUrl` VARCHAR(1024) NULL COMMENT '等级图标URL',
    `minExperience` INT NOT NULL COMMENT '最小经验值',
    `maxExperience` INT NOT NULL COMMENT '最大经验值',
    `description` VARCHAR(512) NULL COMMENT '等级描述',
    `privileges` VARCHAR(1024) NULL COMMENT '等级特权，JSON格式',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_level` (`level`)
) COMMENT '用户等级' COLLATE = utf8mb4_unicode_ci;

-- 用户学习统计表
CREATE TABLE IF NOT EXISTS `user_learning_stats` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `level` INT DEFAULT 1 NOT NULL COMMENT '当前等级',
    `experience` INT DEFAULT 0 NOT NULL COMMENT '当前经验值',
    `nextLevelExp` INT DEFAULT 100 NOT NULL COMMENT '下一级所需经验值',
    `learningDays` INT DEFAULT 0 NOT NULL COMMENT '学习天数',
    `continuousCheckIn` INT DEFAULT 0 NOT NULL COMMENT '连续打卡天数',
    `totalCheckIn` INT DEFAULT 0 NOT NULL COMMENT '总打卡天数',
    `totalPoints` INT DEFAULT 0 NOT NULL COMMENT '总积分',
    `totalBadges` INT DEFAULT 0 NOT NULL COMMENT '获得徽章数',
    `lastCheckInTime` DATETIME NULL COMMENT '最后打卡时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `idx_userId` (`userId`),
    INDEX `idx_level` (`level`),
    INDEX `idx_experience` (`experience`),
    INDEX `idx_learningDays` (`learningDays`),
    INDEX `idx_continuousCheckIn` (`continuousCheckIn`)
) COMMENT '用户学习统计' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 2. 帖子/圈子模块表
-- =============================================

-- 帖子表
CREATE TABLE IF NOT EXISTS `post` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `title` VARCHAR(512) NULL COMMENT '标题',
    `content` TEXT NULL COMMENT '内容',
    `tags` VARCHAR(1024) NULL COMMENT '标签列表（json 数组）',
    `thumbNum` INT DEFAULT 0 NOT NULL COMMENT '点赞数',
    `favourNum` INT DEFAULT 0 NOT NULL COMMENT '收藏数',
    `commentNum` INT DEFAULT 0 NOT NULL COMMENT '评论数',
    `userId` BIGINT NOT NULL COMMENT '创建用户 id',
    `country` VARCHAR(100) NULL COMMENT '国家',
    `city` VARCHAR(100) NULL COMMENT '城市',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    `type` VARCHAR(50) NOT NULL COMMENT '帖子类型，如学习/生活/技巧',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_createTime` (`createTime`),
    INDEX `idx_type` (`type`),
    INDEX `idx_isDelete` (`isDelete`),
    CONSTRAINT `fk_post_user` FOREIGN KEY (`userId`) REFERENCES `user`(`id`)
) COMMENT '帖子' COLLATE = utf8mb4_unicode_ci;

-- 帖子点赞表（硬删除）
CREATE TABLE IF NOT EXISTS `post_thumb` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `postId` BIGINT NOT NULL COMMENT '帖子 id',
    `userId` BIGINT NOT NULL COMMENT '创建用户 id',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_post_user` (`postId`, `userId`),
    INDEX `idx_postId` (`postId`),
    INDEX `idx_userId` (`userId`)
) COMMENT '帖子点赞' COLLATE = utf8mb4_unicode_ci;

-- 帖子收藏表（硬删除）
CREATE TABLE IF NOT EXISTS `post_favour` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `postId` BIGINT NOT NULL COMMENT '帖子 id',
    `userId` BIGINT NOT NULL COMMENT '创建用户 id',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_post_user` (`postId`, `userId`),
    INDEX `idx_postId` (`postId`),
    INDEX `idx_userId` (`userId`)
) COMMENT '帖子收藏' COLLATE = utf8mb4_unicode_ci;

-- 帖子评论表
CREATE TABLE IF NOT EXISTS `post_comment` (
    `id` BIGINT AUTO_INCREMENT COMMENT '评论ID' PRIMARY KEY,
    `postId` BIGINT NOT NULL COMMENT '帖子ID，关联到post表',
    `userId` BIGINT NOT NULL COMMENT '评论者ID，关联到user表',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `country` VARCHAR(100) NULL COMMENT '国家',
    `city` VARCHAR(100) NULL COMMENT '城市',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-否，1-是',
    INDEX `idx_postId` (`postId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_createTime` (`createTime`),
    CONSTRAINT `fk_comment_post` FOREIGN KEY (`postId`) REFERENCES `post`(`id`),
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`userId`) REFERENCES `user`(`id`)
) COMMENT '帖子评论' COLLATE = utf8mb4_unicode_ci;

-- 帖子评论回复表
CREATE TABLE IF NOT EXISTS `post_comment_reply` (
    `id` BIGINT AUTO_INCREMENT COMMENT '回复ID' PRIMARY KEY,
    `postId` BIGINT NOT NULL COMMENT '帖子ID，关联到post表',
    `commentId` BIGINT NOT NULL COMMENT '评论ID，关联到post_comment表',
    `userId` BIGINT NOT NULL COMMENT '回复者ID，关联到user表',
    `content` TEXT NOT NULL COMMENT '回复内容',
    `country` VARCHAR(100) NULL COMMENT '国家',
    `city` VARCHAR(100) NULL COMMENT '城市',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-否，1-是',
    INDEX `idx_postId` (`postId`),
    INDEX `idx_commentId` (`commentId`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_createTime` (`createTime`),
    CONSTRAINT `fk_reply_post` FOREIGN KEY (`postId`) REFERENCES `post` (`id`),
    CONSTRAINT `fk_reply_comment` FOREIGN KEY (`commentId`) REFERENCES `post_comment` (`id`),
    CONSTRAINT `fk_reply_user` FOREIGN KEY (`userId`) REFERENCES `user` (`id`)
) COMMENT '帖子评论回复' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 3. AI 对话模块表
-- =============================================

-- AI 分身表
CREATE TABLE IF NOT EXISTS `ai_avatar` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT 'AI分身名称',
    `description` TEXT NULL COMMENT '描述',
    `avatarImgUrl` VARCHAR(1024) NULL COMMENT '头像URL',
    `category` VARCHAR(64) NULL COMMENT '分类',
    `modelType` VARCHAR(64) NULL COMMENT '模型类型',
    `personality` TEXT NULL COMMENT '人设',
    `abilities` TEXT NULL COMMENT '能力',
    `tags` VARCHAR(1024) NULL COMMENT '标签',
    `rating` DECIMAL(3,2) DEFAULT 0.00 COMMENT '评分',
    `usageCount` BIGINT DEFAULT 0 COMMENT '使用次数',
    `baseUrl` VARCHAR(512) NULL COMMENT 'Dify API 基础URL',
    `avatarAuth` VARCHAR(512) NULL COMMENT 'Dify API 认证Key',
    `status` INT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `isPublic` TINYINT DEFAULT 1 COMMENT '是否公开：0-私有，1-公开',
    `creatorId` BIGINT NULL COMMENT '创建者ID',
    `adminId` BIGINT NULL COMMENT '管理员ID',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_category` (`category`),
    INDEX `idx_status` (`status`),
    INDEX `idx_isPublic` (`isPublic`),
    INDEX `idx_creatorId` (`creatorId`)
) COMMENT 'AI分身' COLLATE = utf8mb4_unicode_ci;

-- AI 聊天历史表
CREATE TABLE IF NOT EXISTS `ai_avatar_chat_history` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `aiAvatarId` BIGINT NOT NULL COMMENT 'AI分身id',
    `sessionId` VARCHAR(128) NULL COMMENT 'Dify会话ID',
    `role` VARCHAR(32) NOT NULL COMMENT '角色：user/ai',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `messageId` VARCHAR(128) NULL COMMENT 'Dify消息ID',
    `tokens` INT DEFAULT 0 COMMENT 'Token数量',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_aiAvatarId` (`aiAvatarId`),
    INDEX `idx_sessionId` (`sessionId`),
    INDEX `idx_createTime` (`createTime`),
    INDEX `idx_role` (`role`)
) COMMENT 'AI分身聊天历史' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 4. 私信/聊天模块表
-- =============================================

-- 私聊会话表
CREATE TABLE IF NOT EXISTS `private_chat_session` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId1` BIGINT NOT NULL COMMENT '用户1 ID',
    `userId2` BIGINT NOT NULL COMMENT '用户2 ID',
    `lastMessageId` BIGINT NULL COMMENT '最后一条消息ID',
    `lastMessageContent` VARCHAR(512) NULL COMMENT '最后一条消息内容',
    `lastMessageTime` DATETIME NULL COMMENT '最后消息时间',
    `unreadCount1` INT DEFAULT 0 NOT NULL COMMENT '用户1未读数',
    `unreadCount2` INT DEFAULT 0 NOT NULL COMMENT '用户2未读数',
    `status` INT DEFAULT 1 NOT NULL COMMENT '状态：0-已删除/屏蔽，1-正常',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_user_pair` (`userId1`, `userId2`),
    INDEX `idx_userId1` (`userId1`),
    INDEX `idx_userId2` (`userId2`),
    INDEX `idx_lastMessageTime` (`lastMessageTime`)
) COMMENT '私聊会话' COLLATE = utf8mb4_unicode_ci;

-- 私聊消息表
CREATE TABLE IF NOT EXISTS `private_message` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `sessionId` BIGINT NOT NULL COMMENT '会话ID',
    `senderId` BIGINT NOT NULL COMMENT '发送者ID',
    `receiverId` BIGINT NOT NULL COMMENT '接收者ID',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `isRead` TINYINT DEFAULT 0 NOT NULL COMMENT '是否已读：0-未读，1-已读',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_sessionId` (`sessionId`),
    INDEX `idx_senderId` (`senderId`),
    INDEX `idx_receiverId` (`receiverId`),
    INDEX `idx_createTime` (`createTime`),
    INDEX `idx_isRead` (`isRead`)
) COMMENT '私聊消息' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 4. 系统配置表
-- =============================================

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `configKey` VARCHAR(128) NOT NULL COMMENT '配置键',
    `configValue` TEXT NULL COMMENT '配置值',
    `configType` VARCHAR(32) DEFAULT 'string' COMMENT '配置类型：string/int/boolean/json',
    `description` VARCHAR(512) NULL COMMENT '配置描述',
    `category` VARCHAR(64) NULL COMMENT '配置分类',
    `isPublic` TINYINT DEFAULT 0 COMMENT '是否前端可见：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE INDEX `uk_configKey` (`configKey`),
    INDEX `idx_category` (`category`)
) COMMENT '系统配置' COLLATE = utf8mb4_unicode_ci;

-- =============================================
-- 5. 初始化基础数据
-- =============================================

-- 初始化用户等级数据
INSERT IGNORE INTO `user_level` (`level`, `levelName`, `minExperience`, `maxExperience`, `description`, `privileges`) VALUES
(1, '初学者', 0, 99, '刚开始学习之旅', '{"canCreatePost": true, "canComment": true}'),
(2, '学习者', 100, 499, '积累基础知识', '{"canCreatePost": true, "canComment": true, "canCreateCourse": false}'),
(3, '进阶者', 500, 1499, '掌握核心技能', '{"canCreatePost": true, "canComment": true, "canCreateCourse": true}'),
(4, '专家', 1500, 4999, '领域深度实践者', '{"canCreatePost": true, "canComment": true, "canCreateCourse": true, "canModerate": true}'),
(5, '大师', 5000, 999999, '行业权威专家', '{"canCreatePost": true, "canComment": true, "canCreateCourse": true, "canModerate": true, "canManageUsers": true}');

-- 初始化系统配置
INSERT IGNORE INTO `system_config` (`configKey`, `configValue`, `configType`, `description`, `category`, `isPublic`) VALUES
('app.name', '智云星课', 'string', '应用名称', 'basic', 1),
('app.version', '1.0.0', 'string', '应用版本', 'basic', 1),
('user.default.password', 'Smartclass@2024', 'string', '用户默认密码', 'user', 0),
('user.default.avatar', '/default.jpg', 'string', '用户默认头像', 'user', 1),
('jwt.expiration', '604800000', 'int', 'JWT过期时间(毫秒)', 'security', 0),
('captcha.enable', 'true', 'boolean', '验证码开关', 'security', 0),
('rag.enabled', 'false', 'boolean', 'RAG功能开关', 'ai', 0),
('exercise.enabled', 'false', 'boolean', 'AI习题生成功能开关', 'ai', 0);