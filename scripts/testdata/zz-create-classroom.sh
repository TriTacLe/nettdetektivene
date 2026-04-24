#!/bin/bash

BASE_URL="http://localhost:8080"
INSTITUTION_ID="aaaaaaaa-0000-0000-0000-000000000001"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'
COMMENT_COLOR='\033[90m'

make_api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    local token=$4

    local args=(-s -w "\n%{http_code}" -X "$method")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")

    curl "${args[@]}" "${BASE_URL}${endpoint}" 2>&1
}

get_json_value() {
    echo "$1" | jq -r "$2" 2>/dev/null || echo ""
}

TEMPLATE_PASSWORD="password123"

echo -e "${COMMENT_COLOR}Setting up classroom with students...${NC}"
echo ""

# Log in as instructor
echo -e "${BLUE}[INFO]${NC} Logging in as instructor..."
login_response=$(make_api_call "POST" "/api/v1/auth/feide" \
    "{\"email\":\"instructor@test.no\",\"password\":\"$TEMPLATE_PASSWORD\",\"affiliation\":\"instructor\"}")
login_body=$(echo "$login_response" | sed '$d')
INSTRUCTOR_TOKEN=$(get_json_value "$login_body" ".token")

if [ -z "$INSTRUCTOR_TOKEN" ]; then
    echo -e "${RED}[FAIL]${NC} Could not log in as instructor. Make sure create-admin.sh has run."
    exit 1
fi
echo -e "      ${GREEN}✓${NC} Logged in"

# Check if classroom already exists
existing=$(make_api_call "GET" "/api/v1/classrooms" "" "$INSTRUCTOR_TOKEN")
existing_body=$(echo "$existing" | sed '$d')
CLASSROOM_ID=$(get_json_value "$existing_body" ".data[] | select(.title == \"Testklasse 1\") | .id")

if [ -n "$CLASSROOM_ID" ]; then
    echo -e "${GREEN}⊙${NC} Classroom 'Testklasse 1' already exists (ID: ${CLASSROOM_ID:0:8}...)"
    JOIN_CODE=$(get_json_value "$existing_body" ".data[] | select(.title == \"Testklasse 1\") | .joinCode")
else
    echo -e "${BLUE}[INFO]${NC} Creating classroom 'Testklasse 1'..."
    create_response=$(make_api_call "POST" "/api/v1/classrooms" \
        "{\"title\":\"Testklasse 1\",\"classDesc\":\"Testklasserom for utvikling\",\"institutionId\":\"$INSTITUTION_ID\",\"gameEnabled\":true}" \
        "$INSTRUCTOR_TOKEN")
    create_http=$(echo "$create_response" | tail -n1)
    create_body=$(echo "$create_response" | sed '$d')

    if [ "$create_http" != "200" ] && [ "$create_http" != "201" ]; then
        echo -e "${RED}[FAIL]${NC} Could not create classroom ($create_http): $(echo "$create_body" | head -1)"
        exit 1
    fi

    CLASSROOM_ID=$(get_json_value "$create_body" ".data.id")
    JOIN_CODE=$(get_json_value "$create_body" ".data.joinCode")
    echo -e "      ${GREEN}✓${NC} Created (ID: ${CLASSROOM_ID:0:8}..., Join code: $JOIN_CODE)"
fi

echo ""

# Join each student to the classroom
STUDENTS=(
    "student1@test.no"
    "student2@test.no"
    "student3@test.no"
    "student4@test.no"
    "student5@test.no"
)

echo -e "${BLUE}[INFO]${NC} Joining students to classroom (join code: $JOIN_CODE)..."
echo ""

for EMAIL in "${STUDENTS[@]}"; do
    # Log in as student
    student_login=$(make_api_call "POST" "/api/v1/auth/feide" \
        "{\"email\":\"$EMAIL\",\"password\":\"$TEMPLATE_PASSWORD\",\"affiliation\":\"student\"}")
    student_body=$(echo "$student_login" | sed '$d')
    STUDENT_TOKEN=$(get_json_value "$student_body" ".token")

    if [ -z "$STUDENT_TOKEN" ]; then
        echo -e "  ${RED}✗${NC} $EMAIL — could not log in"
        continue
    fi

    join_response=$(make_api_call "POST" "/api/v1/classrooms/join" \
        "{\"joinCode\":\"$JOIN_CODE\"}" \
        "$STUDENT_TOKEN")
    join_http=$(echo "$join_response" | tail -n1)
    join_body=$(echo "$join_response" | sed '$d')

    if [ "$join_http" = "200" ] || [ "$join_http" = "201" ]; then
        echo -e "  ${GREEN}✓${NC} $EMAIL joined"
    elif echo "$join_body" | grep -qi "already\|exists\|duplicate" || [ "$join_http" = "409" ]; then
        echo -e "  ${GREEN}⊙${NC} $EMAIL already in classroom"
    else
        echo -e "  ${RED}✗${NC} $EMAIL failed ($join_http): $(echo "$join_body" | head -1)"
    fi
done

echo ""
echo "=========================================="
echo -e "${GREEN}[CLASSROOM READY]${NC}"
echo "  Classroom:  Testklasse 1"
echo "  Join code:  $JOIN_CODE"
echo "  Instructor: instructor@test.no"
echo "  Students:   student1-5@test.no"
echo "=========================================="