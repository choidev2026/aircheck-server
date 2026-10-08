# Build stage
FROM gradle:8.12-jdk17 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY core ./core
COPY feature ./feature
COPY app ./app
RUN gradle :app:bootJar -x test --no-daemon

# Run stage
FROM eclipse-temurin:17-jre
WORKDIR /app

# 보안: non-root 유저로 실행
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /app/app/build/libs/app-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
