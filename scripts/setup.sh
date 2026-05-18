#!/bin/bash
set -e

echo "Setting up Real-Time Chat Application..."

if ! command -v docker &> /dev/null; then
    echo "Docker is required but not installed."
    exit 1
fi

if [ ! -f .env ]; then
    cp .env.example .env
    echo "Created .env from .env.example"
fi

echo "Building Docker images..."
docker compose build

echo "Starting services..."
docker compose up -d

echo "Running database migrations..."
docker compose exec api-service alembic upgrade head

echo "Setup complete! Services are running."
echo "API: http://localhost:8000"
echo "WebSocket: ws://localhost:8001"
echo "Grafana: http://localhost:3001"
echo "Jaeger: http://localhost:16686"
