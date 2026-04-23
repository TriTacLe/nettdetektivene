#!/bin/bash

# Activates all tasks for a given stop location (or all tasks if no stop given)
# Usage: ./activate-tasks.sh [stopLocation]
# Examples:
#   ./activate-tasks.sh                  # activate all tasks
#   ./activate-tasks.sh nyhetskvartalet  # activate only nyhetskvartalet tasks
#   ./activate-tasks.sh fotografen       # activate only fotografen tasks

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

if ! docker ps --format '{{.Names}}' | grep -q '^nd-db$'; then
    echo -e "${RED}[FAIL] nd-db container is not running. Run: docker-compose up -d${NC}"
    exit 1
fi

STOP="$1"

if [ -n "$STOP" ]; then
    SQL="UPDATE task SET is_active = TRUE WHERE stop_location = '${STOP}';"
    LABEL="stop_location = '$STOP'"
else
    SQL="UPDATE task SET is_active = TRUE;"
    LABEL="all tasks"
fi

docker exec nd-db mysql -und-usr -pnd-pwd nd-db -e "$SQL" 2>/dev/null

ROWS=$(docker exec nd-db mysql -und-usr -pnd-pwd nd-db -se \
    "SELECT ROW_COUNT();" 2>/dev/null)

echo -e "${GREEN}Done. Activated $ROWS task(s) for $LABEL.${NC}"
