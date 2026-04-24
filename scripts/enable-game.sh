#!/bin/bash

# Enables the game for a classroom so students can submit task attempts
# Usage: ./enable-game.sh [classroom_id]
# Example: ./enable-game.sh 593b3210-d509-4116-89f2-0210426b5537

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

if ! docker ps --format '{{.Names}}' | grep -q '^nd-db$'; then
    echo -e "${RED}[FAIL] nd-db container is not running. Run: docker-compose up -d${NC}"
    exit 1
fi

CLASSROOM_ID="$1"

if [ -z "$CLASSROOM_ID" ]; then
    echo -e "${YELLOW}No classroom_id given — enabling game for ALL classrooms.${NC}"
    SQL="UPDATE classroom SET is_game_enabled = TRUE WHERE deleted_at IS NULL;"
    LABEL="all classrooms"
else
    SQL="UPDATE classroom SET is_game_enabled = TRUE WHERE classroom_id = '${CLASSROOM_ID}' AND deleted_at IS NULL;"
    LABEL="classroom ${CLASSROOM_ID}"
fi

docker exec nd-db mysql -uroot -proot nd-db -e "$SQL" 2>/dev/null

if [ -z "$CLASSROOM_ID" ]; then
    ENABLED=$(docker exec nd-db mysql -uroot -proot nd-db -se "SELECT COUNT(*) FROM classroom WHERE is_game_enabled = TRUE AND deleted_at IS NULL;" 2>/dev/null)
    echo -e "${GREEN}[OK] Game enabled for ${LABEL} (${ENABLED} classroom(s) now active).${NC}"
else
    EXISTS=$(docker exec nd-db mysql -uroot -proot nd-db -se "SELECT COUNT(*) FROM classroom WHERE classroom_id = '${CLASSROOM_ID}' AND is_game_enabled = TRUE AND deleted_at IS NULL;" 2>/dev/null)
    if [ "$EXISTS" -eq 1 ]; then
        echo -e "${GREEN}[OK] Game is enabled for ${LABEL}.${NC}"
    else
        echo -e "${RED}[FAIL] Classroom not found or could not be updated. Check the classroom_id.${NC}"
        exit 1
    fi
fi
