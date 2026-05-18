#!/bin/bash
set -e

echo "Checking service health..."

SERVICES=(
    "http://localhost:8000/health"
    "http://localhost:8002/health"
)

for service in "${SERVICES[@]}"; do
    if curl -sf "$service" > /dev/null 2>&1; then
        echo "✓ $service is healthy"
    else
        echo "✗ $service is not responding"
    fi
done

echo "Health check complete!"
