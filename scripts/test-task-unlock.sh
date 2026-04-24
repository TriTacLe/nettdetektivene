#!/bin/bash

# Test Task Unlock Feature
# Tests that passing a task unlocks the next task for the user

BASE_URL="http://localhost:8080"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

# CONFIGURATION
INSTITUTION_ID="a1b2c3d4-0000-0000-0000-000000000003"
INSTITUTION_NAME="NTNU"

INSTRUCTOR_EMAIL="instructor_unlock_test_$(date +%s)@ntnu.no"
INSTRUCTOR_NAME="Test Instructor"
INSTRUCTOR_ROLE="inst_admin"

STUDENT_EMAIL="student_unlock_test_$(date +%s)@ntnu.no"
STUDENT_NAME="Test Student"
TEST_PASS="password123"

# GLOBAL STATE
INSTRUCTOR_TOKEN="" INSTRUCTOR_USER_ID=""
STUDENT_TOKEN="" STUDENT_USER_ID=""
CLASSROOM_ID=""
TASK_IDS=()

# REUSABLE FUNCTIONS
call_api() {
    local method=$1 endpoint=$2 data=${3:-} token=${4:-}
    local args=(-s -w "\n%{http_code}" -X "$method")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")
    curl "${args[@]}" "${BASE_URL}${endpoint}" 2>&1
}

test_endpoint() {
    local method=$1 endpoint=$2 data=${3:-} desc=$4 token=${5:-}
    local response http_code body
    response=$(call_api "$method" "$endpoint" "$data" "$token")
    http_code=$(echo "$response" | tail -n1 | tr -d '\n\r')
    body=$(echo "$response" | sed '$d')

    printf "  %-65s " "$desc"
    if [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
        echo -e "${GREEN}PASS${NC} ($http_code)"
        ((PASS++))
        echo "$body"
    else
        echo -e "${RED}FAIL${NC} ($http_code)"
        ((FAIL++))
        echo "  Response: $body"
    fi
}

get_json_value() {
    echo "$1" | jq -r "$2" 2>/dev/null || echo ""
}

# SETUP
setup() {
    echo "========================================="
    echo "Task Unlock Feature Test"
    echo "========================================="
    echo ""

    # Check server
    echo "Checking server..."
    if ! curl -s --max-time 5 "${BASE_URL}/api/v1/auth/login" >/dev/null 2>&1; then
        echo -e "${RED}Backend not running${NC}"
        exit 1
    fi
    echo -e "  ${GREEN}Server OK${NC}"

    # Load .env
    set -a
    [ -f "$(dirname "$0")/.env" ] && source "$(dirname "$0")/.env"
    set +a

    # Cleanup
    echo ""
    echo "Cleaning up previous test data..."
    cleanup_database

    # Create users
    echo ""
    echo "Creating test users..."

    # Register instructor
    call_api "POST" "/api/v1/auth/register" \
        "{\"displayName\":\"$INSTRUCTOR_NAME\",\"email\":\"$INSTRUCTOR_EMAIL\",\"password\":\"$TEST_PASS\",\"role\":\"$INSTRUCTOR_ROLE\"}" >/dev/null
    echo -e "  ${GREEN}Instructor registered${NC}"

    # Register student
    call_api "POST" "/api/v1/auth/register" \
        "{\"displayName\":\"$STUDENT_NAME\",\"email\":\"$STUDENT_EMAIL\",\"password\":\"$TEST_PASS\",\"role\":\"inst_admin\"}" >/dev/null
    echo -e "  ${GREEN}Student registered${NC}"

    # Get user IDs
    INSTRUCTOR_USER_ID=$(docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" -sN -e "SELECT user_id FROM app_user WHERE email = '$INSTRUCTOR_EMAIL'" 2>/dev/null)
    STUDENT_USER_ID=$(docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" -sN -e "SELECT user_id FROM app_user WHERE email = '$STUDENT_EMAIL'" 2>/dev/null)

    # Add roles
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO user_role (user_id, role_id) SELECT '$INSTRUCTOR_USER_ID', role_id FROM role WHERE role_name = 'instructor'" 2>/dev/null
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO user_role (user_id, role_id) SELECT '$INSTRUCTOR_USER_ID', role_id FROM role WHERE role_name = 'student'" 2>/dev/null
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO user_role (user_id, role_id) SELECT '$STUDENT_USER_ID', role_id FROM role WHERE role_name = 'student'" 2>/dev/null

    # Re-login to get tokens with updated roles
    sleep 1
    local instr_login student_login
    instr_login=$(call_api "POST" "/api/v1/auth/login" "{\"email\":\"$INSTRUCTOR_EMAIL\",\"password\":\"$TEST_PASS\"}")
    INSTRUCTOR_TOKEN=$(get_json_value "$(echo "$instr_login" | sed '$d')" ".token")

    student_login=$(call_api "POST" "/api/v1/auth/login" "{\"email\":\"$STUDENT_EMAIL\",\"password\":\"$TEST_PASS\"}")
    STUDENT_TOKEN=$(get_json_value "$(echo "$student_login" | sed '$d')" ".token")

    echo -e "  ${GREEN}Users logged in${NC}"

    # Ensure institution exists
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO institution (institution_id, inst_name) VALUES ('$INSTITUTION_ID', '$INSTITUTION_NAME')" 2>/dev/null

    # Create classroom
    echo ""
    echo "Creating test entities..."
    local cr
    cr=$(call_api "POST" "/api/v1/classrooms" \
        "{\"title\":\"Test Class\",\"classDesc\":\"Test\",\"institutionId\":\"$INSTITUTION_ID\"}" "$INSTRUCTOR_TOKEN" | sed '$d')
    CLASSROOM_ID=$(get_json_value "$cr" ".data.id")
    echo -e "  ${GREEN}Classroom: $CLASSROOM_ID${NC}"

    # Enable game mode
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "UPDATE classroom SET is_game_enabled = true WHERE classroom_id = '$CLASSROOM_ID'" 2>/dev/null

    # Add users to classroom
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO classroom_instructor (classroom_id, user_id) VALUES ('$CLASSROOM_ID', '$INSTRUCTOR_USER_ID')" 2>/dev/null
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO classroom_student (classroom_id, user_id, display_name, is_allowed, name_change_status) VALUES ('$CLASSROOM_ID', '$INSTRUCTOR_USER_ID', '$INSTRUCTOR_NAME', true, 'NONE')" 2>/dev/null
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO classroom_student (classroom_id, user_id, display_name, is_allowed, name_change_status) VALUES ('$CLASSROOM_ID', '$STUDENT_USER_ID', '$STUDENT_NAME', true, 'NONE')" 2>/dev/null

    # Create user_profile for student
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO user_profile (user_id, xp, xp_level) VALUES ('$STUDENT_USER_ID', 0, 1)" 2>/dev/null

    # Create 6 tasks
    echo "  Creating 6 tasks with sort_order 1-6..."
    local tr
    for i in {1..6}; do
        tr=$(call_api "POST" "/api/v1/tasks" \
            "{\"title\":\"Task $i\",\"taskType\":\"EMAIL\",\"difficulty\":$i,\"content\":\"{}\",\"stopLocation\":\"stop_$i\"}" "$INSTRUCTOR_TOKEN" | sed '$d')
        local task_id=$(get_json_value "$tr" ".data.id")
        TASK_IDS+=("$task_id")
        echo -e "    ${GREEN}Task $i: $task_id${NC}"
    done

    # Add tasks to classroom with sort_order
    for i in {0..5}; do
        local sort_order=$((i + 1))
        docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
            -e "INSERT INTO classroom_task (classroom_id, task_id, sort_order, is_active) VALUES ('$CLASSROOM_ID', '${TASK_IDS[$i]}', $sort_order, 1)" 2>/dev/null
    done

    echo ""
}

# CLEANUP
cleanup_database() {
    local test_emails="'$INSTRUCTOR_EMAIL','$STUDENT_EMAIL'"

    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM user_task_unlocked WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM task_attempt WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM task_best_score WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM classroom_task WHERE classroom_id IN (SELECT classroom_id FROM classroom WHERE created_by IN (SELECT user_id FROM app_user WHERE email IN ($test_emails)))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM classroom_student WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM classroom_instructor WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM classroom WHERE created_by IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM user_profile WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM user_role WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM task WHERE owner_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "DELETE FROM app_user WHERE email IN ($test_emails)" 2>/dev/null || true
}

# TESTS
test_unlock_feature() {
    echo "========================================="
    echo "Testing Task Unlock Feature (6 Tasks)"
    echo "========================================="
    echo ""

    echo "Step 0: Check unlocked tasks BEFORE any attempts"
    echo "-------------------------------------------------------"
    local unlocked_before
    unlocked_before=$(call_api "GET" "/api/v1/classrooms/$CLASSROOM_ID/tasks/unlocked" "" "$STUDENT_TOKEN" | sed '$d')
    local count_before=$(get_json_value "$unlocked_before" ".data | length")

    echo "  Unlocked tasks count: $count_before"

    if [ "$count_before" = "0" ]; then
        echo -e "  ${GREEN}INFO${NC} - No tasks unlocked initially (expected)"
    else
        echo "  Unlocked tasks:"
        echo "$unlocked_before" | jq -r '.data[] | "    - \(.task.title) (sort_order: \(.sortOrder))"' 2>/dev/null || echo "$unlocked_before"
    fi

    echo ""
    echo "Step 1: Unlock first task manually (so student can start)"
    echo "-------------------------------------------------------"
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
        -e "INSERT IGNORE INTO user_task_unlocked (user_id, classroom_id, task_id, unlocked_at) VALUES ('$STUDENT_USER_ID', '$CLASSROOM_ID', '${TASK_IDS[0]}', NOW())" 2>/dev/null

    echo -e "  ${GREEN}Task 1 manually unlocked${NC}"

    echo ""
    echo "Step 2: Test unlock chain - Pass each task and verify next unlocks"
    echo "-------------------------------------------------------"

    for i in {0..4}; do
        local current_task_num=$((i + 1))
        local next_task_num=$((i + 2))
        local current_task_id="${TASK_IDS[$i]}"
        local next_task_id="${TASK_IDS[$i+1]}"

        echo ""
        echo "--- Submitting PASSING attempt on Task $current_task_num ---"
        local attempt_response
        attempt_response=$(call_api "POST" "/api/v1/attempts" \
            "{\"taskId\":\"$current_task_id\",\"classroomId\":\"$CLASSROOM_ID\",\"score\":90}" "$STUDENT_TOKEN")
        local attempt_http=$(echo "$attempt_response" | tail -n1)
        local attempt_body=$(echo "$attempt_response" | sed '$d')

        if [ "$attempt_http" = "200" ] || [ "$attempt_http" = "201" ]; then
            echo -e "  ${GREEN}PASS${NC} - Task $current_task_num attempt submitted (HTTP $attempt_http)"
            ((PASS++))
        else
            echo -e "  ${RED}FAIL${NC} - Task $current_task_num attempt failed (HTTP $attempt_http)"
            echo "  Response: $attempt_body"
            ((FAIL++))
            continue
        fi

        sleep 1

        echo "--- Checking if Task $next_task_num is now unlocked ---"
        local unlocked_after
        unlocked_after=$(call_api "GET" "/api/v1/classrooms/$CLASSROOM_ID/tasks/unlocked" "" "$STUDENT_TOKEN" | sed '$d')
        local next_task_unlocked=$(echo "$unlocked_after" | jq -r ".data[].task.id | select(. == \"$next_task_id\")" 2>/dev/null)

        if [ "$next_task_unlocked" = "$next_task_id" ]; then
            echo -e "  ${GREEN}PASS${NC} - Task $next_task_num is now unlocked!"
            ((PASS++))
        else
            echo -e "  ${RED}FAIL${NC} - Task $next_task_num not in unlocked list"
            echo "  Unlocked tasks:"
            echo "$unlocked_after" | jq -r '.data[] | "    - \(.task.title) (sort_order: \(.sortOrder))"' 2>/dev/null || echo "$unlocked_after"
            ((FAIL++))
        fi
    done

    echo ""
    echo "Step 3: Pass Task 6 (final task - no next to unlock)"
    echo "-------------------------------------------------------"
    local attempt_response
    attempt_response=$(call_api "POST" "/api/v1/attempts" \
        "{\"taskId\":\"${TASK_IDS[5]}\",\"classroomId\":\"$CLASSROOM_ID\",\"score\":95}" "$STUDENT_TOKEN")
    local attempt_http=$(echo "$attempt_response" | tail -n1)
    local attempt_body=$(echo "$attempt_response" | sed '$d')

    if [ "$attempt_http" = "200" ] || [ "$attempt_http" = "201" ]; then
        echo -e "  ${GREEN}PASS${NC} - Task 6 attempt submitted (HTTP $attempt_http)"
        ((PASS++))
    else
        echo -e "  ${RED}FAIL${NC} - Task 6 attempt failed (HTTP $attempt_http)"
        echo "  Response: $attempt_body"
        ((FAIL++))
    fi

    echo ""
    echo "Step 4: Final database check - all unlocked tasks"
    echo "-------------------------------------------------------"
    local db_unlocked=$(docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" -sN -e "
        SELECT ct.sort_order, t.title, utu.unlocked_at
        FROM user_task_unlocked utu
        JOIN task t ON utu.task_id = t.task_id
        JOIN classroom_task ct ON utu.classroom_id = ct.classroom_id AND utu.task_id = ct.task_id
        WHERE utu.user_id = '$STUDENT_USER_ID' AND utu.classroom_id = '$CLASSROOM_ID'
        ORDER BY ct.sort_order
    " 2>/dev/null)

    if [ -n "$db_unlocked" ]; then
        echo "  Unlocked tasks in database:"
        echo "$db_unlocked" | while IFS=$'\t' read -r sort_order title unlocked_at; do
            echo "    - Task $sort_order: $title (unlocked at: $unlocked_at)"
        done

        local unlocked_count=$(echo "$db_unlocked" | wc -l | tr -d ' ')
        echo -e "  Total unlocked: $unlocked_count/6 tasks"

        if [ "$unlocked_count" = "6" ]; then
            echo -e "  ${GREEN}PASS${NC} - All 6 tasks unlocked!"
            ((PASS++))
        else
            echo -e "  ${RED}FAIL${NC} - Expected 6 unlocked tasks, got $unlocked_count"
            ((FAIL++))
        fi
    else
        echo -e "  ${RED}FAIL${NC} - No records in user_task_unlocked table"
        ((FAIL++))
    fi
}

# MAIN
setup
test_unlock_feature

echo ""
echo "========================================="
echo -e "RESULTS: ${GREEN}${PASS} passed${NC}, ${RED}${FAIL} failed${NC}"
echo "========================================="

if [ $FAIL -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${YELLOW}Some tests failed - check logs above${NC}"
    exit 1
fi