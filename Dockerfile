# Stage 1: Build file .jar bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy các file cấu hình và source code vào container
COPY pom.xml .
COPY src ./src

# Tiến hành Build dự án thành file .jar
RUN mvn clean package -DskipTests

# Stage 2: Môi trường chạy siêu nhẹ
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy tệp .jar từ giai đoạn build ở trên sang
COPY --from=build /app/target/telegram-bot-0.0.1-SNAPSHOT.jar app.jar

# Cấu hình port (dành cho các nền tảng serverless)
EXPOSE 8080

# Chạy Bot
ENTRYPOINT ["java", "-jar", "app.jar"]
