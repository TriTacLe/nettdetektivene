#!/bin/bash

BASE_URL="http://localhost:8080"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

COMMENT_COLOR='\033[90m'

make_api_call() {
    local method=$1
    local endpoint=$2
    local data=$3

    local args=(-s -w "\n%{http_code}" -X "$method")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")

    curl "${args[@]}" "${BASE_URL}${endpoint}" 2>&1
}

get_json_value() {
    echo "$1" | jq -r "$2" 2>/dev/null || echo ""
}

check_server() {
    if ! curl -s --max-time 3 "${BASE_URL}/api/v1/auth/login" >/dev/null 2>&1; then
        echo -e "${RED}[FAIL] Backend not running. Run: docker-compose up -d${NC}"
        exit 1
    fi
}

add_student_role() {
    local user_id=$1
    local mysql_user="${MYSQL_USER:-root}"
    local mysql_password="${MYSQL_PASSWORD:-root}"
    local mysql_database="${MYSQL_DATABASE:-nd-db}"

    docker exec nd-db mysql -u"$mysql_user" -p"$mysql_password" "$mysql_database" \
        -e "INSERT IGNORE INTO user_role (user_id, role_id) 
            SELECT '$user_id', role_id FROM role WHERE role_name = 'student'" 2>/dev/null
}

check_server

TEMPLATE_STUDENTS=(
    "student1@test.no|Student One"
    "student2@test.no|Student Two"
    "student3@test.no|Student Three"
    "student4@test.no|Student Four"
    "student5@test.no|Student Five"
)

TEMPLATE_PASSWORD="password123"

echo -e "${COMMENT_COLOR}Creating template Feide students...${NC}"
echo ""

SUCCESS_COUNT=0
FAIL_COUNT=0

for student_data in "${TEMPLATE_STUDENTS[@]}"; do
    IFS='|' read -r EMAIL DISPLAY_NAME <<< "$student_data"

    echo -e "${BLUE}[INFO]${NC} Creating $EMAIL..."

    response=$(make_api_call "POST" "/api/v1/auth/feide" \
        "{\"displayName\":\"$DISPLAY_NAME\",\"email\":\"$EMAIL\",\"affiliation\":\"student\",\"password\":\"$TEMPLATE_PASSWORD\"}")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        USER_ID=$(get_json_value "$body" ".feideId")
        add_student_role "$USER_ID"
        echo -e "      ${GREEN}✓${NC} Created (ID: ${USER_ID:0:8}...)"
        ((SUCCESS_COUNT++))
    else
        if echo "$body" | grep -q "already exists" || [ "$http_code" = "409" ]; then
            echo -e "      ${GREEN}⊙${NC} Already exists"
            ((SUCCESS_COUNT++))
        else
            echo -e "      ${RED}✗${NC} Failed ($(echo "$body" | head -1))"
            ((FAIL_COUNT++))
        fi
    fi
    echo ""
done

echo "=========================================="
echo -e "${BLUE}[SUMMARY]${NC}"
echo "=========================================="
echo "Total:   ${#TEMPLATE_STUDENTS[@]}"
echo -e "Success: ${GREEN}${SUCCESS_COUNT}${NC}"
echo -e "Failed:  ${RED}${FAIL_COUNT}${NC}"
echo "=========================================="
echo ""
echo -e "${GREEN}[CREDENTIALS]${NC}"
echo "Password for all students: password123"
echo ""
for student_data in "${TEMPLATE_STUDENTS[@]}"; do
    IFS='|' read -r EMAIL DISPLAY_NAME <<< "$student_data"
    echo "  • $EMAIL ($DISPLAY_NAME)"
done

if [ $FAIL_COUNT -eq 0 ]; then
    exit 0
else
    exit 1
fi