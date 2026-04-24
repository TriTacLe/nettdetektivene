#!/bin/bash

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

if ! docker ps --format '{{.Names}}' | grep -q '^nd-db$'; then
    echo -e "${RED}[FAIL] nd-db container is not running. Run: docker-compose up -d${NC}"
    exit 1
fi

echo -e "${BLUE}[INFO]${NC} Getting the first task by stop_location, difficulty..."
echo ""

FIRST_TASK_ID=$(docker exec nd-db mysql -uroot -proot nd-db -se \
    "SELECT task_id FROM task ORDER BY stop_location IS NULL ASC, stop_location ASC, difficulty ASC LIMIT 1;" 2>/dev/null)

if [ -z "$FIRST_TASK_ID" ]; then
    echo -e "${RED}[ERROR]${NC} No tasks found in database"
    exit 1
fi

echo -e "${GREEN}✓ First task ID: ${FIRST_TASK_ID}${NC}"
echo ""
echo -e "${BLUE}[INFO]${NC} Deactivating all tasks except the first..."
echo ""

docker exec nd-db mysql -uroot -proot nd-db -e \
    "UPDATE task SET is_active = FALSE WHERE task_id != '${FIRST_TASK_ID}';" 2>/dev/null

docker exec nd-db mysql -uroot -proot nd-db -e \
    "UPDATE task SET is_active = TRUE WHERE task_id = '${FIRST_TASK_ID}';" 2>/dev/null

DEACTIVATED_COUNT=$(docker exec nd-db mysql -uroot -proot nd-db -se \
    "SELECT COUNT(*) FROM task WHERE is_active = FALSE;" 2>/dev/null)

ACTIVE_COUNT=$(docker exec nd-db mysql -uroot -proot nd-db -se \
    "SELECT COUNT(*) FROM task WHERE is_active = TRUE;" 2>/dev/null)

echo -e "${GREEN}✓ Done${NC}"
echo "  Deactivated tasks: $DEACTIVATED_COUNT"
echo "  Active tasks: $ACTIVE_COUNT (task ID: ${FIRST_TASK_ID})"
echo ""
echo "Active task details:"
docker exec nd-db mysql -uroot -proot nd-db -e \
    "SELECT task_id, title, stop_location, difficulty FROM task WHERE task_id = '${FIRST_TASK_ID}';" 2>/dev/null