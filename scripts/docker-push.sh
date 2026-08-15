#!/bin/bash
set -e

REGISTRY=${1:-your-registry.azurecr.io}
echo "Pushing images to $REGISTRY..."

SERVICES=("api-service" "websocket-service" "worker-service" "search-service" "nginx")
for service in "${SERVICES[@]}"; do
    echo "Tagging and pushing $service..."
    docker tag "real_time_chat_app-$service:latest" "$REGISTRY/$service:latest"
    docker push "$REGISTRY/$service:latest"
done

echo "All images pushed to $REGISTRY"
