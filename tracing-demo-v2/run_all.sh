#!/bin/bash

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}>>> Starting Distributed Tracing Demo System (v2) with Spring Boot 4.0.1 <<<${NC}"
echo -e "${BLUE}>>> Java 25 + Gradle 9.2.1 + Groovy DSL <<<${NC}"
echo "Java version: $(java -version 2>&1 | head -1)"
echo "Gradle version: $(./gradlew --version | grep "Gradle" | head -1)"

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

# 2. Start Services (Gradle)
echo "Using Java: $(java -version 2>&1 | head -1)"

start_service() {
    SERVICE_NAME=$1
    PORT=$2
    echo -e "${GREEN}Starting $SERVICE_NAME on port $PORT using Gradle...${NC}"
    ./gradlew :$SERVICE_NAME:bootRun > "logs/$SERVICE_NAME.log" 2>&1 &
}

# Create logs directory
mkdir -p logs
touch logs/graphql-service.log logs/order-service.log logs/inventory-service.log logs/notification-service.log logs/cqrs-service.log logs/orchestrator-service.log

# Pre-build all services (download dependencies once to avoid concurrency issues)
echo -e "${BLUE}Building services with Gradle (First run may take time)...${NC}"
./gradlew build -x test > /dev/null 2>&1

echo -e "${GREEN}[2/5] Starting Core Services...${NC}"
start_service "graphql-service" 8080
start_service "order-service" 8081
start_service "inventory-service" 8082
start_service "notification-service" 8083

echo -e "${GREEN}[3/5] Starting Advanced Services...${NC}"
start_service "cqrs-service" 8084
start_service "orchestrator-service" 8085

echo -e "${BLUE}>>> All services are starting in the background! <<<${NC}"
echo ""
echo -e "${GREEN}Core Services:${NC}"
echo -e "  GraphQL UI:         http://localhost:8080/graphiql"
echo -e "  Order Service:      http://localhost:8081"
echo -e "  Inventory Service:  http://localhost:8082"
echo -e "  Notification:       http://localhost:8083"
echo ""
echo -e "${GREEN}Advanced Services:${NC}"
echo -e "  CQRS Service:       http://localhost:8084/api/products"
echo -e "  Orchestrator:       http://localhost:8085/api/workflows"
echo ""
echo -e "${GREEN}Infrastructure:${NC}"
echo -e "  Grafana:            http://localhost:3000"
echo -e "  RabbitMQ:           http://localhost:15672 (guest/guest)"
echo ""
echo -e "${BLUE}Logs:${NC} ./logs directory"
echo -e "${RED}Press [CTRL+C] to stop all services.${NC}"

# Tail logs to keep script alive and show output
tail -f logs/*.log
