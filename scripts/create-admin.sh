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
    local token=$4

    local args=(-s -w "\n%{http_code}" -X "$method")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
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

add_roles() {
    local user_id=$1
    local role_type=$2
    local mysql_user="${MYSQL_USER:-root}"
    local mysql_password="${MYSQL_PASSWORD:-root}"
    local mysql_database="${MYSQL_DATABASE:-nd-db}"

    docker exec nd-db mysql -u"$mysql_user" -p"$mysql_password" "$mysql_database" \
        -e "INSERT IGNORE INTO user_role (user_id, role_id) 
            SELECT '$user_id', role_id FROM role WHERE role_name = '$role_type'" 2>/dev/null
}

check_server

TEMPLATE_INSTRUCTOR="instructor@test.no|Test Instructor"
TEMPLATE_ADMIN="admin@test.no|Test Admin"

TEMPLATE_PASSWORD="password123"

echo -e "${COMMENT_COLOR}Creating template admin and instructor...${NC}"
echo ""

for user_data in "$TEMPLATE_INSTRUCTOR" "$TEMPLATE_ADMIN"; do
    IFS='|' read -r EMAIL DISPLAY_NAME <<< "$user_data"

    if [ "$EMAIL" = "instructor@test.no" ]; then
        ROLE="instructor"
    else
        ROLE="admin"
    fi

    echo -e "${BLUE}[INFO]${NC} Creating $ROLE: $EMAIL..."

    response=$(make_api_call "POST" "/api/v1/auth/register" \
        "{\"displayName\":\"$DISPLAY_NAME\",\"email\":\"$EMAIL\",\"password\":\"$TEMPLATE_PASSWORD\",\"role\":\"inst_admin\"}")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')

    USER_ID=""

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        USER_ID=$(get_json_value "$body" ".userId")
        add_roles "$USER_ID" "instructor"
        add_roles "$USER_ID" "admin"
        add_roles "$USER_ID" "student"

        echo -e "      ${GREEN}✓${NC} Created (ID: ${USER_ID:0:8}...)"
    elif echo "$body" | grep -q "already exists" || [ "$http_code" = "409" ]; then
        echo -e "      ${GREEN}⊙${NC} Already exists"
        login_check=$(make_api_call "POST" "/api/v1/auth/login" \
            "{\"email\":\"$EMAIL\",\"password\":\"$TEMPLATE_PASSWORD\"}")
        login_body=$(echo "$login_check" | sed '$d')
        USER_ID=$(get_json_value "$(make_api_call GET "/api/v1/users/me" "" "$(get_json_value "$login_body" ".token")" | sed '$d')" ".id")
    else
        echo -e "      ${RED}✗${NC} Failed ($(echo "$body" | head -1))"
    fi

    if [ -n "$USER_ID" ]; then
        echo -n "      [VERIFY] Checking roles... "

        CURRENT_ROLES=$(docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
            -sN -e "SELECT GROUP_CONCAT(r.role_name) 
                    FROM user_role ur 
                    JOIN role r ON ur.role_id = r.role_id 
                    WHERE ur.user_id = '$USER_ID'" 2>/dev/null)

        if echo "$CURRENT_ROLES" | grep -qE "instructor|inst_admin"; then
            echo -e "${GREEN}✓${NC} ($CURRENT_ROLES)"
        else
            echo -e "${RED}✗${NC} Missing required role ($CURRENT_ROLES)"
        fi
    fi

    echo ""
done

echo "=========================================="
echo -e "${GREEN}[CREDENTIALS]${NC}"
echo "=========================================="
echo ""
echo "Instructor:"
echo "  Email:    instructor@test.no"
echo "  Password: password123"
echo ""
echo "Admin:"
echo "  Email:    admin@test.no"
echo "  Password: password123"
echo "=========================================="