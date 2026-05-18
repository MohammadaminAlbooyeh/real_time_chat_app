#!/bin/bash
set -e

ENV=${1:-production}
echo "Deploying to $ENV environment..."

if [ ! -f .env.$ENV ]; then
    echo "Environment file .env.$ENV not found!"
    exit 1
fi

echo "Building production images..."
docker compose -f docker-compose.yml build

echo "Stopping existing services..."
docker compose -f docker-compose.yml down

echo "Starting services..."
docker compose -f docker-compose.yml up -d

echo "Running migrations..."
docker compose exec api-service alembic upgrade head

echo "Health check..."
sleep 5
curl -f http://localhost/health || echo "Health check failed"

echo "Deployment to $ENV complete!"
