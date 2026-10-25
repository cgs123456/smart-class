# 归档目录（只读）

本目录存放已被 Flyway 取代的历史手工 SQL 与 ES 索引映射，仅作溯源参考，不再维护。

## 归档原因

- 单体后端的数据库结构唯一正源是 `smartclass-backend/src/main/resources/db/migration/`（Flyway V1–V9），应用启动时自动执行，手工 DDL 链与其并存且已漂移（评审报告附录 I 记录过 V6 漏表即因此）。
- `sql/`（原 `smartclass-backend/sql/`，7 个文件）：Flyway 引入前的手工建表/加字段脚本，含 P0-3 唯一索引去重迁移（评审报告 A 节引用的 `migrate_20261025_unique_indexes.sql`）。
- `microservice-sql/`（原 `smartclass-backend-microservice/sql/`，2 个文件）：微服务模式早期手工脚本；微服务与单体共用同一数据库，schema 由单体 Flyway 统一管理，无需独立执行。

## 使用约束

- 禁止在任何环境执行本目录脚本（重复执行会与 Flyway 记录冲突）。
- 评审报告等文档中提到的 `sql/...` 路径，均指本归档目录下的对应文件。
- 新增表结构变更一律新增 Flyway 脚本 `V<n>__描述.sql`，禁止修改已合入的历史迁移。
