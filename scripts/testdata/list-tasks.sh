#!/bin/bash

# List all tasks from backend database
# Usage: ./list-tasks.sh [email] [password]
# Default: ./list-tasks.sh instructor@test.no password123

BASE_URL="http://localhost:8080"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
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

get_json_value() {
    echo "$1" | jq -r "$2" 2>/dev/null || echo ""
}

check_server() {
    if ! curl -s --max-time 3 "${BASE_URL}/api/v1/tasks" >/dev/null 2>&1; then
        echo -e "${RED}[FAIL] Backend not running. Run: docker-compose up -d${NC}"
        exit 1
    fi
}

check_server

EMAIL="${1:-laerer@ntnu.no}"
PASSWORD="${2:-password123}"

echo -e "${BLUE}[INFO]${NC} Logging in as $EMAIL..."
echo ""

login_response=$(make_api_call "POST" "/api/v1/auth/login" "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
token=$(echo "$login_response" | jq -r '.token' 2>/dev/null)

if [ "$token" = "null" ] || [ -z "$token" ]; then
    echo -e "${RED}[ERROR]${NC} Login failed. Check credentials"
    exit 1
fi

echo -e "${GREEN}✓ Logged in successfully${NC}"
echo ""
echo -e "${BLUE}[INFO]${NC} Listing all tasks from database..."
echo ""

response=$(make_api_call "GET" "/api/v1/tasks" "" "$token")
body="$response"

if [ -z "$body" ] || [ "$body" = "null" ]; then
    echo -e "${RED}[ERROR]${NC} Failed to fetch tasks - empty response"
    exit 1
fi

success=$(echo "$body" | jq -r '.success' 2>/dev/null)
if [ "$success" != "true" ]; then
    echo -e "${RED}[ERROR]${NC} API returned unsuccessful response"
    echo "Response: $body"
    exit 1
fi

task_count=$(echo "$body" | jq '.data | length' 2>/dev/null)

if [ "$task_count" = "null" ] || [ "$task_count" = "0" ]; then
    echo -e "${YELLOW}[INFO]${NC} No tasks found in database"
    exit 0
fi

echo -e "========================================="
echo -e "${GREEN}Total Tasks: $task_count${NC}"
echo "========================================="
echo ""

for ((i=0; i<task_count; i++)); do
    task_id=$(get_json_value "$body" ".data[$i].id")
    title=$(get_json_value "$body" ".data[$i].title")
    task_type=$(get_json_value "$body" ".data[$i].taskType")
    difficulty=$(get_json_value "$body" ".data[$i].difficulty")
    stop_location=$(get_json_value "$body" ".data[$i].stopLocation")

    echo -e "${GREEN}Task ${i} (${task_id:0:8}...)${NC}"
    echo "  Title: $title"
    echo "  Type: $task_type"
    echo "  Difficulty: $difficulty"
    echo "  Stop Location: $stop_location"
    echo ""
done

echo "========================================="
exit 0