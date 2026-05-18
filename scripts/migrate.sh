#!/bin/bash
set -e

echo "Running database migrations..."

if command -v docker &> /dev/null && docker compose ps api-service &> /dev/null; then
    docker compose exec api-service alembic upgrade head
else
    echo "Running migrations locally..."
    cd services/api-service
    alembic upgrade head
fi

echo "Migrations complete!"
