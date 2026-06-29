# 构建阶段
FROM node:18.17-alpine as builder

# 设置工作目录
WORKDIR /app

# 复制项目依赖文件
COPY package.json ./
COPY *.lock* ./
COPY pnpm-lock.yaml* ./

# 安装依赖
RUN npm install

# 复制所有文件

COPY . .

# 构建应用
RUN npm run build

FROM nginx:1.25-alpine

# 复制构建产物到Nginx服务目录
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 清理缓存减小镜像体积
RUN rm -rf /var/cache/apk/* && \
    rm -rf /tmp/*

# 创建非 root 用户并切换（生产安全）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /usr/share/nginx/html && \
    chown -R appuser:appgroup /etc/nginx/conf.d && \
    mkdir -p /etc/nginx/ssl && \
    chown -R appuser:appgroup /etc/nginx/ssl && \
    chown -R appuser:appgroup /var/cache/nginx && \
    chown -R appuser:appgroup /var/run

USER appuser

# 暴露端口（同时支持 HTTP 跳转与 HTTPS 主服务）
EXPOSE 80
EXPOSE 443

# 健康检查（探测 HTTPS 主服务）
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD wget --no-check-certificate --spider -q https://localhost/health || exit 1

# 启动Nginx
CMD ["nginx", "-g", "daemon off;"]
