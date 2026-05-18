#!/bin/bash
set -e

SCENARIO=${1:-medium}
echo "Running $SCENARIO load test..."

if ! command -v locust &> /dev/null; then
    echo "Installing locust..."
    pip install locust
fi

locust -f tests/load/locustfile.py \
    --host http://localhost:8000 \
    --users 50 \
    --spawn-rate 5 \
    --run-time 5m \
    --headless \
    --print-stats

echo "Load test complete!"
