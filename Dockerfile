FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# 그래들 파일 복사 (레이어 캐싱 활용을 위해)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY .gitattributes .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드 (소스 변경 시에도 캐시 활용)
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build -x test -x spotlessCheck --no-daemon

# 실행 이미지 생성
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드 이미지에서 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 9000

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]
