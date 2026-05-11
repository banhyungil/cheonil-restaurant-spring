#!/usr/bin/env bash
# ============================================================
#  Cheonil DB Restore - Append mode (macOS / Linux)
#
#  특징:
#    - 기존 데이터를 지우지 않음
#    - 백업의 INSERT 문에 'ON CONFLICT DO NOTHING' 을 자동 추가
#      → PK / unique 충돌 시 해당 row 만 skip
#    - 복원 후 모든 시퀀스를 MAX 값으로 재정렬
#
#  사용:
#    ./scripts/db-restore-append.sh <백업파일_data.sql>
#
#  주의:
#    - 입력은 db-backup.bat 이 생성한 *_data.sql (plain, column-inserts)
#    - .dump (custom format) 는 db-restore-full.sh 사용
# ============================================================
set -euo pipefail

if [[ $# -lt 1 ]]; then
    echo "사용법: $0 <백업파일_data.sql>"
    exit 1
fi

BACKUP_FILE="$1"
if [[ ! -f "$BACKUP_FILE" ]]; then
    echo "[ERROR] 파일 없음: $BACKUP_FILE"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RESET_SQL="$SCRIPT_DIR/sql/reset-sequences.sql"
if [[ ! -f "$RESET_SQL" ]]; then
    echo "[ERROR] reset-sequences.sql 없음: $RESET_SQL"
    exit 1
fi

CONTAINER="cheonil-db"
DB="cheonil"
DB_USER="root"

TMP_FILE="$(mktemp -t cheonil_restore.XXXXXX)"
trap 'rm -f "$TMP_FILE"' EXIT

echo
echo "=== [1/4] INSERT 문에 ON CONFLICT DO NOTHING 추가 ==="
echo "source: $BACKUP_FILE"
echo "temp:   $TMP_FILE"
sed -E 's/^(INSERT INTO .*\));$/\1 ON CONFLICT DO NOTHING;/' "$BACKUP_FILE" > "$TMP_FILE"

echo
echo "=== [2/4] 컨테이너로 SQL 복사 ==="
docker cp "$TMP_FILE" "$CONTAINER:/tmp/restore.sql"

echo
echo "=== [3/4] 데이터 복원 (append) ==="
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB" -v ON_ERROR_STOP=1 -f /tmp/restore.sql
docker exec "$CONTAINER" rm -f /tmp/restore.sql

echo
echo "=== [4/4] 시퀀스 재정렬 ==="
docker cp "$RESET_SQL" "$CONTAINER:/tmp/reset-sequences.sql"
docker exec "$CONTAINER" psql -U "$DB_USER" -d "$DB" -f /tmp/reset-sequences.sql
docker exec "$CONTAINER" rm -f /tmp/reset-sequences.sql

echo
echo "=== Append 복원 완료 ==="
