-- Flyway Migration V2: 学习模块、课程、文章、词汇等扩展表结构

-- 用户与每日文章关联表
CREATE TABLE IF NOT EXISTS `user_daily_article` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `articleId` BIGINT NOT NULL COMMENT '文章id',
    `isRead` TINYINT DEFAULT 0 NOT NULL COMMENT '是否阅读：0-否，1-是',
    `readTime` DATETIME NULL COMMENT '阅读时间',
    `isLiked` TINYINT DEFAULT 0 NOT NULL COMMENT '是否点赞：0-否，1-是',
    `likeTime` DATETIME NULL COMMENT '点赞时间',
    `isCollected` TINYINT DEFAULT 0 NOT NULL COMMENT '是否收藏：0-否，1-是',
    `collectTime` DATETIME NULL COMMENT '收藏时间',
    `commentContent` TEXT NULL COMMENT '评论内容',
    `commentTime` DATETIME NULL COMMENT '评论时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_articleId` (`articleId`),
    UNIQUE INDEX `uk_user_article` (`userId`, `articleId`)
) COMMENT '用户与每日文章关联' COLLATE = utf8mb4_unicode_ci;

-- 用户与每日单词关联表
CREATE TABLE IF NOT EXISTS `user_daily_word` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `wordId` BIGINT NOT NULL COMMENT '单词id',
    `isStudied` TINYINT DEFAULT 0 NOT NULL COMMENT '是否学习：0-否，1-是',
    `studyTime` DATETIME NULL COMMENT '学习时间',
    `isLiked` TINYINT DEFAULT 0 NOT NULL COMMENT '是否点赞：0-否，1-是',
    `likeTime` DATETIME NULL COMMENT '点赞时间',
    `isCollected` TINYINT DEFAULT 0 NOT NULL COMMENT '是否收藏：0-否，1-是',
    `collectTime` DATETIME NULL COMMENT '收藏时间',
    `noteContent` TEXT NULL COMMENT '笔记内容',
    `noteTime` DATETIME NULL COMMENT '笔记时间',
    `masteryLevel` TINYINT DEFAULT 0 NOT NULL COMMENT '掌握程度：0-未知，1-生词，2-熟悉，3-掌握',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_wordId` (`wordId`),
    UNIQUE INDEX `uk_user_word` (`userId`, `wordId`)
) COMMENT '用户与每日单词关联' COLLATE = utf8mb4_unicode_ci;

-- 用户每日学习目标表
CREATE TABLE IF NOT EXISTS `user_daily_goal` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `goalDate` DATE NOT NULL COMMENT '目标日期',
    `totalGoals` INT DEFAULT 0 NOT NULL COMMENT '总目标数',
    `completedGoals` INT DEFAULT 0 NOT NULL COMMENT '已完成目标数',
    `progressPercent` INT DEFAULT 0 NOT NULL COMMENT '完成百分比',
    `isCompleted` TINYINT DEFAULT 0 NOT NULL COMMENT '是否全部完成：0-否，1-是',
    `completedTime` DATETIME NULL COMMENT '全部完成时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_goalDate` (`goalDate`),
    UNIQUE INDEX `uk_user_date` (`userId`, `goalDate`)
) COMMENT '用户每日学习目标' COLLATE = utf8mb4_unicode_ci;

-- 学习目标类型表
CREATE TABLE IF NOT EXISTS `goal_type` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT '目标类型名称',
    `code` VARCHAR(64) NOT NULL COMMENT '目标类型编码',
    `icon` VARCHAR(1024) NULL COMMENT '图标URL',
    `description` VARCHAR(512) NULL COMMENT '描述',
    `category` VARCHAR(64) NOT NULL COMMENT '分类',
    `unit` VARCHAR(32) NULL COMMENT '单位',
    `defaultValue` INT DEFAULT 1 NOT NULL COMMENT '默认值',
    `minValue` INT DEFAULT 1 NOT NULL COMMENT '最小值',
    `maxValue` INT NULL COMMENT '最大值',
    `points` INT DEFAULT 5 NOT NULL COMMENT '完成可获得积分',
    `experience` INT DEFAULT 10 NOT NULL COMMENT '完成可获得经验值',
    `isSystem` TINYINT DEFAULT 1 NOT NULL COMMENT '是否系统预设：0-否，1-是',
    `isEnabled` TINYINT DEFAULT 1 NOT NULL COMMENT '是否启用：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_code` (`code`),
    INDEX `idx_category` (`category`)
) COMMENT '学习目标类型' COLLATE = utf8mb4_unicode_ci;

-- 用户学习目标项表
CREATE TABLE IF NOT EXISTS `user_goal_item` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `dailyGoalId` BIGINT NOT NULL COMMENT '每日目标id',
    `goalTypeId` BIGINT NOT NULL COMMENT '目标类型id',
    `goalDate` DATE NOT NULL COMMENT '目标日期',
    `title` VARCHAR(256) NOT NULL COMMENT '目标标题',
    `targetValue` INT DEFAULT 1 NOT NULL COMMENT '目标值',
    `currentValue` INT DEFAULT 0 NOT NULL COMMENT '当前值',
    `progressPercent` INT DEFAULT 0 NOT NULL COMMENT '完成百分比',
    `isCompleted` TINYINT DEFAULT 0 NOT NULL COMMENT '是否完成：0-否，1-是',
    `completedTime` DATETIME NULL COMMENT '完成时间',
    `isRewarded` TINYINT DEFAULT 0 NOT NULL COMMENT '是否已发放奖励：0-否，1-是',
    `rewardTime` DATETIME NULL COMMENT '奖励发放时间',
    `sort` INT DEFAULT 0 NOT NULL COMMENT '排序，数字越小排序越靠前',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_dailyGoalId` (`dailyGoalId`),
    INDEX `idx_goalTypeId` (`goalTypeId`),
    INDEX `idx_goalDate` (`goalDate`),
    INDEX `idx_isCompleted` (`isCompleted`),
    UNIQUE INDEX `uk_user_goal_item` (`userId`, `dailyGoalId`, `goalTypeId`)
) COMMENT '用户学习目标项' COLLATE = utf8mb4_unicode_ci;

-- 用户学习记录表
CREATE TABLE IF NOT EXISTS `user_learning_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `recordDate` DATE NOT NULL COMMENT '记录日期',
    `recordType` VARCHAR(64) NOT NULL COMMENT '记录类型，如：word_card, listening, course等',
    `relatedId` BIGINT NULL COMMENT '关联ID，如单词ID、课程ID等',
    `lessonNumber` INT DEFAULT 0 NOT NULL COMMENT '课程中的课次或子活动编号',
    `duration` INT DEFAULT 0 NOT NULL COMMENT '学习时长(秒)',
    `count` INT DEFAULT 1 NOT NULL COMMENT '学习数量',
    `points` INT DEFAULT 0 NOT NULL COMMENT '获得积分',
    `experience` INT DEFAULT 0 NOT NULL COMMENT '获得经验值',
    `accuracy` DECIMAL(5,2) DEFAULT 0 NOT NULL COMMENT '正确率(百分比)',
    `status` VARCHAR(32) DEFAULT 'completed' NOT NULL COMMENT '活动状态，如：in_progress, completed, failed',
    `remark` VARCHAR(512) NULL COMMENT '备注',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_recordDate` (`recordDate`),
    INDEX `idx_recordType` (`recordType`),
    UNIQUE INDEX `idx_unique_record` (`userId`, `recordDate`, `recordType`, `relatedId`)
) COMMENT '用户学习记录' COLLATE = utf8mb4_unicode_ci;

-- 成就定义表
CREATE TABLE IF NOT EXISTS `achievement` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT '成就名称',
    `description` VARCHAR(512) NOT NULL COMMENT '成就描述',
    `iconUrl` VARCHAR(1024) NOT NULL COMMENT '成就图标URL',
    `badgeUrl` VARCHAR(1024) NOT NULL COMMENT '成就徽章URL',
    `bannerUrl` VARCHAR(1024) NULL COMMENT '成就横幅URL',
    `category` VARCHAR(64) NOT NULL COMMENT '成就分类，如：学习、社交、活动等',
    `level` TINYINT DEFAULT 1 NOT NULL COMMENT '成就等级：1-普通，2-稀有，3-史诗，4-传说',
    `points` INT DEFAULT 0 NOT NULL COMMENT '成就点数',
    `achievementCondition` VARCHAR(512) NOT NULL COMMENT '获取条件描述',
    `conditionType` VARCHAR(64) NOT NULL COMMENT '条件类型，如：course_complete, login_days, article_read等',
    `conditionValue` INT DEFAULT 1 NOT NULL COMMENT '条件值，如完成10门课程，登录30天等',
    `isHidden` TINYINT DEFAULT 0 NOT NULL COMMENT '是否隐藏成就：0-否，1-是，隐藏成就不会提前显示给用户',
    `isSecret` TINYINT DEFAULT 0 NOT NULL COMMENT '是否是彩蛋成就：0-否，1-是，彩蛋成就是特殊发现的成就',
    `rewardType` VARCHAR(64) NULL COMMENT '奖励类型，如：points, badge, coupon等',
    `rewardValue` VARCHAR(256) NULL COMMENT '奖励值，如积分数量、优惠券ID等',
    `sort` INT DEFAULT 0 NOT NULL COMMENT '排序，数字越小排序越靠前',
    `adminId` BIGINT NOT NULL COMMENT '创建管理员id',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_category` (`category`),
    INDEX `idx_level` (`level`),
    INDEX `idx_conditionType` (`conditionType`),
    INDEX `idx_sort` (`sort`)
) COMMENT '成就定义' COLLATE = utf8mb4_unicode_ci;

-- 用户成就表
CREATE TABLE IF NOT EXISTS `user_achievement` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `achievementId` BIGINT NOT NULL COMMENT '成就id',
    `progress` INT DEFAULT 0 NOT NULL COMMENT '当前进度值',
    `progressMax` INT DEFAULT 1 NOT NULL COMMENT '目标进度值',
    `progressPercent` INT DEFAULT 0 NOT NULL COMMENT '进度百分比',
    `isCompleted` TINYINT DEFAULT 0 NOT NULL COMMENT '是否完成：0-否，1-是',
    `completedTime` DATETIME NULL COMMENT '完成时间',
    `isRewarded` TINYINT DEFAULT 0 NOT NULL COMMENT '是否已发放奖励：0-否，1-是',
    `rewardTime` DATETIME NULL COMMENT '奖励发放时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_achievementId` (`achievementId`),
    INDEX `idx_isCompleted` (`isCompleted`),
    UNIQUE INDEX `uk_user_achievement` (`userId`, `achievementId`)
) COMMENT '用户成就' COLLATE = utf8mb4_unicode_ci;

-- 成就展示配置表
CREATE TABLE IF NOT EXISTS `achievement_display` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `achievementId` BIGINT NOT NULL COMMENT '成就id',
    `displayType` VARCHAR(64) NOT NULL COMMENT '展示类型：profile(个人资料页), card(成就卡片), banner(成就横幅), popup(弹窗通知)',
    `title` VARCHAR(256) NULL COMMENT '展示标题，为空则使用成就名称',
    `subtitle` VARCHAR(512) NULL COMMENT '展示副标题',
    `imageUrl` VARCHAR(1024) NULL COMMENT '展示图片URL，为空则使用成就图标',
    `backgroundColor` VARCHAR(32) NULL COMMENT '背景颜色，十六进制颜色代码',
    `textColor` VARCHAR(32) NULL COMMENT '文字颜色，十六进制颜色代码',
    `animationType` VARCHAR(64) NULL COMMENT '动画类型',
    `displayDuration` INT DEFAULT 0 NOT NULL COMMENT '展示时长(秒)，0表示永久展示',
    `sort` INT DEFAULT 0 NOT NULL COMMENT '排序，数字越小排序越靠前',
    `adminId` BIGINT NOT NULL COMMENT '创建管理员id',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_achievementId` (`achievementId`),
    INDEX `idx_displayType` (`displayType`),
    INDEX `idx_sort` (`sort`)
) COMMENT '成就展示配置' COLLATE = utf8mb4_unicode_ci;

-- 用户成就展示记录表
CREATE TABLE IF NOT EXISTS `user_achievement_display` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `achievementId` BIGINT NOT NULL COMMENT '成就id',
    `achievementDisplayId` BIGINT NOT NULL COMMENT '成就展示配置id',
    `isEnabled` TINYINT DEFAULT 1 NOT NULL COMMENT '是否启用展示：0-否，1-是',
    `isPinned` TINYINT DEFAULT 0 NOT NULL COMMENT '是否置顶：0-否，1-是',
    `customTitle` VARCHAR(256) NULL COMMENT '自定义标题，为空则使用默认标题',
    `customImageUrl` VARCHAR(1024) NULL COMMENT '自定义图片URL，为空则使用默认图片',
    `displayCount` INT DEFAULT 0 NOT NULL COMMENT '展示次数',
    `lastDisplayTime` DATETIME NULL COMMENT '最后展示时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_achievementId` (`achievementId`),
    INDEX `idx_achievementDisplayId` (`achievementDisplayId`)
) COMMENT '用户成就展示记录' COLLATE = utf8mb4_unicode_ci;