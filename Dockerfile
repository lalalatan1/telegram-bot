# Stage 1: Build file .jar bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy các file cấu hình và source code vào container
COPY pom.xml .
COPY src ./src

# Tiến hành Build dự án thành file .jar
RUN mvn clean package -DskipTests

# Stage 2: Môi trường chạy siêu nhẹ của Ubuntu (Jammy)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Khắc phục lỗi apt-get install bằng cách cài đặt trực tiếp Google Chrome bản Stable chuẩn
RUN apt-get update && apt-get install -y wget gnupg && \
    wget -q -O - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add - && \
    sh -c 'echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" >> /etc/apt/sources.list.d/google.list' && \
    apt-get update && apt-get install -y google-chrome-stable && \
    rm -rf /var/lib/apt/lists/*

# Copy tệp .jar từ giai đoạn build ở trên sang
COPY --from=build /app/target/telegram-bot-0.0.1-SNAPSHOT.jar app.jar

# Cấu hình port (dành cho các nền tảng serverless)
EXPOSE 8080

# Chạy Bot
ENTRYPOINT ["java", "-jar", "app.jar"]
