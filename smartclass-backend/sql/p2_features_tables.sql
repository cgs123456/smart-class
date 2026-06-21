-- P2功能模块建表SQL

-- 口语练习题目表
CREATE TABLE IF NOT EXISTS `oral_practice` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) NOT NULL COMMENT '题目标题',
  `description` text COMMENT '题目描述/场景说明',
  `category` varchar(64) NOT NULL COMMENT '分类(daily/travel/business/exam/free)',
  `difficulty` tinyint NOT NULL DEFAULT 1 COMMENT '难度(1-5)',
  `referenceAnswer` text COMMENT '参考答案',
  `keywords` varchar(512) DEFAULT NULL COMMENT '关键词(逗号分隔)',
  `audioUrl` varchar(512) DEFAULT NULL COMMENT '示范音频URL',
  `imageUrl` varchar(512) DEFAULT NULL COMMENT '配图URL',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口语练习题目';

-- 口语练习记录表
CREATE TABLE IF NOT EXISTS `oral_practice_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `practiceId` bigint NOT NULL COMMENT '题目ID',
  `userAudioUrl` varchar(512) DEFAULT NULL COMMENT '用户录音URL',
  `duration` int DEFAULT NULL COMMENT '录音时长(秒)',
  `aiScore` decimal(3,1) DEFAULT NULL COMMENT 'AI评分(0-10)',
  `aiFeedback` text COMMENT 'AI反馈',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='口语练习记录';

-- 错题表
CREATE TABLE IF NOT EXISTS `wrong_question` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `questionType` varchar(32) NOT NULL COMMENT '题目类型(word/reading/grammar/oral/listening)',
  `questionContent` text NOT NULL COMMENT '题目内容',
  `correctAnswer` text NOT NULL COMMENT '正确答案',
  `userAnswer` text COMMENT '用户答案',
  `analysis` text COMMENT '解析',
  `sourceType` varchar(32) DEFAULT NULL COMMENT '来源类型(course/practice/exam)',
  `sourceId` bigint DEFAULT NULL COMMENT '来源ID',
  `masteryLevel` tinyint NOT NULL DEFAULT 0 COMMENT '掌握程度(0-未掌握 1-部分掌握 2-已掌握)',
  `reviewCount` int NOT NULL DEFAULT 0 COMMENT '复习次数',
  `lastReviewTime` datetime DEFAULT NULL COMMENT '最后复习时间',
  `nextReviewTime` datetime DEFAULT NULL COMMENT '下次复习时间',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_userId_type` (`userId`, `questionType`),
  KEY `idx_nextReviewTime` (`nextReviewTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- 图片生成记录表
CREATE TABLE IF NOT EXISTS `image_generation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `prompt` text NOT NULL COMMENT '提示词',
  `negativePrompt` text COMMENT '反向提示词',
  `style` varchar(64) DEFAULT NULL COMMENT '风格(realistic/anime/oil-painting/watercolor/sketch)',
  `size` varchar(32) DEFAULT '512x512' COMMENT '尺寸',
  `imageUrl` varchar(512) DEFAULT NULL COMMENT '生成图片URL',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0-生成中 1-已完成 2-失败)',
  `errorMessage` varchar(512) DEFAULT NULL COMMENT '错误信息',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片生成记录';

-- 电子书表
CREATE TABLE IF NOT EXISTS `ebook` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `title` varchar(255) NOT NULL COMMENT '书名',
  `author` varchar(128) DEFAULT NULL COMMENT '作者',
  `description` text COMMENT '简介',
  `coverUrl` varchar(512) DEFAULT NULL COMMENT '封面URL',
  `fileUrl` varchar(512) NOT NULL COMMENT '文件URL',
  `fileSize` bigint DEFAULT NULL COMMENT '文件大小(字节)',
  `category` varchar(64) DEFAULT NULL COMMENT '分类',
  `level` varchar(32) DEFAULT NULL COMMENT '难度等级',
  `language` varchar(16) DEFAULT 'en' COMMENT '语言',
  `pageCount` int DEFAULT NULL COMMENT '页数',
  `downloadCount` int NOT NULL DEFAULT 0 COMMENT '下载次数',
  `isVipOnly` tinyint NOT NULL DEFAULT 0 COMMENT '是否VIP专属',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(0-下架 1-上架)',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子书';

-- 用户电子书阅读记录
CREATE TABLE IF NOT EXISTS `ebook_reading_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `userId` bigint NOT NULL COMMENT '用户ID',
  `ebookId` bigint NOT NULL COMMENT '电子书ID',
  `progress` decimal(5,2) DEFAULT 0 COMMENT '阅读进度(%)',
  `lastReadPage` int DEFAULT 0 COMMENT '最后阅读页码',
  `lastReadTime` datetime DEFAULT NULL COMMENT '最后阅读时间',
  `isFavorite` tinyint NOT NULL DEFAULT 0 COMMENT '是否收藏',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_ebook` (`userId`, `ebookId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子书阅读记录';
