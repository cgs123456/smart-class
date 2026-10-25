-- 2026-10-25 P0 索引加固迁移（对应评审报告 P0-3）
-- 适用于已按旧版 create_table.sql 初始化的存量库；新库直接用新版 create_table.sql 即可。

-- 1) user 表：账号与微信 unionId 唯一化
--    先清理存量重复（保留最早注册的行，其余标记删除），再加唯一索引。
DELETE u1 FROM user u1
JOIN user u2
  ON u1.userAccount = u2.userAccount
 AND u1.id > u2.id;
ALTER TABLE user ADD UNIQUE INDEX uk_user_account (userAccount);

-- unionId 允许多个 NULL（MySQL 唯一索引特性，未绑定微信的旧用户不受影响）
DELETE u1 FROM user u1
JOIN user u2
  ON u1.unionId IS NOT NULL
 AND u2.unionId IS NOT NULL
 AND u1.unionId = u2.unionId
 AND u1.id > u2.id;
ALTER TABLE user ADD UNIQUE INDEX uk_unionId (unionId);
DROP INDEX idx_unionId ON user;

-- 2) post_thumb / post_favour：一人一赞一藏，复合唯一键防重复写入
--    清理存量重复行（保留 id 最小的），再加复合唯一。
DELETE t1 FROM post_thumb t1
JOIN post_thumb t2
  ON t1.postId = t2.postId AND t1.userId = t2.userId AND t1.id > t2.id;
ALTER TABLE post_thumb ADD UNIQUE INDEX uk_post_user (postId, userId);
ALTER TABLE post_thumb DROP INDEX idx_postId; -- 复合唯一键的最左前缀已覆盖

DELETE f1 FROM post_favour f1
JOIN post_favour f2
  ON f1.postId = f2.postId AND f1.userId = f2.userId AND f1.id > f2.id;
ALTER TABLE post_favour ADD UNIQUE INDEX uk_post_user (postId, userId);
ALTER TABLE post_favour DROP INDEX idx_postId;

-- 3) 业务侧配套：点赞/收藏入库应改为 INSERT IGNORE 或捕获 DuplicateKeyException 幂等返回，
--    否则唯一键生效后重复点赞会抛 500（见评审报告 P0-3 修复方向）。
