#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Stopping All Services ===${NC}"

# Stop all Gradle Spring Boot processes
echo -e "${YELLOW}Stopping Gradle Spring Boot services...${NC}"
pkill -f "gradle.*bootRun" 2>/dev/null
pkill -f "gradlew.*bootRun" 2>/dev/null
pkill -f "GradleDaemon" 2>/dev/null

# Stop Java processes for our services
echo -e "${YELLOW}Stopping Java service processes...${NC}"
pkill -f "GraphqlServiceApplication" 2>/dev/null
pkill -f "OrderServiceApplication" 2>/dev/null
pkill -f "InventoryServiceApplication" 2>/dev/null
pkill -f "NotificationServiceApplication" 2>/dev/null
pkill -f "CqrsServiceApplication" 2>/dev/null
pkill -f "OrchestratorServiceApplication" 2>/dev/null

# Wait a bit for processes to terminate
sleep 3

# Check if any processes are still running
RUNNING=$(ps aux | grep -E "GraphqlServiceApplication|OrderServiceApplication|InventoryServiceApplication|NotificationServiceApplication|CqrsServiceApplication|OrchestratorServiceApplication" | grep -v grep | wc -l)
if [ "$RUNNING" -gt 0 ]; then
    echo -e "${YELLOW}Some processes still running, force killing...${NC}"
    pkill -9 -f "GraphqlServiceApplication" 2>/dev/null
    pkill -9 -f "OrderServiceApplication" 2>/dev/null
    pkill -9 -f "InventoryServiceApplication" 2>/dev/null
    pkill -9 -f "NotificationServiceApplication" 2>/dev/null
    pkill -9 -f "CqrsServiceApplication" 2>/dev/null
    pkill -9 -f "OrchestratorServiceApplication" 2>/dev/null
    sleep 2
fi

# Verify ports are free
echo -e "${YELLOW}Checking ports...${NC}"
for port in 8080 8081 8082 8083 8084 8085; do
    if lsof -ti:$port > /dev/null 2>&1; then
        echo -e "${YELLOW}Port $port still in use, killing process...${NC}"
        lsof -ti:$port | xargs kill -9 2>/dev/null
    fi
done

# Optional: Stop Docker infrastructure (commented out by default)
# Uncomment the following lines if you want to stop Docker services too
# echo -e "${YELLOW}Stopping Docker infrastructure...${NC}"
# docker compose down 2>/dev/null

echo -e "${GREEN}✅ All services stopped!${NC}"
