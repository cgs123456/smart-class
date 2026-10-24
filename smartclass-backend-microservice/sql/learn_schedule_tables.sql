-- 学习计划表
CREATE TABLE IF NOT EXISTS `learn_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `userId` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) NOT NULL COMMENT '计划标题',
  `description` text COMMENT '计划描述',
  `startDate` date NOT NULL COMMENT '开始日期',
  `endDate` date NOT NULL COMMENT '结束日期',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态(0-进行中 1-已完成 2-已放弃)',
  `dailyMinutes` int DEFAULT 30 COMMENT '每日学习目标(分钟)',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_userId` (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划';

CREATE TABLE IF NOT EXISTS `learn_schedule_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `scheduleId` bigint NOT NULL COMMENT '计划ID',
  `courseId` bigint DEFAULT NULL COMMENT '课程ID',
  `dailyWordCount` int DEFAULT 0 COMMENT '每日单词数',
  `dailyArticleCount` int DEFAULT 0 COMMENT '每日文章数',
  `dayOfWeek` tinyint DEFAULT NULL COMMENT '星期几(1-7)',
  `timeSlot` varchar(32) DEFAULT NULL COMMENT '时间段',
  `isDelete` tinyint NOT NULL DEFAULT 0,
  `createTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updateTime` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_scheduleId` (`scheduleId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习计划项';
