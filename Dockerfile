# Stage 1: Build file .jar bằng Maven
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy các file cấu hình và source code vào container
COPY pom.xml .
COPY src ./src

# Tiến hành Build dự án thành file .jar (bỏ qua bước chạy test để nhanh hơn)
RUN mvn clean package -DskipTests

# Stage 2: Môi trường chạy siêu nhẹ chỉ chứa JDK 17
FROM openjdk:17-jdk-slim
WORKDIR /app

# Copy tệp .jar từ giai đoạn build ở trên sang
COPY --from=build /app/target/telegram-bot-0.0.1-SNAPSHOT.jar app.jar

# Spring Boot mặc định chạy ở port 8080 (Koyeb cần biết port này để gán đường truyền mạng)
EXPOSE 8080

# Chạy Bot ngay khi container bật lên
ENTRYPOINT ["java", "-jar", "app.jar"]
