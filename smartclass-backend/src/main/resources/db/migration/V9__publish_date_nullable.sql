-- V9: publishDate 允许为空
-- 原因：daily_word / daily_article 的 publishDate 建表为 NOT NULL 且无默认值，
-- 任何不带发布日期的创建（管理端手动创建、CSV 批量导入）都会插库失败。
-- 语义：publishDate 为空 = 未排期（不出现在"今日"推送），排期后按日期可见。
ALTER TABLE daily_word
    MODIFY COLUMN publishDate date NULL COMMENT '发布日期（空=未排期）';

ALTER TABLE daily_article
    MODIFY COLUMN publishDate date NULL COMMENT '发布日期（空=未排期）';
