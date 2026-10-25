# =====================================================================
# Stage 1: Build & Extract Spring Boot Layered JAR
# =====================================================================
FROM eclipse-temurin:27-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven Wrapper và pom.xml trước để tối ưu Docker layer cache
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code và build ứng dụng
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B && \
    java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

# =====================================================================
# Stage 2: Runtime Image (Minimal JRE 27 Alpine + Non-root User)
# =====================================================================
FROM eclipse-temurin:27-jre-alpine AS runtime
WORKDIR /app

# Cài đặt tzdata và curl phục vụ healthcheck
RUN apk add --no-cache tzdata curl && \
    addgroup -S spring && adduser -S spring -G spring

ENV TZ=Asia/Ho_Chi_Minh \
    JAVA_OPTS="-XX:+UseZGC -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Copy từng layer theo thứ tự ít thay đổi -> hay thay đổi
COPY --from=builder --chown=spring:spring /workspace/target/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/application/ ./

USER spring:spring
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -fsSL http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
