#!/bin/bash

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}>>> Starting Distributed Tracing Demo System (v2) <<<${NC}"

# 0. Stop any existing services for a fresh start
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/stop_all.sh" ]; then
    echo -e "${RED}Stopping existing services for fresh start...${NC}"
    bash "$SCRIPT_DIR/stop_all.sh"
    sleep 2
fi

# 1. Start Infrastructure
echo -e "${GREEN}[1/5] Starting Docker Infrastructure (RabbitMQ, Tempo, Loki, Grafana)...${NC}"
docker compose up -d
sleep 5 # Wait for containers to warm up

# Function to kill all background jobs on exit
cleanup() {
    echo -e "${RED}Stopping all services...${NC}"
    jobs -p | xargs -r kill 2>/dev/null
    echo -e "${GREEN}Services stopped. Run 'docker compose down' to stop infrastructure.${NC}"
}
trap cleanup EXIT

# 2. Start Services (Maven)
# Set JAVA_HOME to Java 21
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

start_service() {
    SERVICE_NAME=$1
    PORT=$2
    echo -e "${GREEN}Starting $SERVICE_NAME on port $PORT using Maven...${NC}"
    cd "$SERVICE_NAME"
    mvn -q spring-boot:run > "../logs/$SERVICE_NAME.log" 2>&1 &
    cd ..
}

# Create logs directory
mkdir -p logs
touch logs/graphql-service.log logs/order-service.log logs/inventory-service.log logs/notification-service.log

# Pre-warm Maven (download dependencies once to avoid concurrency lock)
echo -e "${BLUE}Downloading/Verifying Maven Dependencies (First run may take time)...${NC}"
cd graphql-service
mvn -q dependency:go-offline > /dev/null 2>&1
cd ..

start_service "graphql-service" 8080
start_service "order-service" 8081
start_service "inventory-service" 8082
start_service "notification-service" 8083

echo -e "${BLUE}>>> All services are starting in the background! <<<${NC}"
echo -e "Logs are being written to the './logs' directory."
echo -e "GraphQL UI:    http://localhost:8080/graphiql"
echo -e "Grafana:       http://localhost:3000"
echo -e "${RED}Press [CTRL+C] to stop all services.${NC}"

# Tail logs to keep script alive and show output
tail -f logs/*.log
