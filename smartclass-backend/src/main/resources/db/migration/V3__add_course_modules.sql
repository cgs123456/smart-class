-- Flyway Migration V3: 课程、文章、学习资源、统计等模块表结构

-- 用户与公告阅读记录表
CREATE TABLE IF NOT EXISTS `user_announcement_reader` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `announcementId` BIGINT NOT NULL COMMENT '公告id',
    `readTime` DATETIME NULL COMMENT '阅读时间',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`userId`),
    INDEX `idx_announcement_id` (`announcementId`),
    UNIQUE INDEX `uk_user_announcement` (`userId`, `announcementId`)
) COMMENT '用户公告阅读记录' COLLATE = utf8mb4_unicode_ci;

-- 用户每日签到表
CREATE TABLE IF NOT EXISTS `user_sign_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `checkInDate` DATE NOT NULL COMMENT '签到日期',
    `checkInType` VARCHAR(32) DEFAULT 'daily' COMMENT '签到类型：daily/continuous/bonus',
    `checkInSource` VARCHAR(64) NULL COMMENT '签到来源：app/wechat/mini-program/h5',
    `experienceGained` INT DEFAULT 0 NOT NULL COMMENT '获得经验值',
    `pointsGained` INT DEFAULT 0 NOT NULL COMMENT '获得积分',
    `isContinuous` TINYINT DEFAULT 0 NOT NULL COMMENT '是否连续签到：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    UNIQUE INDEX `uk_user_date` (`userId`, `checkInDate`),
    INDEX `idx_userId` (`userId`),
    INDEX `idx_checkInDate` (`checkInDate`)
) COMMENT '用户每日签到记录' COLLATE = utf8mb4_unicode_ci;

-- 用户消息推送记录表
CREATE TABLE IF NOT EXISTS `user_message_push_record` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `messageType` VARCHAR(32) NOT NULL COMMENT '消息类型：system/announcement/achievement/course_reminder',
    `messageTitle` VARCHAR(256) NOT NULL COMMENT '消息标题',
    `messageContent` TEXT NOT NULL COMMENT '消息内容',
    `relatedId` BIGINT NULL COMMENT '相关记录ID（如公告id、课程id等）',
    `pushChannel` VARCHAR(32) DEFAULT 'all' COMMENT '推送渠道：all/sms/email/push/wechat',
    `pushStatus` VARCHAR(32) DEFAULT 'pending' COMMENT '推送状态：pending/sending/sent/failed',
    `pushErrorMsg` VARCHAR(512) NULL COMMENT '推送错误信息',
    `sendTime` DATETIME NULL COMMENT '发送时间',
    `createdTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updatedTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_userId` (`userId`),
    INDEX `idx_messageType` (`messageType`),
    INDEX `idx_pushStatus` (`pushStatus`)
) COMMENT '用户消息推送记录' COLLATE = utf8mb4_unicode_ci;

-- 课程表
CREATE TABLE IF NOT EXISTS `course` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `title` VARCHAR(256) NOT NULL COMMENT '课程标题',
    `coverImgUrl` VARCHAR(1024) NULL COMMENT '封面图URL',
    `summary` TEXT NULL COMMENT '课程简介',
    `providerId` BIGINT NULL COMMENT '提供者ID（教师或机构ID）',
    `categoryId` BIGINT NULL COMMENT '分类ID',
    `level` VARCHAR(32) NULL COMMENT '难度等级：beginner/intermediate/advanced',
    `duration` VARCHAR(64) NULL COMMENT '预计时长',
    `studyCount` BIGINT DEFAULT 0 NOT NULL COMMENT '学习人数',
    `likeCount` BIGINT DEFAULT 0 NOT NULL COMMENT '点赞数',
    `isPublished` TINYINT DEFAULT 0 NOT NULL COMMENT '是否发布：0-草稿，1-已发布',
    `isFree` TINYINT DEFAULT 1 NOT NULL COMMENT '是否免费：0-付费，1-免费',
    `price` DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格（人民币）',
    `salePrice` DECIMAL(10,2) NULL COMMENT '促销价',
    `tags` VARCHAR(1024) NULL COMMENT '标签列表',
    `description` TEXT NULL COMMENT '详细描述',
    `teachingPlan` TEXT NULL COMMENT '教学计划',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_providerId` (`providerId`),
    INDEX `idx_categoryId` (`categoryId`),
    INDEX `idx_level` (`level`),
    INDEX `idx_isPublished` (`isPublished`),
    INDEX `idx_isFree` (`isFree`),
    INDEX `idx_studyCount` (`studyCount`)
) COMMENT '课程' COLLATE = utf8mb4_unicode_ci;

-- 课程章节表
CREATE TABLE IF NOT EXISTS `course_chapter` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `courseId` BIGINT NOT NULL COMMENT '课程ID',
    `chapterName` VARCHAR(128) NOT NULL COMMENT '章节名称',
    `chapterOrder` INT DEFAULT 0 NOT NULL COMMENT '章节顺序',
    `isPublished` TINYINT DEFAULT 0 NOT NULL COMMENT '是否发布：0-草稿，1-已发布',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_courseId` (`courseId`),
    INDEX `idx_chapterOrder` (`chapterOrder`),
    UNIQUE INDEX `uk_course_chapter` (`courseId`, `chapterName`)
) COMMENT '课程章节' COLLATE = utf8mb4_unicode_ci;

-- 课程节(课时)表
CREATE TABLE IF NOT EXISTS `course_lesson` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `chapterId` BIGINT NOT NULL COMMENT '章节ID',
    `lessonName` VARCHAR(128) NOT NULL COMMENT '节标题',
    `lessonOrder` INT DEFAULT 0 NOT NULL COMMENT '节顺序',
    `videoUrl` VARCHAR(1024) NULL COMMENT '视频URL',
    `videoCoverUrl` VARCHAR(1024) NULL COMMENT '视频封面URL',
    `duration` INT DEFAULT 0 NOT NULL COMMENT '时长(秒)',
    `isFree` TINYINT DEFAULT 1 NOT NULL COMMENT '是否免费：0-付费，1-免费',
    `isPublished` TINYINT DEFAULT 0 NOT NULL COMMENT '是否发布：0-草稿，1-已发布',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_chapterId` (`chapterId`),
    INDEX `idx_lessonOrder` (`lessonOrder`),
    INDEX `idx_isPublished` (`isPublished`)
) COMMENT '课程节(课时)' COLLATE = utf8mb4_unicode_ci;

-- 用户学习进度表
CREATE TABLE IF NOT EXISTS `user_course_progress` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `userId` BIGINT NOT NULL COMMENT '用户id',
    `courseId` BIGINT NOT NULL COMMENT '课程ID',
    `chapterId` BIGINT NULL COMMENT '章节ID',
    `lessonId` BIGINT NULL COMMENT '节ID',
    `progressPercent` INT DEFAULT 0 NOT NULL COMMENT '学习进度百分比',
    `status` VARCHAR(32) DEFAULT 'not_started' COMMENT '状态：not_started/in_progress/completed/paused',
    `lastLessonTime` DATETIME NULL COMMENT '最后学习时间',
    `totalStudyTime` INT DEFAULT 0 NOT NULL COMMENT '累计学习时长(秒)',
    `completedLessons` INT DEFAULT 0 NOT NULL COMMENT '已完成节数',
    `totalLessons` INT DEFAULT 0 NOT NULL COMMENT '总节数',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX `uk_user_course` (`userId`, `courseId`),
    INDEX `idx_courseId` (`courseId`),
    INDEX `idx_status` (`status`)
) COMMENT '用户学习进度' COLLATE = utf8mb4_unicode_ci;

-- 文章表
CREATE TABLE IF NOT EXISTS `article` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `title` VARCHAR(256) NOT NULL COMMENT '文章标题',
    `coverImgUrl` VARCHAR(1024) NULL COMMENT '封面图URL',
    `content` TEXT NOT NULL COMMENT '文章内容（HTML格式）',
    `authorId` BIGINT NOT NULL COMMENT '作者ID',
    `categoryId` BIGINT NULL COMMENT '分类ID',
    `tags` VARCHAR(1024) NULL COMMENT '标签列表',
    `viewCount` BIGINT DEFAULT 0 NOT NULL COMMENT '阅读量',
    `likeCount` BIGINT DEFAULT 0 NOT NULL COMMENT '点赞数',
    `collectCount` BIGINT DEFAULT 0 NOT NULL COMMENT '收藏数',
    `commentCount` INT DEFAULT 0 NOT NULL COMMENT '评论数',
    `isPublished` TINYINT DEFAULT 0 NOT NULL COMMENT '是否发布：0-草稿，1-已发布',
    `isTop` TINYINT DEFAULT 0 NOT NULL COMMENT '是否置顶：0-否，1-是',
    `isRecommend` TINYINT DEFAULT 0 NOT NULL COMMENT '是否推荐：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX `idx_authorId` (`authorId`),
    INDEX `idx_categoryId` (`categoryId`),
    INDEX `idx_isPublished` (`isPublished`),
    INDEX `idx_isTop` (`isTop`),
    INDEX `idx_isRecommend` (`isRecommend`),
    INDEX `idx_viewCount` (`viewCount`)
) COMMENT '文章' COLLATE = utf8mb4_unicode_ci;

-- 文章分类表
CREATE TABLE IF NOT EXISTS `article_category` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `name` VARCHAR(128) NOT NULL COMMENT '分类名称',
    `code` VARCHAR(64) NOT NULL COMMENT '分类编码',
    `description` VARCHAR(512) NULL COMMENT '分类描述',
    `sort` INT DEFAULT 0 NOT NULL COMMENT '排序',
    `isSystem` TINYINT DEFAULT 1 NOT NULL COMMENT '是否系统预设：0-否，1-是',
    `isEnabled` TINYINT DEFAULT 1 NOT NULL COMMENT '是否启用：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE INDEX `uk_code` (`code`),
    INDEX `idx_isEnabled` (`isEnabled`)
) COMMENT '文章分类' COLLATE = utf8mb4_unicode_ci;

-- 文章标签表
CREATE TABLE IF NOT EXISTS `article_tag` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `name` VARCHAR(64) NOT NULL COMMENT '标签名称',
    `code` VARCHAR(64) NOT NULL COMMENT '标签编码',
    `category` VARCHAR(64) NULL COMMENT '标签分类',
    `sort` INT DEFAULT 0 NOT NULL COMMENT '排序',
    `isSystem` TINYINT DEFAULT 1 NOT NULL COMMENT '是否系统预设：0-否，1-是',
    `isEnabled` TINYINT DEFAULT 1 NOT NULL COMMENT '是否启用：0-否，1-是',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    `updateTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `isDelete` TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    UNIQUE INDEX `uk_code` (`code`),
    INDEX `idx_isEnabled` (`isEnabled`)
) COMMENT '文章标签' COLLATE = utf8mb4_unicode_ci;

-- 文章标签关联表（多对多）
CREATE TABLE IF NOT EXISTS `article_tag_relation` (
    `id` BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    `articleId` BIGINT NOT NULL COMMENT '文章ID',
    `tagId` BIGINT NOT NULL COMMENT '标签ID',
    `createTime` DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    INDEX `idx_articleId` (`articleId`),
    INDEX `idx_tagId` (`tagId`),
    UNIQUE INDEX `uk_article_tag` (`articleId`, `tagId`)
) COMMENT '文章标签关联' COLLATE = utf8mb4_unicode_ci;