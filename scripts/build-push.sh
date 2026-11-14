#!/usr/bin/env bash
# ============================================================
#  Cheonil Restaurant - Image Build & Push (개발 PC, macOS / Linux)
#
#  Backend / Frontend 이미지를 빌드해 Docker Hub 에 push.
#  매장 PC 는 scripts/deploy.(sh|bat) 로 pull + 재기동.
#
#  태그:
#    <image>:latest     매장 PC 기본 배포 대상
#    <image>:<git-sha>  롤백용 (매장 PC .env 에 APP_TAG / WEB_TAG=<sha> 지정)
#
#  사용:
#    ./scripts/build-push.sh          # app + web 모두
#    ./scripts/build-push.sh app      # backend 만
#    ./scripts/build-push.sh web      # frontend 만
#
#  환경변수 (선택):
#    PLATFORM   빌드 플랫폼 (기본: linux/amd64 — 매장 PC Windows x64)
#
#  사전 조건:
#    - docker login 완료 (banhyungil 계정)
#    - 두 저장소가 sibling 폴더 구조로 위치
#        <ROOT>/cheonil-restaurant-spring/
#        <ROOT>/cheonil-restaurant-next/
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$(cd "$SCRIPT_DIR/../../cheonil-restaurant-next" && pwd)"

PLATFORM="${PLATFORM:-linux/amd64}"
TARGET="${1:-all}"

BACKEND_IMAGE="banhyungil/cheonil-restaurant-spring"
FRONTEND_IMAGE="banhyungil/cheonil-restaurant-next"

fail() {
  echo "[ERROR] $1" >&2
  exit 1
}

# 커밋 SHA 태그 — 빌드 소스가 HEAD 커밋이므로 dirty 여부와 무관하게 sha 그대로.
# 미커밋 변경은 이미지에 포함되지 않으므로 알림만.
git_tag() {
  local dir="$1"
  if [[ -n "$(git -C "$dir" status --porcelain)" ]]; then
    echo "[WARN] $(basename "$dir") 에 커밋되지 않은 변경이 있습니다. (이미지에 미포함 — HEAD 기준 빌드)" >&2
  fi
  git -C "$dir" rev-parse --short HEAD
}

# build + push — Mac(arm64) 에서도 매장 PC 용 amd64 이미지를 만들기 위해 buildx 사용.
# 빌드 context 는 git archive(HEAD) tar — 작업 트리의 미커밋/untracked 파일 제외, 파일 권한도 git 기준(644/755).
build_push() {
  local name="$1" dir="$2" image="$3"
  local tag
  tag="$(git_tag "$dir")"

  echo
  echo "=== [$name] $image ($tag, $PLATFORM) ==="
  git -C "$dir" archive --format=tar HEAD | docker buildx build \
    --platform "$PLATFORM" \
    -t "$image:latest" \
    -t "$image:$tag" \
    --push \
    - || fail "$name 빌드/push 실패"
}

case "$TARGET" in
  all)
    build_push "app" "$BACKEND_DIR" "$BACKEND_IMAGE"
    build_push "web" "$FRONTEND_DIR" "$FRONTEND_IMAGE"
    ;;
  app) build_push "app" "$BACKEND_DIR" "$BACKEND_IMAGE" ;;
  web) build_push "web" "$FRONTEND_DIR" "$FRONTEND_IMAGE" ;;
  *) fail "알 수 없는 대상: $TARGET (all | app | web)" ;;
esac

echo
echo "=== push 완료 — 매장 PC 에서 scripts/deploy 실행 ==="
