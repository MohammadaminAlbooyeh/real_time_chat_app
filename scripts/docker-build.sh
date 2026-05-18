#!/bin/bash
set -e

echo "Building all Docker images..."
docker compose build

echo "Images built successfully!"
docker compose images
