# 第一阶段：构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

# 工作目录
WORKDIR /app

# 首先复制 pom.xml 并下载依赖，利用 Docker 缓存机制
# 注意：原 settings.xml 不存在于仓库中已移除该 COPY，避免 Docker 构建失败
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn ./.mvn
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 构建应用
RUN mvn package -DskipTests -B

# 第二阶段：运行阶段
FROM eclipse-temurin:17-jre-alpine

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 添加标签信息
LABEL maintainer="SmartClass Team"
LABEL description="SmartClass Backend Application"
LABEL version="1.0.0"

# 创建非 root 用户运行应用
RUN addgroup -S smartclass && adduser -S smartclass -G smartclass

# 工作目录
WORKDIR /app

# 从构建阶段复制构建结果
COPY --from=builder /app/target/smartclass-cgs-0.0.1-SNAPSHOT.jar /app/app.jar

# 复制可能的配置文件（如果需要特定配置）
COPY src/main/resources/application-prod.yml /app/config/

# 暴露应用端口
EXPOSE 8101

# 定义数据卷，用于存储可能的上传文件等
VOLUME ["/app/data", "/app/logs"]

# 将应用所有权授予非 root 用户
RUN chown -R smartclass:smartclass /app
USER smartclass

# 设置应用启动时的默认环境变量
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SERVER_PORT=8101
ENV TZ=Asia/Shanghai

# 健康检查 - 通过 Actuator 接口检测应用状态
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD curl -f http://localhost:8101/api/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -Dserver.port=$SERVER_PORT -Dspring.datasource.url=$SPRING_DATASOURCE_URL -Dspring.datasource.username=$SPRING_DATASOURCE_USERNAME -Dspring.datasource.password=$MYSQL_PASSWORD -Dspring.redis.host=$SPRING_REDIS_HOST -Dspring.redis.port=$SPRING_REDIS_PORT -Dspring.redis.password=$REDIS_PASSWORD -jar /app/app.jar"]
