# 阶段 1：使用 Maven 进行代码编译打包
FROM docker.m.daocloud.io/library/maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
# 先拷贝 pom.xml 下载依赖，利用 Docker 缓存加速以后的构建速度
COPY pom.xml .
RUN mvn dependency:go-offline
# 拷贝源代码并进行无测试打包
COPY src ./src
RUN mvn clean package -DskipTests

# 阶段 2：提取编译好的 jar 包，运行在轻量级 JRE 环境中
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 设置时区为东八区，保证专注记录时间戳正确
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从 builder 阶段复制构建好的 jar 包
COPY --from=builder /build/target/*.jar app.jar

# 暴露后端的 8080 端口
EXPOSE 8080

# 容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]