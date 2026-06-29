# 构建阶段
FROM node:18-alpine AS build

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY package.json package-lock.json ./

# 安装依赖
RUN npm ci

# 复制所有文件
COPY . .

# 构建项目
RUN npm run build

# 生产阶段
FROM nginx:alpine AS production

# 从构建阶段复制构建好的文件到nginx目录
COPY --from=build /app/dist /usr/share/nginx/html

# 复制nginx配置文件(如果需要)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露80端口
EXPOSE 80

# 以非 root 用户运行 nginx，提升容器安全性
USER nginx

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://localhost/health || exit 1

# 启动nginx
CMD ["nginx", "-g", "daemon off;"]