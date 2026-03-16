# Build stage
FROM gradle:8.12-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY domain ./domain
COPY application ./application
COPY adapter ./adapter
COPY app ./app
RUN gradle :app:bootJar -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안: non-root 유저로 실행
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /app/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
