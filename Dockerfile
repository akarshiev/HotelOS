# ==========================================
# HotelOS - Multi-stage Dockerfile
# ==========================================

# Stage 1: Build with Gradle
FROM gradle:8.12-jdk17 AS build
WORKDIR /app

# Copy build files first for better layer caching
COPY build.gradle settings.gradle ./
COPY shared/build.gradle shared/build.gradle
COPY reception-service/build.gradle reception-service/build.gradle
COPY housekeeping-service/build.gradle housekeeping-service/build.gradle
COPY room-service/build.gradle room-service/build.gradle
COPY maintenance-service/build.gradle maintenance-service/build.gradle

# Copy Gradle wrapper
COPY gradle/ gradle/
COPY gradlew .
RUN chmod +x gradlew

# Download dependencies (cached unless build.gradle changes)
RUN ./gradlew --no-daemon dependencies || true

# Copy source code
COPY shared/ shared/
COPY reception-service/ reception-service/
COPY housekeeping-service/ housekeeping-service/
COPY room-service/ room-service/
COPY maintenance-service/ maintenance-service/

ARG SERVICE_NAME
RUN ./gradlew :${SERVICE_NAME}:bootJar --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/UTC /etc/localtime && \
    echo "UTC" > /etc/timezone

WORKDIR /app

ARG SERVICE_NAME
COPY --from=build /app/${SERVICE_NAME}/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
