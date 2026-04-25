#!/usr/bin/env sh

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

BACKUP_DIR="${BACKUP_DIR:-$APP_DIR/backups/mysql}"
BACKUP_RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-7}"
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
OUTPUT_FILE="$BACKUP_DIR/nexatalk-$TIMESTAMP.sql.gz"

mkdir -p "$BACKUP_DIR"

compose exec -T mysql sh -c \
  'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --quick --default-character-set=utf8mb4 "$MYSQL_DATABASE"' \
  | gzip > "$OUTPUT_FILE"

ln -sfn "$(basename "$OUTPUT_FILE")" "$BACKUP_DIR/latest.sql.gz"

find "$BACKUP_DIR" -type f -name 'nexatalk-*.sql.gz' -mtime +"$BACKUP_RETENTION_DAYS" -delete

echo "Backup created: $OUTPUT_FILE"
