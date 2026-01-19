#!/bin/bash

# Start the orchestrator service

echo "Starting orchestrator service..."
echo "This service demonstrates distributed tracing across HTTP and RabbitMQ"
echo ""

cd "$(dirname "$0")"
./gradlew :orchestrator-service:bootRun
