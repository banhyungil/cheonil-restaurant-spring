#!/usr/bin/env bash
# ============================================================
#  Cheonil DB Restore - Full mode (macOS / Linux)
#
#  ⚠️ 위험: 기존 cheonil DB 를 완전히 삭제하고 백업으로 재구성합니다.
#           실행 전 'YES' 확인을 받습니다.
#
#  동작:
#    1) app 컨테이너 정지 (DB 연결 해제)
#    2) 남은 세션 강제 종료
#    3) DROP DATABASE / CREATE DATABASE
#    4) pg_restore 로 .dump 적용
#    5) app 컨테이너 재시작
#
#  사용:
#    ./scripts/db-restore-full.sh <백업파일.dump>
#
#  주의:
#    - 입력은 db-backup.bat 이 생성한 *.dump (custom format)
#    - *_data.sql (plain data-only) 는 db-restore-append.sh 사용
# ============================================================
set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "사용법: $0 <백업파일.dump>"
    exit 1
fi

BACKUP_FILE="$1"
if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "[ERROR] 파일 없음: $BACKUP_FILE"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CONTAINER="cheonil-db"
DB="cheonil"
DB_USER="root"

echo
echo "⚠️  경고: $DB DB 가 완전히 삭제되고 아래 백업으로 대체됩니다."
echo "    백업 파일: $BACKUP_FILE"
echo
read -rp "계속하려면 'YES' 입력: " CONFIRM
if [[ "$CONFIRM" != "YES" ]]; then
    echo "취소되었습니다."
    exit 1
fi

cleanup_on_error() {
    local exit_code=$?
    echo
    echo "[복구 시도] app 컨테이너 재시작 (exit=$exit_code)"
    (cd "$BACKEND_DIR" && docker compose start app) || true
    docker exec "$CONTAINER" rm -f /tmp/restore.dump 2>/dev/null || true
    exit "$exit_code"
}

echo
echo "=== [1/6] app 컨테이너 정지 ==="
(cd "$BACKEND_DIR" && docker compose stop app)

# 이 시점부터 실패하면 app 을 다시 살려야 함
trap cleanup_on_error ERR

echo
echo "=== [2/6] 백업 파일을 컨테이너로 복사 ==="
docker cp "$BACKUP_FILE" "$CONTAINER:/tmp/restore.dump"

echo
echo "=== [3/6] 잔여 DB 세션 강제 종료 ==="
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c \
    "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$DB' AND pid<>pg_backend_pid();" > /dev/null

echo
echo "=== [4/6] DB DROP / CREATE ==="
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB;"
docker exec "$CONTAINER" psql -U "$DB_USER" -d postgres -c "CREATE DATABASE $DB;"

echo
echo "=== [5/6] pg_restore ==="
# pg_restore 는 일부 경고만으로도 exit 1 을 반환할 수 있으므로 fallback 처리
docker exec "$CONTAINER" pg_restore -U "$DB_USER" -d "$DB" --no-owner --no-acl /tmp/restore.dump \
    || echo "[WARN] pg_restore 가 경고를 출력했습니다. 위 로그를 확인하세요."
docker exec "$CONTAINER" rm -f /tmp/restore.dump

trap - ERR

echo
echo "=== [6/6] app 컨테이너 재시작 ==="
(cd "$BACKEND_DIR" && docker compose start app)

echo
echo "=== Full 복원 완료 ==="
