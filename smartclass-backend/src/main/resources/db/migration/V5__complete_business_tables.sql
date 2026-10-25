-- V5: 补齐 V1-V4 之外的剩余业务表（与 sql/ 手工脚本同源，实体层全部需要）
-- 来源：sql/create_table.sql + payment_tables.sql + p2_features_tables.sql（本仓库手工导入链）
SET FOREIGN_KEY_CHECKS=0;

-- 公告表
create table if not exists announcement
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(256)                       not null comment '公告标题',
    content    text                               not null comment '公告内容',
    priority   int      default 0                 not null comment '优先级，数字越大优先级越高',
    status     tinyint  default 1                 not null comment '状态：0-草稿，1-已发布，2-已下线',
    startTime  datetime                           null comment '公告开始展示时间',
    endTime    datetime                           null comment '公告结束展示时间',
    coverImage varchar(1024)                      null comment '封面图片URL',
    adminId    bigint                             not null comment '发布管理员id',
    viewCount  int      default 0                 not null comment '查看次数',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_adminId (adminId),
    index idx_status (status),
    index idx_priority (priority)
) comment '系统公告' collate = utf8mb4_unicode_ci;


-- 课程分类表
create table if not exists course_category
(
    id          bigint auto_increment comment 'id' primary key,
    name        varchar(128)                       not null comment '分类名称',
    description varchar(512)                       null comment '分类描述',
    icon        varchar(1024)                      null comment '分类图标URL',
    sort        int      default 0                 not null comment '排序权重，数字越大排序越靠前',
    parentId    bigint   default 0                 not null comment '父分类id，0表示一级分类',
    adminId     bigint                             not null comment '创建管理员id',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_parentId (parentId),
    index idx_sort (sort)
) comment '课程分类' collate = utf8mb4_unicode_ci;


-- 讲师表
create table if not exists teacher
(
    id           bigint auto_increment comment 'id' primary key,
    name         varchar(128)                       not null comment '讲师姓名',
    avatar       varchar(1024)                      null comment '讲师头像URL',
    title        varchar(128)                       null comment '讲师职称',
    introduction text                               null comment '讲师简介',
    expertise    varchar(512)                       null comment '专业领域，JSON数组格式',
    userId       bigint                             null comment '关联的用户id，如果讲师也是平台用户',
    adminId      bigint                             not null comment '创建管理员id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
) comment '讲师' collate = utf8mb4_unicode_ci;


-- 课程小节表
create table if not exists course_section
(
    id           bigint auto_increment comment 'id' primary key,
    courseId     bigint                             not null comment '课程id',
    chapterId    bigint                             not null comment '章节id',
    title        varchar(256)                       not null comment '小节标题',
    description  text                               null comment '小节描述',
    videoUrl     varchar(1024)                      null comment '视频URL',
    duration     int      default 0                 not null comment '时长(秒)',
    sort         int      default 0                 not null comment '排序，数字越小排序越靠前',
    isFree       tinyint  default 0                 not null comment '是否免费：0-否，1-是',
    resourceType tinyint  default 0                 not null comment '资源类型：0-视频，1-音频，2-文档，3-图片，4-直播',
    resourceUrl  varchar(1024)                      null comment '资源URL',
    adminId      bigint                             not null comment '创建管理员id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    index idx_courseId (courseId),
    index idx_chapterId (chapterId),
    index idx_sort (sort)
) comment '课程小节' collate = utf8mb4_unicode_ci;


-- 课程资料表
create table if not exists course_material
(
    id            bigint auto_increment comment 'id' primary key,
    courseId      bigint                             not null comment '课程id',
    title         varchar(256)                       not null comment '资料标题',
    description   text                               null comment '资料描述',
    fileUrl       varchar(1024)                      not null comment '文件URL',
    fileSize      bigint   default 0                 not null comment '文件大小(字节)',
    fileType      varchar(64)                        null comment '文件类型',
    downloadCount int      default 0                 not null comment '下载次数',
    sort          int      default 0                 not null comment '排序，数字越小排序越靠前',
    adminId       bigint                             not null comment '创建管理员id',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    index idx_courseId (courseId),
    index idx_sort (sort)
) comment '课程资料' collate = utf8mb4_unicode_ci;


-- 用户课程购买记录表
create table if not exists user_course
(
    id            bigint auto_increment comment 'id' primary key,
    userId        bigint                             not null comment '用户id',
    courseId      bigint                             not null comment '课程id',
    orderNo       varchar(64)                        null comment '订单编号',
    price         decimal(10, 2)                     not null comment '购买价格',
    paymentMethod varchar(32)                        null comment '支付方式',
    paymentTime   datetime                           null comment '支付时间',
    expireTime    datetime                           null comment '过期时间，null表示永久有效',
    status        tinyint  default 1                 not null comment '状态：0-未支付，1-已支付，2-已过期，3-已退款',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_courseId (courseId),
    index idx_orderNo (orderNo),
    unique uk_user_course (userId, courseId)
) comment '用户课程购买记录' collate = utf8mb4_unicode_ci;


-- 课程评价表
create table if not exists course_review
(
    id             bigint auto_increment comment 'id' primary key,
    userId         bigint                             not null comment '用户id',
    courseId       bigint                             not null comment '课程id',
    content        text                               not null comment '评价内容',
    rating         tinyint  default 5                 not null comment '评分(1-5分)',
    likeCount      int      default 0                 not null comment '点赞数',
    replyCount     int      default 0                 not null comment '回复数',
    adminReply     text                               null comment '管理员回复',
    adminReplyTime datetime                           null comment '管理员回复时间',
    status         tinyint  default 1                 not null comment '状态：0-待审核，1-已发布，2-已拒绝',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete       tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_courseId (courseId),
    index idx_rating (rating)
) comment '课程评价' collate = utf8mb4_unicode_ci;


-- 课程收藏表
create table if not exists course_favourite
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                             not null comment '用户id',
    courseId   bigint                             not null comment '课程id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_userId (userId),
    index idx_courseId (courseId),
    unique uk_user_course (userId, courseId)
) comment '课程收藏' collate = utf8mb4_unicode_ci;


-- 每日单词表
CREATE TABLE IF NOT EXISTS daily_word
(
    id                 BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    word               VARCHAR(128) NOT NULL COMMENT '单词',
    pronunciation      VARCHAR(128) NULL COMMENT '音标',
    audioUrl           VARCHAR(2048) NULL COMMENT '发音音频URL',
    translation        VARCHAR(512) NOT NULL COMMENT '翻译',
    example            TEXT NULL COMMENT '例句',
    exampleTranslation TEXT NULL COMMENT '例句翻译',
    difficulty         TINYINT DEFAULT 1 NOT NULL COMMENT '难度等级：1-简单，2-中等，3-困难',
    category           VARCHAR(64) NULL COMMENT '单词分类（如名词、动词、商务英语等）',
    notes              TEXT NULL COMMENT '单词笔记或补充说明',
    likeCount          INT DEFAULT 0 NOT NULL COMMENT '点赞次数',
    publishDate        DATE NOT NULL COMMENT '发布日期',
    adminId            BIGINT NOT NULL COMMENT '创建管理员ID',
    createTime         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime         DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete           TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除：0-未删除，1-已删除',
    INDEX idx_publishDate (publishDate),
    INDEX idx_word (word),
    INDEX idx_category (category),
    INDEX idx_difficulty (difficulty)
) COMMENT '每日单词' COLLATE = utf8mb4_unicode_ci;


-- 每日文章表
create table if not exists daily_article
(
    id          bigint auto_increment comment 'id' primary key,
    title       varchar(256)                       not null comment '文章标题',
    content     text                               not null comment '文章内容',
    summary     varchar(512)                       null comment '文章摘要',
    coverImage  varchar(1024)                      null comment '封面图片URL',
    author      varchar(128)                       null comment '作者',
    source      varchar(256)                       null comment '来源',
    sourceUrl   varchar(1024)                      null comment '原文链接',
    category    varchar(64)                        null comment '文章分类',
    tags        varchar(512)                       null comment '标签，JSON数组格式',
    difficulty  tinyint  default 1                 not null comment '难度等级：1-简单，2-中等，3-困难',
    readTime    int      default 0                 not null comment '预计阅读时间(分钟)',
    publishDate date                               not null comment '发布日期',
    adminId     bigint                             not null comment '创建管理员id',
    viewCount   int      default 0                 not null comment '查看次数',
    likeCount   int      default 0                 not null comment '点赞次数',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_publishDate (publishDate),
    index idx_category (category),
    index idx_difficulty (difficulty)
) comment '每日文章' collate = utf8mb4_unicode_ci;


-- 用户文章阅读记录表
create table if not exists user_article_record
(
    id           bigint auto_increment comment 'id' primary key,
    userId       bigint                             not null comment '用户id',
    articleId    bigint                             not null comment '文章id',
    readStatus   tinyint  default 0                 not null comment '阅读状态：0-未读，1-阅读中，2-已读完',
    readProgress int      default 0                 not null comment '阅读进度(百分比)',
    isLiked      tinyint  default 0                 not null comment '是否点赞',
    userNotes    text                               null comment '用户笔记',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_userId (userId),
    index idx_articleId (articleId),
    unique uk_user_article (userId, articleId)
) comment '用户文章阅读记录' collate = utf8mb4_unicode_ci;


-- 用户AI分身关联表
create table if not exists user_ai_avatar
(
    id             bigint auto_increment comment 'id' primary key,
    userId         bigint                             not null comment '用户id',
    aiAvatarId     bigint                             not null comment 'AI分身id',
    isFavorite     tinyint  default 0                 not null comment '是否收藏：0-否，1-是',
    lastUseTime    datetime                           null comment '最后使用时间',
    useCount       int      default 0                 not null comment '使用次数',
    userRating     decimal(2, 1)                      null comment '用户评分，1-5分',
    userFeedback   text                               null comment '用户反馈',
    customSettings text                               null comment '用户自定义设置，JSON格式',
    createTime     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_userId (userId),
    index idx_aiAvatarId (aiAvatarId),
    index idx_isFavorite (isFavorite),
    unique uk_user_avatar (userId, aiAvatarId)
) comment '用户AI分身关联' collate = utf8mb4_unicode_ci;


-- 用户反馈表
create table if not exists user_feedback
(
    id          bigint auto_increment comment 'id' primary key,
    userId      bigint                             not null comment '用户ID',
    feedbackType varchar(64)                       not null comment '反馈类型',
    title       varchar(256)                       null comment '反馈标题',
    content     text                               not null comment '反馈内容',
    attachment  varchar(1024)                      null comment '附件URL',
    status      tinyint  default 0                 not null comment '处理状态：0-待处理，1-处理中，2-已处理',
    adminId     bigint                             null comment '处理管理员ID',
    processTime datetime                           null comment '处理时间',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_status (status)
) comment '用户反馈' collate = utf8mb4_unicode_ci;


CREATE TABLE `user_feedback_reply`
(
    id          bigint        NOT NULL AUTO_INCREMENT COMMENT 'id',
    feedbackId  bigint        NOT NULL COMMENT '关联的反馈ID',
    senderId    bigint        NOT NULL COMMENT '发送者ID',
    senderRole  tinyint       NOT NULL COMMENT '发送者角色：0-用户，1-管理员',
    content     varchar(1000) NOT NULL COMMENT '回复内容',
    attachment  varchar(1024)          DEFAULT NULL COMMENT '附件URL',
    isRead      tinyint       NOT NULL DEFAULT '0' COMMENT '是否已读：0-未读，1-已读',
    createTime  datetime      NOT NULL COMMENT '创建时间',
    updateTime  datetime      NOT NULL COMMENT '更新时间',
    isDelete    tinyint       NOT NULL DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (id),
    KEY idx_feedback_id (feedbackId),
    KEY idx_sender_id (senderId),
    KEY idx_create_time (createTime)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈回复';


-- 用户生词本表
CREATE TABLE IF NOT EXISTS user_word_book
(
    id             BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,
    userId         BIGINT                             NOT NULL COMMENT '用户id，关联到user表',
    wordId         BIGINT                             NOT NULL COMMENT '单词id，关联到daily_word表',
    learningStatus TINYINT  DEFAULT 0                 NOT NULL COMMENT '学习状态：0-未学习，1-已学习，2-已掌握',
    isCollected    TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否收藏：0-否，1-是',
    collectedTime  DATETIME                           NULL COMMENT '收藏时间',
    difficulty     TINYINT  DEFAULT 1                 NOT NULL COMMENT '难度等级：1-简单，2-中等，3-困难',
    isDeleted      TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除：0-否，1-是',
    createTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_userId (userId),
    INDEX idx_wordId (wordId),
    INDEX idx_learningStatus (learningStatus),
    INDEX idx_isCollected (isCollected),
    INDEX idx_difficulty (difficulty),
    UNIQUE uk_user_word (userId, wordId)
) COMMENT '用户生词本' COLLATE = utf8mb4_unicode_ci;


-- 好友关系表
CREATE TABLE IF NOT EXISTS friend_relationship
(
    id         BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    userId1    BIGINT                             NOT NULL COMMENT '用户1 ID，关联到user表',
    userId2    BIGINT                             NOT NULL COMMENT '用户2 ID，关联到user表',
    status     VARCHAR(20)                        NOT NULL COMMENT '关系状态：pending/accepted/blocked',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_userId1 (userId1),
    INDEX idx_userId2 (userId2),
    UNIQUE uk_friend (userId1, userId2),
    CONSTRAINT fk_friend_user1 FOREIGN KEY (userId1) REFERENCES user(id),
    CONSTRAINT fk_friend_user2 FOREIGN KEY (userId2) REFERENCES user(id)
) COMMENT '好友关系表' COLLATE = utf8mb4_unicode_ci;


-- 好友申请表
CREATE TABLE IF NOT EXISTS friend_request
(
    id         BIGINT AUTO_INCREMENT COMMENT '主键' PRIMARY KEY,
    senderId   BIGINT                             NOT NULL COMMENT '发送者 ID，关联到user表',
    receiverId BIGINT                             NOT NULL COMMENT '接收者 ID，关联到user表',
    status     VARCHAR(20)                        NOT NULL COMMENT '申请状态：pending/accepted/rejected',
    message    VARCHAR(512)                       NULL COMMENT '申请消息',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_senderId (senderId),
    INDEX idx_receiverId (receiverId),
    UNIQUE uk_request (senderId, receiverId),
    CONSTRAINT fk_request_sender FOREIGN KEY (senderId) REFERENCES user(id),
    CONSTRAINT fk_request_receiver FOREIGN KEY (receiverId) REFERENCES user(id)
) COMMENT '好友申请表' COLLATE = utf8mb4_unicode_ci;-- 支付订单表
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

SET FOREIGN_KEY_CHECKS=1;
