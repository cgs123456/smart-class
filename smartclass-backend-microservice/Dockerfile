# P1-5: 通用 Dockerfile 模板（多阶段构建）
# 使用方式：在构建时通过 --buildarg SERVICE_DIR=<模块目录名> 指定要构建的服务
# 示例：
#   docker build -t smartclass-backend-user \
#     --buildarg SERVICE_DIR=smartclass-backend-user \
#     -f Dockerfile .
#
# 注意：本模板为保守实现，未为每个服务单独创建 Dockerfile。
# 各服务主类与 pom.xml 均遵循标准 Maven 结构，可直接复用本模板。

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-17 AS builder

# 服务目录名（构建时传入），例如 smartclass-backend-user
ARG SERVICE_DIR

WORKDIR /workspace

# 先拷贝父 pom 与公共模块，利用 Docker 层缓存加速依赖解析
COPY pom.xml ./
COPY smartclass-backend-common ./smartclass-backend-common
COPY smartclass-backend-model ./smartclass-backend-model
COPY smartclass-backend-service-client ./smartclass-backend-service-client

# 拷贝目标服务源码
COPY ${SERVICE_DIR} ./ ${SERVICE_DIR}

# 仅构建目标服务（跳过测试以加速构建）
RUN mvn -B clean package -pl ${SERVICE_DIR} -am -DskipTests

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:17-jre

ARG SERVICE_DIR

# 服务名（用于 JVM 启动参数与日志标识）
ENV SERVICE_NAME=${SERVICE_DIR}
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

WORKDIR /app

# 拷贝构建产物
COPY --from=builder /workspace/${SERVICE_DIR}/target/*.jar /app/app.jar

# 暴露 Actuator 健康检查端口（默认 8080，由 application.yml 中的 server.port 覆盖）
EXPOSE 8080

# 健康检查（依赖 spring-boot-starter-actuator）
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
