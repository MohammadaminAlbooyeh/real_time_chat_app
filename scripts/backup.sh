#!/bin/bash
set -e

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/chat_db_$TIMESTAMP.sql"

mkdir -p "$BACKUP_DIR"

echo "Backing up database..."
docker compose exec -T postgres pg_dump -U postgres chat_db > "$BACKUP_FILE"

gzip "$BACKUP_FILE"
echo "Backup saved to $BACKUP_FILE.gz"
