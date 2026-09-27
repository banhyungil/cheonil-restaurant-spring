#!/usr/bin/env bash
# ============================================================
#  Cheonil Restaurant - Deploy Script (매장 PC, macOS / Linux)
#
#  실행 순서:
#    1) Backend (cheonil-restaurant-spring) git pull  — compose / scripts 최신화
#    2) docker compose pull app web                   — Docker Hub 이미지 수신
#    3) docker compose up -d --no-build               — 변경된 컨테이너만 재기동
#
#  이미지는 개발 PC 에서 scripts/build-push.sh 로 미리 push.
#  롤백: .env 에 APP_TAG / WEB_TAG=<git-sha> 지정 후 재실행 (기본 latest).
#
#  사용:
#    ./scripts/deploy.sh
#
#  사전 조건:
#    - Docker 실행 중 + docker login 완료 (private 이미지)
#    - .env 작성 (DB_PASSWORD, CLOUDFLARED_TOKEN, GOOGLE_API_KEY)
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/.."

fail() {
  echo "[ERROR] $1" >&2
  exit 1
}

cd "$BACKEND_DIR" 2>/dev/null || fail "Backend 디렉터리를 찾을 수 없습니다."

echo
echo "=== [1/3] Backend git pull ==="
echo "path: $BACKEND_DIR"
git pull || fail "Backend git pull 실패"

echo
echo "=== [2/3] docker compose pull ==="
# app / web 만 — db / whisper / cloudflared 는 의도치 않은 버전 업 방지.
docker compose pull app web || fail "docker compose pull 실패 (docker login 확인)"

echo
echo "=== [3/3] docker compose up -d ==="
docker compose up -d --no-build || fail "docker compose up 실패"

echo
echo "=== 배포 완료 ==="
