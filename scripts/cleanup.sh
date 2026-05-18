#!/bin/bash
set -e

echo "Cleaning up old logs and temporary files..."
find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
find . -name "*.pyc" -delete 2>/dev/null || true
find . -name "*.pyo" -delete 2>/dev/null || true
find . -name ".pytest_cache" -exec rm -rf {} + 2>/dev/null || true

echo "Cleanup complete!"
