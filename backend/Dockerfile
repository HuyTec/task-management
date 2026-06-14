# ==========================================
# Dockerfile cho Spring Boot Backend
# Multi-stage build để tối ưu kích thước image
# ==========================================

# === Stage 1: BUILD ===
# Dùng Maven để compile và đóng gói thành file JAR
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml trước để tận dụng Docker layer cache
# (nếu pom.xml không thay đổi, Maven không cần download lại dependencies)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy toàn bộ source code và build
COPY src ./src
RUN mvn clean package -DskipTests

# === Stage 2: RUN ===
# Dùng JRE nhẹ hơn JDK để chạy ứng dụng
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file JAR từ stage build
COPY --from=build /app/target/*.jar app.jar

# Expose port mà Spring Boot lắng nghe
EXPOSE 8080

# Lệnh khởi chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
