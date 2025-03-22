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
TAG=${2:-latest}
IMAGE_NAME="soon-my-room-api"

# 이미지 태그 생성
FULL_TAG="${IMAGE_NAME}:${ENV}-${TAG}"

echo "====== 빌드 환경: ${ENV} ======"
echo "====== 이미지 태그: ${FULL_TAG} ======"
echo "====== 프로젝트 경로: $(pwd) ======"

# Dockerfile 존재 확인
if [ ! -f "Dockerfile" ]; then
  echo "오류: Dockerfile을 찾을 수 없습니다."
  echo "현재 디렉토리: $(pwd)"
  echo "디렉토리 내용:"
  ls -la
  exit 1
fi

# Docker 빌드
echo "Docker 이미지 빌드 중..."
docker build -t ${FULL_TAG} .

# 빌드 결과 확인
if [ $? -eq 0 ]; then
  echo "빌드 성공! 이미지: ${FULL_TAG}"
  echo "다음 명령으로 실행할 수 있습니다:"
  echo "docker-compose -f docker-compose.${ENV}.yml up -d"
else
  echo "빌드 실패!"
  exit 1
fi