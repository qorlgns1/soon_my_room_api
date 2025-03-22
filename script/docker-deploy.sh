#!/bin/bash

# macOS 및 Linux 호환을 위한 경로 확인
get_script_path() {
  local script_path="$0"

  # macOS에서는 readlink -f가 작동하지 않으므로 대체 로직 사용
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS 환경
    while [ -L "$script_path" ]; do
      script_path=$(readlink "$script_path")
    done
  else
    # Linux 환경
    script_path=$(readlink -f "$0")
  fi

  echo "$(dirname "$script_path")"
}

# 스크립트의 절대 경로 확인
SCRIPT_DIR=$(get_script_path)
# 프로젝트 루트 디렉토리
PROJECT_DIR=$(dirname "$SCRIPT_DIR")

# 프로젝트 루트 디렉토리로 이동
cd "$PROJECT_DIR"

# 환경 변수
ENV=${1:-dev}  # 기본값은 dev

# Docker Compose 파일 존재 확인
if [ ! -f "docker-compose.${ENV}.yml" ]; then
  echo "오류: docker-compose.${ENV}.yml 파일을 찾을 수 없습니다."
  echo "현재 디렉토리: $(pwd)"
  echo "디렉토리 내용:"
  ls -la
  exit 1
fi

# .env 파일 확인
if [ ! -f ".env" ]; then
  echo "경고: .env 파일이 없습니다. .env.example 파일을 복사하여 .env 파일을 생성해 주세요."

  if [ -f ".env.example" ]; then
    echo "경고: .env.example 파일이 있습니다. 이 파일을 복사하여 사용하시겠습니까? (y/n)"
    read -r response
    if [[ "$response" =~ ^([yY][eE][sS]|[yY])$ ]]; then
      cp .env.example .env
      echo ".env.example 파일을 .env로 복사했습니다. 환경 설정을 수정해주세요."
    else
      echo ".env 파일이 필요합니다. 나중에 생성해주세요."
      exit 1
    fi
  else
    echo ".env 파일이 필요합니다. 환경 변수 설정 파일을 생성해주세요."
    exit 1
  fi
fi

# 로그 디렉토리 생성
mkdir -p logs

echo "====== 배포 환경: ${ENV} ======"
echo "====== 프로젝트 경로: $(pwd) ======"

# 배포 시작
echo "Docker Compose 실행 중..."
docker-compose -f docker-compose.${ENV}.yml up -d

# 배포 결과 확인
if [ $? -eq 0 ]; then
  echo "배포 성공!"
  echo "컨테이너 상태:"
  docker-compose -f docker-compose.${ENV}.yml ps
else
  echo "배포 실패!"
  exit 1
fi

echo "로그 확인:"
echo "docker-compose -f docker-compose.${ENV}.yml logs -f app"