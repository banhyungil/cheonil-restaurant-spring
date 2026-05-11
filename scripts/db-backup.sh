#!/usr/bin/env bash
# ============================================================
#  Cheonil DB Backup (macOS / Linux)
#
#  생성 파일:
#    <BACKUP_DIR>/cheonil_YYYYMMDD_HHMMSS.dump       (custom format, 전체 복원용)
#    <BACKUP_DIR>/cheonil_YYYYMMDD_HHMMSS_data.sql   (plain data-only, append 복원용)
#
#  사용:
#    ./scripts/db-backup.sh
#
#  환경변수 (선택):
#    BACKUP_DIR        백업 저장 경로 (기본: <BACKEND_DIR>/backup)
#    RETENTION_DAYS    이 일수 이전 백업은 자동 삭제 (기본: 14)
#
#  사전 조건:
#    - docker compose 로 cheonil-db 컨테이너가 실행 중
# ============================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

BACKUP_DIR="${BACKUP_DIR:-$BACKEND_DIR/backup}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"

CONTAINER="cheonil-db"
DB="cheonil"
DB_USER="root"

mkdir -p "$BACKUP_DIR"

TS="$(date +%Y%m%d_%H%M%S)"
DUMP_FILE="$BACKUP_DIR/cheonil_${TS}.dump"
DATA_FILE="$BACKUP_DIR/cheonil_${TS}_data.sql"

echo
echo "=== [1/3] Custom dump (전체 복원용) ==="
echo "path: $DUMP_FILE"
docker exec "$CONTAINER" sh -c "pg_dump -U $DB_USER -d $DB -Fc -f /tmp/backup.dump"
docker cp "$CONTAINER:/tmp/backup.dump" "$DUMP_FILE"
docker exec "$CONTAINER" rm -f /tmp/backup.dump

echo
echo "=== [2/3] Data-only SQL (append 복원용) ==="
echo "path: $DATA_FILE"
docker exec "$CONTAINER" sh -c "pg_dump -U $DB_USER -d $DB -Fp --data-only --column-inserts -f /tmp/backup_data.sql"
docker cp "$CONTAINER:/tmp/backup_data.sql" "$DATA_FILE"
docker exec "$CONTAINER" rm -f /tmp/backup_data.sql

echo
echo "=== [3/3] ${RETENTION_DAYS}일 이상 된 백업 정리 ==="
# -mtime +N : N일보다 오래된 파일. +0 은 24h+, +14 는 15일째부터 매칭 (find 표준)
# 호환성을 위해 ctime 대신 mtime, BSD/GNU find 둘 다 동작.
find "$BACKUP_DIR" -maxdepth 1 -type f -name 'cheonil_*' -mtime +"$RETENTION_DAYS" -print -delete

echo
echo "=== 백업 완료 ==="
