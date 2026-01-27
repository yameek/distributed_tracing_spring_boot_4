#!/bin/bash

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${RED}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${RED}║           Docker Reset Script - Full Clean Restart            ║${NC}"
echo -e "${RED}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 1. Stop all running containers from this compose project
echo -e "${YELLOW}[1/5] Stopping all containers...${NC}"
docker compose down --remove-orphans
sleep 2

# 2. Remove all containers (including stopped ones)
echo -e "${YELLOW}[2/5] Removing all project containers...${NC}"
docker compose rm -f -s -v 2>/dev/null

# 3. Remove project volumes (clears persistent data like Postgres, Loki, Tempo)
echo -e "${YELLOW}[3/5] Removing Docker volumes (clearing persistent data)...${NC}"
docker compose down -v 2>/dev/null

# 4. Remove project images to force fresh pull/build
echo -e "${YELLOW}[4/5] Removing Docker images used by this project...${NC}"
docker compose down --rmi local 2>/dev/null

# List images that will be affected
echo -e "${BLUE}Pulling fresh images...${NC}"
docker compose pull

# 5. Start fresh containers
echo -e "${GREEN}[5/5] Starting fresh Docker containers...${NC}"
docker compose up -d --force-recreate

echo ""
echo -e "${GREEN}╔════════════════════════════════════════════════════════════════╗${NC}"
echo -e "${GREEN}║              Docker Reset Complete!                            ║${NC}"
echo -e "${GREEN}╚════════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BLUE}All containers are now running with fresh configurations:${NC}"
echo -e "  • OTel Collector:   http://localhost:4317 (gRPC), :4318 (HTTP), :8888 (metrics)"
echo -e "  • Grafana:          http://localhost:3000"
echo -e "  • Tempo:            http://localhost:3200"
echo -e "  • Loki:             http://localhost:3100"
echo -e "  • RabbitMQ:         http://localhost:15672 (guest/guest)"
echo -e "  • PostgreSQL:       localhost:5433"
echo ""
echo -e "${YELLOW}Note: All persistent data (Postgres, Loki, Tempo) has been cleared.${NC}"
echo -e "${GREEN}Run ./run_all.sh to start the Java services.${NC}"
