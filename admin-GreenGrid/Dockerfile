# ==================== 构建阶段 ====================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 先复制 pom.xml 单独拉取依赖，充分利用 Docker 层缓存
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 复制源码并打包（跳过测试）
COPY src ./src
RUN mvn -B clean package -DskipTests

# ==================== 运行阶段 ====================
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区
ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Duser.timezone=Asia/Shanghai"

# 复制可执行 jar（finalName=greengrid-admin-server）
COPY --from=build /build/target/greengrid-admin-server.jar app.jar

EXPOSE 8080

# 默认激活 prod profile，数据库/JWT 等一律由环境变量注入
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
