# 智云星课 - 智能学习平台

## 项目简介

智云星课是一个基于 Spring Boot 3.3 和 AI 技术的智能学习平台，集成了 Dify AI 接口，支持 AI 聊天、课程学习、每日文章、单词记忆、社交圈子等功能，为用户提供全方位的智能化学习体验。系统采用前后端分离架构，包含移动端、管理后台和后端服务三个子项目。

## 系统架构

```
┌─────────────────────────────────────────────────┐
│                   Nginx 反向代理                   │
├────────────────┬────────────────┬───────────────┤
│   移动端前端     │   管理后台前端    │               │
│  Vue3 + Vant   │ React + AntD   │               │
├────────────────┴────────────────┴───────────────┤
│              Spring Boot 后端服务                  │
│  MyBatis Plus + Redis + Elasticsearch            │
├─────────────────────────────────────────────────┤
│     MySQL      │     Redis     │  Elasticsearch  │
└─────────────────────────────────────────────────┘
```

## 子项目结构

| 子项目                               | 说明     | 技术栈                                                     |
| --------------------------------- | ------ | ------------------------------------------------------- |
| `smartclass-backend`              | 后端单体服务 | Spring Boot 3.3.6 + Java 17 + MyBatis Plus 3.5.9        |
| `smartclass-backend-microservice` | 后端微服务版 | Spring Cloud 2023.0.4 + Spring Cloud Alibaba 2023.0.3.2 |
| `smartclass-frontend`             | 移动端前端  | Vue 3.5 + TypeScript + Vant 4.9 + Pinia + Rsbuild       |
| `smartclass-manage-frontend`      | 管理后台前端 | React 18 + Ant Design 5 + UmiJS 4.x + ECharts           |

## 核心功能

### AI 智能对话

集成 Dify AI，支持流式/阻塞式响应、多智能体切换、聊天历史管理、会话总结、多模态输入（文本/图片/文件）。

### 课程学习

课程分类/章节/小节管理、学习进度跟踪、收藏与评价、课程表、课程资料管理。

### 每日学习

每日文章推送、单词学习与记忆、生词本、学习打卡、个性化学习推荐。

### 社交圈子

帖子发布、评论/回复、点赞/收藏、推荐/关注/热榜/问答分类。

### 用户体系

注册登录（含手机号）、用户等级与成长体系、微信公众号集成。

### 好友与私信

好友请求、私聊、SSE 实时消息推送、在线状态通知。

### 管理后台

- **数据看板** - 用户活跃度趋势、课程完成率、班级活跃度等多维度数据可视化（折线图/饼图/柱状图/雷达图/热力图）
- **用户管理** - 学生/教师账户管理、权限控制、批量操作
- **课程管理** - 课程内容/章节/材料/评价管理
- **单词库管理** - 词汇录入/分类/例句/学习进度跟踪
- **每日美文管理** - 文章发布/分类/阅读统计
- **AI 分身助教管理** - 分身创建/知识库配置/使用统计
- **帖子管理** - 内容审核/分类/互动统计
- **用户反馈管理** - 反馈收集/分类处理/满意度跟踪

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 5.7+
- Redis 5.0+
- Node.js 18.0+
- npm 9.0+

### 1. 后端启动

创建数据库并导入初始化脚本：

```sql
CREATE DATABASE smart_class DEFAULT CHARACTER SET utf8mb4;
```

```bash
mysql -u root -p smart_class < smartclass-backend/sql/create_table.sql
```

配置环境变量（敏感信息通过环境变量注入，也可直接在 `application.yml` 中修改默认值）：

```bash
export MYSQL_PASSWORD=your_password
export REDIS_PASSWORD=your_redis_password
export COS_ACCESS_KEY=your_cos_key
export COS_SECRET_KEY=your_cos_secret
export WX_MP_TOKEN=your_wx_token
export WX_MP_AES_KEY=your_wx_aes_key
export WX_MP_APP_ID=your_wx_appid
export WX_MP_SECRET=your_wx_secret
```

启动后端：

```bash
cd smartclass-backend
mvn spring-boot:run
```

<br />

### 2. 移动端前端启动

```bash
cd smartclass-frontend
npm install
npm run dev
```

访问：`http://localhost:8080`

### 3. 管理后台启动

```bash
cd smartclass-manage-frontend
npm install
npm run start:dev
```

访问：`http://localhost:8000`

### 4. Docker 一键部署

```bash
cd smartclass-backend
docker-compose up -d
```

## 技术栈总览

### 后端

- Spring Boot 3.3.6 / Spring Cloud 2023.0.4（微服务版）
- Java 17
- MyBatis Plus 3.5.9
- Spring Data Elasticsearch（CriteriaQuery API）
- Spring Session + Redis（分布式会话）
- Dify AI API 集成（OkHttp 4.12 + SSE）
- Knife4j OpenAPI 3（Jakarta）
- WxJava 4.6 微信公众号
- 腾讯云 COS 对象存储
- Netty 4.1 WebSocket 实时通信
- Spring Boot Actuator 监控

### 移动端前端

- Vue 3.5 + TypeScript
- Vant 4.9 UI 组件库
- Pinia 3.0 状态管理
- Vue Router 4.5 路由
- Axios HTTP 请求
- Rsbuild 1.7 构建
- Capacitor 6 Android 原生打包
- ESLint + Prettier + Biome 代码规范

### 管理后台前端

- React 18 + TypeScript
- Ant Design 5 + Ant Design Pro
- UmiJS 4.x（Max CLI）
- ECharts 5.x 数据可视化
- Less + Emotion 样式方案
- OpenAPI 自动生成接口代码
- 国际化（8 种语言）

## 项目结构

### 后端（smartclass-backend）

```
src/main/java/com/cgs/smartclass/
├── controller/     # 控制器
├── service/        # 业务服务
├── mapper/         # MyBatis 映射
├── model/          # 数据模型（entity/vo/dto/enums）
├── config/         # 配置类（含 TraceIdFilter）
├── common/         # 通用组件
├── aop/            # 切面逻辑
├── esdao/          # ES 数据访问
├── exception/      # 全局异常处理
├── netty/          # WebSocket 服务
├── wxmp/           # 微信公众号
└── utils/          # 工具类
```

### 移动端前端（smartclass-frontend）

```
src/
├── api/            # API 请求封装
├── components/     # 公共组件（Chat/Circle/Course/Dialogue/Home/Profile）
├── router/         # 路由配置（含路由守卫）
├── services/       # 服务层和 API 模型（OpenAPI 自动生成）
├── stores/         # Pinia 状态管理
├── views/          # 页面组件（home/chat/course/circle/profile）
└── utils/          # 工具函数
```

### 管理后台（smartclass-manage-frontend）

```
src/
├── pages/          # 页面（Admin/DataPanel/Welcome）
├── components/     # 公共组件
├── services/       # API 服务（OpenAPI 自动生成）
├── locales/        # 国际化（8 种语言）
└── constants/      # 常量定义
```

## 安全特性

- 敏感配置环境变量化（数据库密码、COS 密钥、微信密钥等）
- Cookie HttpOnly + Secure + SameSite
- WebSocket 连接数限制 + 认证超时
- 分页参数上限保护（MAX\_PAGE\_SIZE=100）
- 请求链路追踪（TraceId + MDC）
- 全局异常处理（参数校验/数据库/超时/解析错误）
- OkHttp 连接池 + 自动重试

## Dify AI 集成

系统通过 Dify API 实现智能对话功能。添加 AI 智能体的步骤：

1. 在 Dify 平台创建对话型应用并配置提示词
2. 发布智能体并获取访问 URL
3. 创建 API 密钥
4. 在管理后台「AI 虚拟形象管理」中添加智能体，配置 Base URL、API Key、App ID
5. 在移动端测试对话功能

