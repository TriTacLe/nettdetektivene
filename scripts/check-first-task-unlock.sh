#!/bin/bash

# Tests that the trg_student_join_unlock_first_task trigger works:
# creates a fresh user, joins a classroom via the API, then checks
# whether the first task was automatically unlocked in the database.
#
# Usage: ./check-first-task-unlock.sh [join_code]
# If no join code is given, the script picks the first classroom in the DB.

BASE_URL="http://localhost:8080"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-nd-db}"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

db() {
    docker exec nd-db mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -sN -e "$1" 2>/dev/null
}

api() {
    local method=$1 endpoint=$2 data=${3:-} token=${4:-}
    local args=(-s -w "\n%{http_code}" -X "$method")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")
    curl "${args[@]}" "${BASE_URL}${endpoint}"
}

body()      { echo "$1" | sed '$d'; }
http_code() { echo "$1" | tail -n1 | tr -d '\n\r'; }

pass() { echo -e "  ${GREEN}[PASS]${NC} $1"; }
fail() { echo -e "  ${RED}[FAIL]${NC} $1"; FAILURES=$((FAILURES + 1)); }
info() { echo -e "  ${BLUE}[INFO]${NC} $1"; }

FAILURES=0

# ---- resolve join code ----
JOIN_CODE="${1:-}"
if [ -z "$JOIN_CODE" ]; then
    JOIN_CODE=$(db "SELECT join_code FROM classroom WHERE deleted_at IS NULL AND join_code IS NOT NULL LIMIT 1")
fi

if [ -z "$JOIN_CODE" ]; then
    echo -e "${RED}[ERROR]${NC} No classroom with a join code found. Create one first."
    exit 1
fi

CLASSROOM_ID=$(db "SELECT classroom_id FROM classroom WHERE join_code = '$JOIN_CODE' AND deleted_at IS NULL LIMIT 1")
CLASSROOM_TITLE=$(db "SELECT title FROM classroom WHERE join_code = '$JOIN_CODE' LIMIT 1")

echo "========================================="
echo "First-task auto-unlock trigger test"
echo "========================================="
echo ""
info "Classroom: $CLASSROOM_TITLE ($CLASSROOM_ID)"
info "Join code: $JOIN_CODE"
echo ""

# ---- check server ----
echo "--- Checking backend ---"
if ! curl -s --max-time 5 "${BASE_URL}/api/v1/auth/login" >/dev/null 2>&1; then
    echo -e "${RED}[ERROR]${NC} Backend not reachable at $BASE_URL"
    exit 1
fi
pass "Backend reachable"
echo ""

# ---- check tasks exist ----
FIRST_TASK_ID=$(db "
    SELECT task_id FROM classroom_task
    WHERE classroom_id = '$CLASSROOM_ID' AND is_active = 1
    ORDER BY sort_order ASC LIMIT 1
")
FIRST_TASK_TITLE=$(db "
    SELECT t.title FROM classroom_task ct
    JOIN task t ON ct.task_id = t.task_id
    WHERE ct.classroom_id = '$CLASSROOM_ID' AND ct.is_active = 1
    ORDER BY ct.sort_order ASC LIMIT 1
")

if [ -z "$FIRST_TASK_ID" ]; then
    echo -e "${RED}[ERROR]${NC} Classroom has no active tasks — nothing to unlock."
    exit 1
fi

info "First task: \"$FIRST_TASK_TITLE\" ($FIRST_TASK_ID)"
echo ""

# ---- create test user ----
TS=$(date +%s)
EMAIL="trigger_test_${TS}@ntnu.no"
PASSWORD="TriggerTest123!"
DISPLAY_NAME="TriggerTest $TS"

info "Registering new user: $EMAIL"
reg=$(api "POST" "/api/v1/auth/register" \
    "{\"displayName\":\"$DISPLAY_NAME\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"inst_admin\"}")
reg_code=$(http_code "$reg")

if [ "$reg_code" != "200" ] && [ "$reg_code" != "201" ]; then
    fail "Registration failed (HTTP $reg_code): $(body "$reg")"
    exit 1
fi
pass "User registered (HTTP $reg_code)"

USER_ID=$(db "SELECT user_id FROM app_user WHERE email = '$EMAIL' LIMIT 1")
info "User ID: $USER_ID"

# Add student role and profile so they can join
db "INSERT IGNORE INTO user_role (user_id, role_id)
    SELECT '$USER_ID', role_id FROM role WHERE role_name = 'student'"
db "INSERT IGNORE INTO user_profile (profile_id, user_id, display_name, xp, xp_level)
    VALUES (UUID(), '$USER_ID', '$DISPLAY_NAME', 0, 1)"

# ---- login ----
login=$(api "POST" "/api/v1/auth/login" "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(body "$login" | jq -r '.token' 2>/dev/null)

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    fail "Login failed: $(body "$login")"
    exit 1
fi
pass "Logged in"
echo ""

# ---- check unlock BEFORE joining ----
echo "--- Before joining classroom ---"
unlocked_before=$(db "
    SELECT COUNT(*) FROM user_task_unlocked
    WHERE user_id = '$USER_ID' AND classroom_id = '$CLASSROOM_ID'
")
info "Unlocked tasks: $unlocked_before"
echo ""

# ---- join classroom via API ----
echo "--- Joining classroom via API ---"
join_resp=$(api "POST" "/api/v1/classrooms/join" "{\"joinCode\":\"$JOIN_CODE\"}" "$TOKEN")
join_code=$(http_code "$join_resp")
join_body=$(body "$join_resp")

if [ "$join_code" = "200" ] || [ "$join_code" = "201" ]; then
    pass "Joined classroom (HTTP $join_code)"
else
    fail "Join failed (HTTP $join_code): $join_body"
    exit 1
fi
echo ""

# ---- check unlock AFTER joining ----
echo "--- Checking database after join ---"
unlocked_at=$(db "
    SELECT unlocked_at FROM user_task_unlocked
    WHERE user_id = '$USER_ID'
      AND classroom_id = '$CLASSROOM_ID'
      AND task_id = '$FIRST_TASK_ID'
    LIMIT 1
")

if [ -n "$unlocked_at" ]; then
    pass "First task auto-unlocked by trigger (unlocked_at=$unlocked_at)"
    pass "Trigger trg_student_join_unlock_first_task is working"
else
    fail "First task NOT unlocked — trigger did not fire"
    echo ""
    echo "  Debug info:"
    echo "    user_id:      $USER_ID"
    echo "    classroom_id: $CLASSROOM_ID"
    echo "    task_id:      $FIRST_TASK_ID"
    echo ""
    echo "  Rows in user_task_unlocked for this user:"
    db "SELECT * FROM user_task_unlocked WHERE user_id = '$USER_ID'"
fi

# ---- cleanup ----
echo ""
echo "--- Cleaning up test user ---"
db "DELETE FROM user_task_unlocked WHERE user_id = '$USER_ID'"
db "DELETE FROM classroom_student WHERE user_id = '$USER_ID'"
db "DELETE FROM user_profile WHERE user_id = '$USER_ID'"
db "DELETE FROM user_role WHERE user_id = '$USER_ID'"
db "DELETE FROM app_user WHERE user_id = '$USER_ID'"
info "Test user removed"

echo ""
echo "========================================="
if [ $FAILURES -eq 0 ]; then
    echo -e "${GREEN}All checks passed${NC}"
    exit 0
else
    echo -e "${RED}$FAILURES check(s) failed${NC}"
    exit 1
fi
