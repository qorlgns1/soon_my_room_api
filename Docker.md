Docker 배포 가이드: Soon My Room API

1. Docker 설정 개요
   이 프로젝트는 Docker를 통해 쉽게 배포할 수 있습니다. Docker 컨테이너를 통해 개발 환경과 운영 환경에서 일관된 방식으로 애플리케이션을 실행할 수 있습니다.
2. 주요 Docker 파일
   프로젝트 루트 디렉토리에 다음 파일들이 포함되어 있습니다:

Dockerfile: 애플리케이션 빌드 및 실행 환경을 정의합니다.
docker-compose.dev.yml: 개발 환경 배포 설정
docker-compose.prod.yml: 운영 환경 배포 설정
.dockerignore: Docker 빌드 시 제외할 파일 목록
script/docker-build.sh: Docker 이미지 빌드 스크립트
script/docker-deploy.sh: Docker 컨테이너 배포 스크립트

3. 환경 변수 설정
   애플리케이션 실행에 필요한 환경 변수를 .env 파일에 설정해야 합니다:
   bash복사# .env.example 파일을 복사하여 .env 파일 생성
   cp .env.example .env
   .env 파일에 다음 환경 변수를 설정합니다:
   properties복사JWT_SECRET=your_jwt_secret_here
   JWT_EXPIRATION=86400000
   DB_URL=jdbc:postgresql://your_db_host:5432/your_db_name
   DB_USERNAME=your_db_username
   DB_PASSWORD=your_db_password
   SUPABASE_ENDPOINT=your_supabase_endpoint
   SUPABASE_REGION=your_supabase_region
   SUPABASE_ACCESS_KEY=your_supabase_access_key
   SUPABASE_SECRET_KEY=your_supabase_secret_key
   SUPABASE_BUCKET_PROFILES=user-profiles
   SUPABASE_BUCKET_POSTS=post-images
   SUPABASE_BUCKET_PRODUCTS=product-images
   SUPABASE_BUCKET_DEFAULT=default
   CORS_ALLOWED_ORIGINS=http://localhost:3000,https://soon-my-room.com
4. Docker 이미지 빌드
   애플리케이션 Docker 이미지를 빌드하려면 다음 명령어를 실행합니다:
   bash복사# 개발 환경 이미지 빌드
   chmod +x script/docker-build.sh
   ./script/docker-build.sh dev

# 운영 환경 이미지 빌드

./script/docker-build.sh prod
빌드 스크립트는 환경에 따라 soon-my-room-api:dev-latest 또는 soon-my-room-api:prod-latest 태그를 가진 이미지를 생성합니다.
빌드된 이미지는 다음 명령어로 확인할 수 있습니다:
bash복사docker images

5. Docker 컨테이너 배포
   애플리케이션을 Docker 컨테이너로 배포하려면 다음 명령어를 실행합니다:
   bash복사# 개발 환경 배포
   chmod +x script/docker-deploy.sh
   ./script/docker-deploy.sh dev

# 운영 환경 배포

./script/docker-deploy.sh prod
배포 스크립트는 환경에 맞는 Docker Compose 파일을 사용하여 애플리케이션을 배포합니다.

6. Docker Compose 설정 파일
   개발 환경 설정 (docker-compose.dev.yml)
   yaml복사version: '3.8'

services:
app:
image: soon-my-room-api:dev-latest
container_name: soon-my-room-api-dev
ports:

- "9000:9000"
  environment:
- SPRING_PROFILES_ACTIVE=dev
- JWT_SECRET=${JWT_SECRET}
- JWT_EXPIRATION=${JWT_EXPIRATION}
- DB_URL=${DB_URL}
- DB_USERNAME=${DB_USERNAME}
- DB_PASSWORD=${DB_PASSWORD}
- SUPABASE_ENDPOINT=${SUPABASE_ENDPOINT}
- SUPABASE_REGION=${SUPABASE_REGION}
- SUPABASE_ACCESS_KEY=${SUPABASE_ACCESS_KEY}
- SUPABASE_SECRET_KEY=${SUPABASE_SECRET_KEY}
- SUPABASE_BUCKET_PROFILES=${SUPABASE_BUCKET_PROFILES:-user-profiles}
- SUPABASE_BUCKET_POSTS=${SUPABASE_BUCKET_POSTS:-post-images}
- SUPABASE_BUCKET_PRODUCTS=${SUPABASE_BUCKET_PRODUCTS:-product-images}
- SUPABASE_BUCKET_DEFAULT=${SUPABASE_BUCKET_DEFAULT:-default}
- CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-http://localhost:3000}
  volumes:
- ./logs:/app/logs
  restart: unless-stopped
  networks:
- soon-my-room-network

networks:
soon-my-room-network:
driver: bridge
운영 환경 설정 (docker-compose.prod.yml)
yaml복사version: '3.8'

services:
app:
image: soon-my-room-api:prod-latest
container_name: soon-my-room-api-prod
ports:

- "9000:9000"
  environment:
- SPRING_PROFILES_ACTIVE=prod
- JWT_SECRET=${JWT_SECRET}
- JWT_EXPIRATION=${JWT_EXPIRATION}
- DB_URL=${DB_URL}
- DB_USERNAME=${DB_USERNAME}
- DB_PASSWORD=${DB_PASSWORD}
- SUPABASE_ENDPOINT=${SUPABASE_ENDPOINT}
- SUPABASE_REGION=${SUPABASE_REGION}
- SUPABASE_ACCESS_KEY=${SUPABASE_ACCESS_KEY}
- SUPABASE_SECRET_KEY=${SUPABASE_SECRET_KEY}
- SUPABASE_BUCKET_PROFILES=${SUPABASE_BUCKET_PROFILES:-user-profiles}
- SUPABASE_BUCKET_POSTS=${SUPABASE_BUCKET_POSTS:-post-images}
- SUPABASE_BUCKET_PRODUCTS=${SUPABASE_BUCKET_PRODUCTS:-product-images}
- SUPABASE_BUCKET_DEFAULT=${SUPABASE_BUCKET_DEFAULT:-default}
- CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-https://soon-my-room.com,https://www.soon-my-room.com}
  volumes:
- ./logs:/app/logs
  restart: always
  networks:
- soon-my-room-network
  deploy:
  resources:
  limits:
  cpus: '1'
  memory: 1G
  reservations:
  cpus: '0.5'
  memory: 512M

networks:
soon-my-room-network:
driver: bridge

7. Docker 컨테이너 관리
   컨테이너 로그 확인
   bash복사# 개발 환경 로그
   docker-compose -f docker-compose.dev.yml logs -f app

# 운영 환경 로그

docker-compose -f docker-compose.prod.yml logs -f app
컨테이너 중지
bash복사# 개발 환경 중지
docker-compose -f docker-compose.dev.yml down

# 운영 환경 중지

docker-compose -f docker-compose.prod.yml down
컨테이너 재시작
bash복사# 개발 환경 재시작
docker-compose -f docker-compose.dev.yml restart app

# 운영 환경 재시작

docker-compose -f docker-compose.prod.yml restart app

8. 애플리케이션 접속
   배포가 완료되면 다음 URL로 애플리케이션에 접속할 수 있습니다:

API 접속: http://localhost:9000
Swagger UI: http://localhost:9000/swagger-ui/index.html

9. 문제 해결
   빌드 실패
   빌드 과정에서 코드 스타일 검사(Spotless) 실패가 발생할 수 있습니다. 이 경우 다음 명령어로 코드 스타일을 수정할 수 있습니다:
   bash복사./gradlew spotlessApply
   컨테이너 시작 실패
   컨테이너가 시작되지 않는 경우 로그를 확인하세요:
   bash복사docker logs soon-my-room-api-dev
   환경 변수 문제
   환경 변수가 제대로 설정되었는지 확인하세요:
   bash복사docker-compose -f docker-compose.dev.yml config
   데이터베이스 연결 오류
   데이터베이스 연결 정보가 올바른지 확인하고, Supabase 프로젝트 설정을 확인하세요.
10. 주의사항

운영 환경에서는 보안을 위해 항상 강력한 비밀번호와 보안 키를 사용하세요.
.env 파일은 민감한 정보를 포함하므로 버전 관리 시스템에 포함하지 마세요.
배포 전에 항상 로컬 환경에서 테스트를 수행하세요.

이 가이드를 통해 Soon My Room API를 Docker를 사용하여 쉽게 배포하고 관리할 수 있습니다. 추가 질문이나 문제가 있으면 프로젝트 관리자에게 문의하세요.