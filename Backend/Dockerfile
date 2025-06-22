FROM openjdk:17-jdk-slim

WORKDIR /app

# 의존성 먼저 복사 (캐싱 활용)
COPY gradle gradle
COPY gradlew .
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew bootJar --no-daemon

# 실행
EXPOSE 8080
CMD ["java", "-jar", "-Dspring.profiles.active=prod", "build/libs/Studia-0.0.1-SNAPSHOT.jar"]
