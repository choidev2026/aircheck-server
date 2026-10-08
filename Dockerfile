# Build stage
FROM gradle:8.12-jdk17 AS build
WORKDIR /app
COPY . .
# --no-daemon: Gradle 데몬은 다음 빌드에서 재사용하려고 띄워두는 건데,
# Docker 빌드는 이 컨테이너가 끝나면 그 안 상태가 통째로 버려지는 일회성이라 의미가 없음
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
