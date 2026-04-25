# 使用一个带有 Maven 和 JDK 8 的基础镜像来构建应用
FROM docker.m.daocloud.io/library/maven:3.8.4-openjdk-8 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 并下载依赖，利用 Docker 缓存机制
COPY pom.xml .
RUN mvn dependency:go-offline

# 复制所有源代码
COPY src ./src

# 打包应用
RUN mvn package -DskipTests

# --- 第二阶段：创建轻量级的最终运行镜像 ---
FROM docker.m.daocloud.io/library/openjdk:8-jre-slim

WORKDIR /app

# 从构建阶段复制打包好的 JAR 文件
COPY --from=builder /app/target/*.jar app.jar

# 暴露服务端口 (每个服务不同，但可以在 docker-compose 中覆盖)
# EXPOSE 8081 

# 启动应用的命令
ENTRYPOINT ["java","-jar","app.jar"]