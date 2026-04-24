#!/bin/bash

# Unlocks the first task for a user in a classroom
# Usage: ./unlock-first-task.sh [email] [password] [classroom_id]

BASE_URL="http://localhost:8080"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

make_api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    local token=$4

    local args=(-s -X "$method")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")

    curl "${args[@]}" "${BASE_URL}${endpoint}" 2>&1
}

check_server() {
    if ! curl -s --max-time 3 "${BASE_URL}/api/v1/tasks" >/dev/null 2>&1; then
        echo -e "${RED}[FAIL] Backend not running. Run: docker-compose up -d${NC}"
        exit 1
    fi
}

check_server

EMAIL="${1:-test@test}"
PASSWORD="${2:-password123}"
CLASSROOM_ID="${3:-}"

if [ -z "$CLASSROOM_ID" ]; then
    echo -e "${RED}[ERROR]${NC} Classroom ID required"
    echo "Usage: $0 <email> <password> <classroom_id>"
    exit 1
fi

echo -e "${BLUE}[INFO]${NC} Logging in as $EMAIL..."
login_response=$(make_api_call "POST" "/api/v1/auth/login" "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
token=$(echo "$login_response" | jq -r '.token' 2>/dev/null)

if [ "$token" = "null" ] || [ -z "$token" ]; then
    echo -e "${RED}[ERROR]${NC} Login failed. Check credentials."
    exit 1
fi

echo -e "${GREEN}Logged in successfully${NC}"
echo ""

USER_ID=$(docker exec nd-db mysql -uroot -proot nd-db -sN \
    -e "SELECT user_id FROM app_user WHERE email = '$EMAIL';" 2>/dev/null)

if [ -z "$USER_ID" ]; then
    echo -e "${RED}[ERROR]${NC} User not found: $EMAIL"
    exit 1
fi

echo -e "${BLUE}[INFO]${NC} Getting first task for classroom..."

FIRST_TASK_ID=$(docker exec nd-db mysql -uroot -proot nd-db -sN \
    -e "SELECT ct.task_id FROM classroom_task ct 
        JOIN task t ON ct.task_id = t.task_id 
        WHERE ct.classroom_id = '$CLASSROOM_ID' 
        AND ct.is_active = 1 
        ORDER BY ct.sort_order ASC 
        LIMIT 1;" 2>/dev/null)

if [ -z "$FIRST_TASK_ID" ]; then
    echo -e "${RED}[ERROR]${NC} No active tasks found in classroom"
    exit 1
fi

TASK_TITLE=$(docker exec nd-db mysql -uroot -proot nd-db -sN \
    -e "SELECT title FROM task WHERE task_id = '$FIRST_TASK_ID';" 2>/dev/null)

echo -e "${BLUE}[INFO]${NC} Unlocking task: $TASK_TITLE"

docker exec nd-db mysql -uroot -proot nd-db \
    -e "INSERT IGNORE INTO user_task_unlocked (user_id, classroom_id, task_id, unlocked_at) 
        VALUES ('$USER_ID', '$CLASSROOM_ID', '$FIRST_TASK_ID', NOW());" 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}[OK]${NC} Task unlocked successfully"
else
    echo -e "${YELLOW}[WARN]${NC} Task may already be unlocked or an error occurred"
fi

echo ""
echo -e "${GREEN}Done.${NC}"