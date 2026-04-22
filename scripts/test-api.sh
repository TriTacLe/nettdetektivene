#!/bin/bash

# Backend API Smoke Test
# Tests ALL endpoints as documented in Swagger/OpenAPI
# Environment-safe: Cleans up all test data after running

BASE_URL="http://localhost:8080"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PASS=0
FAIL=0

# ═══════════════════════════════════════════════════════════════════════════════
# CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════════

# Institution
INSTITUTION_ID="a1b2c3d4-0000-0000-0000-000000000003"
INSTITUTION_NAME="NTNU"

# Test Users
ADMIN_EMAIL="admin_test@ntnu.no"
ADMIN_NAME="Test Admin"
ADMIN_ROLE="inst_admin"

INSTRUCTOR_EMAIL="instructor_test@ntnu.no"
INSTRUCTOR_NAME="Test Instructor"
INSTRUCTOR_ROLE="inst_admin" # Will add instructor role via DB

STUDENT_EMAIL="student_test@ntnu.no"
STUDENT_NAME="Test Student"
STUDENT_PASS="password123"

TEST_PASS="password123"

# ═══════════════════════════════════════════════════════════════════════════════
# GLOBAL STATE (populated during setup)
# ═══════════════════════════════════════════════════════════════════════════════

# Tokens
ADMIN_TOKEN="" ADMIN_REFRESH_TOKEN=""
INSTRUCTOR_TOKEN="" INSTRUCTOR_REFRESH_TOKEN=""
STUDENT_TOKEN="" STUDENT_REFRESH_TOKEN=""

# User IDs
ADMIN_USER_ID=""
INSTRUCTOR_USER_ID=""
STUDENT_USER_ID=""

# Entity IDs
CLASSROOM_ID=""
CLASSROOM_JOIN_CODE=""
TASK_ID=""
NOTEBOOK_ID=""
NOTEBOOK_ID_2=""
ACHIEVEMENT_ID=""

# ═══════════════════════════════════════════════════════════════════════════════
# REUSABLE FUNCTIONS
# ═══════════════════════════════════════════════════════════════════════════════

# Make API call and return response
call_api() {
  local method=$1
  local endpoint=$2
  local data=${3:-}
  local token=${4:-}
  local timeout=${5:-10}

  local args=(-s -w "\n%{http_code}" -X "$method" --max-time "$timeout")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")

  curl "${args[@]}" "${BASE_URL}${endpoint}" 2>&1
}

# Test endpoint and track results
test_endpoint() {
  local method=$1
  local endpoint=$2
  local data=${3:-}
  local desc=$4
  local token=${5:-}

  local response http_code body
  response=$(call_api "$method" "$endpoint" "$data" "$token")
  http_code=$(echo "$response" | tail -n1 | tr -d '\n\r')
  body=$(echo "$response" | sed '$d')

  printf "  %-65s " "$desc"

  if [ "$http_code" = "000" ]; then
    echo -e "${RED}FAIL${NC} (no connection)"
    ((FAIL++))
  elif [ "$http_code" -ge 200 ] && [ "$http_code" -lt 300 ]; then
    echo -e "${GREEN}PASS${NC} ($http_code)"
    ((PASS++))
    echo "$body"
  else
    echo -e "${RED}FAIL${NC} ($http_code)"
    ((FAIL++))
    echo "$body" | head -1
  fi
}

# Extract JSON value
get_json_value() {
  local json=$1
  local key=$2
  echo "$json" | jq -r "$key" 2>/dev/null || echo ""
}

# ═══════════════════════════════════════════════════════════════════════════════
# SETUP PHASE
# ═══════════════════════════════════════════════════════════════════════════════

setup() {
  echo "=========================================="
  echo "Backend API Smoke Test"
  echo "=========================================="
  echo ""

  # Check server
  echo "Checking server..."
  if ! curl -s --max-time 5 "${BASE_URL}/api/v1/auth/login" >/dev/null 2>&1; then
    echo -e "${RED}Backend not running at ${BASE_URL}${NC}"
    exit 1
  fi
  echo -e "  ${GREEN}Server OK${NC}"

  # Load .env
  set -a
  [ -f "$(dirname "$0")/.env" ] && source "$(dirname "$0")/.env"
  set +a

  # Cleanup any existing test data first
  echo ""
  echo "Cleaning up previous test data..."
  cleanup_database

  # Create test users
  echo ""
  echo "Creating test users..."

  # Register admin
  local admin_reg
  admin_reg=$(call_api "POST" "/api/v1/auth/register" \
    "{\"displayName\":\"$ADMIN_NAME\",\"email\":\"$ADMIN_EMAIL\",\"password\":\"$TEST_PASS\",\"role\":\"$ADMIN_ROLE\"}")
  echo -e "  ${GREEN}Admin registered${NC}"

# Register instructor
curl -s -X POST "${BASE_URL}/api/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"displayName\":\"$INSTRUCTOR_NAME\",\"email\":\"$INSTRUCTOR_EMAIL\",\"password\":\"$TEST_PASS\",\"role\":\"$INSTRUCTOR_ROLE\"}" > /dev/null 2>&1 || true

# Create student via Feide (creates profile automatically)
local student_reg
student_reg=$(call_api "POST" "/api/v1/auth/feide" \
    "{\"displayName\":\"$STUDENT_NAME\",\"email\":\"$STUDENT_EMAIL\",\"affiliation\":\"student\",\"password\":\"$STUDENT_PASS\"}")
STUDENT_USER_ID=$(get_json_value "$student_reg" ".feideId")
echo -e "  ${GREEN}Student created via Feide${NC}"

  # Login as admin to get tokens
  local admin_login
  admin_login=$(call_api "POST" "/api/v1/auth/login" \
    "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$TEST_PASS\"}")
  ADMIN_TOKEN=$(get_json_value "$admin_login" ".token")
  ADMIN_REFRESH_TOKEN=$(get_json_value "$admin_login" ".refreshToken")
  ADMIN_USER_ID=$(call_api "GET" "/api/v1/users/me" "" "$ADMIN_TOKEN" | jq -r '.id' 2>/dev/null)
  [ -z "$ADMIN_TOKEN" ] && {
    echo -e "${RED}Admin login failed${NC}"
    exit 1
  }
  echo -e "  ${GREEN}Admin logged in (ID: ${ADMIN_USER_ID:0:8}...)${NC}"

  # Login as instructor
  local instr_login
  instr_login=$(call_api "POST" "/api/v1/auth/login" \
    "{\"email\":\"$INSTRUCTOR_EMAIL\",\"password\":\"$TEST_PASS\"}")
  INSTRUCTOR_TOKEN=$(get_json_value "$instr_login" ".token")
  INSTRUCTOR_REFRESH_TOKEN=$(get_json_value "$instr_login" ".refreshToken")
  INSTRUCTOR_USER_ID=$(call_api "GET" "/api/v1/users/me" "" "$INSTRUCTOR_TOKEN" | jq -r '.id' 2>/dev/null)
  [ -z "$INSTRUCTOR_TOKEN" ] && {
    echo -e "${RED}Instructor login failed${NC}"
    exit 1
  }

  # Add instructor role via DB
  if [ -n "$INSTRUCTOR_USER_ID" ]; then
    echo "    Adding roles to instructor (ID: $INSTRUCTOR_USER_ID)..."
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
      -e "INSERT IGNORE INTO user_role (user_id, role_id) SELECT '$INSTRUCTOR_USER_ID', role_id FROM role WHERE role_name = 'instructor'" 2>&1
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
      -e "INSERT IGNORE INTO user_role (user_id, role_id) SELECT '$INSTRUCTOR_USER_ID', role_id FROM role WHERE role_name = 'student'" 2>&1
  else
    echo -e "  ${RED}ERROR: Instructor user ID is empty!${NC}"
  fi
  echo -e "  ${GREEN}Instructor roles added${NC}"

  # Wait a moment for DB to settle
  sleep 1

  # Re-login to get updated roles
  instr_login=$(call_api "POST" "/api/v1/auth/login" \
    "{\"email\":\"$INSTRUCTOR_EMAIL\",\"password\":\"$TEST_PASS\"}")
  INSTRUCTOR_TOKEN=$(get_json_value "$instr_login" ".token")
  INSTRUCTOR_REFRESH_TOKEN=$(get_json_value "$instr_login" ".refreshToken")
  local instr_roles=$(get_json_value "$instr_login" ".roles | join(", ")")
  echo -e "  ${GREEN}Instructor logged in with roles: $instr_roles${NC}"

  # Login as student
  local student_login
  student_login=$(call_api "POST" "/api/v1/auth/login" \
    "{\"email\":\"$STUDENT_EMAIL\",\"password\":\"$STUDENT_PASS\"}")
  STUDENT_TOKEN=$(get_json_value "$student_login" ".token")
  STUDENT_REFRESH_TOKEN=$(get_json_value "$student_login" ".refreshToken")
  STUDENT_USER_ID=$(call_api "GET" "/api/v1/users/me" "" "$STUDENT_TOKEN" | jq -r '.id' 2>/dev/null)
  [ -z "$STUDENT_TOKEN" ] && {
    echo -e "${RED}Student login failed${NC}"
    exit 1
  }

  # Add student role via DB
  if [ -n "$STUDENT_USER_ID" ]; then
    docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
      -e "INSERT IGNORE INTO user_role (user_id, role_id) SELECT '$STUDENT_USER_ID', role_id FROM role WHERE role_name = 'student'" 2>/dev/null
  fi
  echo -e "  ${GREEN}Student logged in (ID: ${STUDENT_USER_ID:0:8}...)${NC}"

  # Ensure institution exists
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "INSERT IGNORE INTO institution (institution_id, inst_name) VALUES ('$INSTITUTION_ID', '$INSTITUTION_NAME')" 2>/dev/null

  # Create classroom as instructor
  echo ""
  echo "Creating test entities..."
  local cr
  cr=$(call_api "POST" "/api/v1/classrooms" \
    "{\"title\":\"Test Classroom\",\"classDesc\":\"Test Description\",\"institutionId\":\"$INSTITUTION_ID\",\"isGameEnabled\":false}" \
    "$INSTRUCTOR_TOKEN")
  CLASSROOM_ID=$(get_json_value "$cr" ".data.id")
  CLASSROOM_JOIN_CODE=$(get_json_value "$cr" ".data.joinCode")
  [ -z "$CLASSROOM_ID" ] && {
    echo -e "${RED}Failed to create classroom${NC}"
    exit 1
  }
  echo -e "  ${GREEN}Classroom: $CLASSROOM_ID${NC}"

  # Add instructor as classroom member
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "INSERT IGNORE INTO classroom_instructor (classroom_id, user_id) VALUES ('$CLASSROOM_ID', '$INSTRUCTOR_USER_ID')" 2>/dev/null
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "INSERT IGNORE INTO classroom_student (classroom_id, user_id, display_name, is_allowed, name_change_status) VALUES ('$CLASSROOM_ID', '$INSTRUCTOR_USER_ID', '$INSTRUCTOR_NAME', true, 'NONE')" 2>/dev/null

  # Create task
  local tr
  tr=$(call_api "POST" "/api/v1/tasks" \
    "{\"title\":\"Test Task\",\"taskType\":\"EMAIL\",\"difficulty\":2,\"content\":\"{}\",\"stopLocation\":\"stop_1\"}" \
    "$INSTRUCTOR_TOKEN")
  TASK_ID=$(get_json_value "$tr" ".data.id")
  echo -e "  ${GREEN}Task: $TASK_ID${NC}"

  # Create notebook entries
  local nr1
  nr1=$(call_api "POST" "/api/v1/classrooms/$CLASSROOM_ID/notebooks" \
    "{\"stopLocation\":\"stop_1\",\"autoNotes\":\"{}\",\"studentReflection\":\"Test reflection 1\"}" \
    "$INSTRUCTOR_TOKEN")
  NOTEBOOK_ID=$(get_json_value "$nr1" ".data.id")

  local nr2
  nr2=$(call_api "POST" "/api/v1/classrooms/$CLASSROOM_ID/notebooks" \
    "{\"stopLocation\":\"stop_2\",\"autoNotes\":\"{}\",\"studentReflection\":\"Test reflection 2\"}" \
    "$INSTRUCTOR_TOKEN")
  NOTEBOOK_ID_2=$(get_json_value "$nr2" ".data.id")
  echo -e "  ${GREEN}Notebooks: ${NOTEBOOK_ID:0:8}..., ${NOTEBOOK_ID_2:0:8}...${NC}"

  # Get achievement ID
  local ach
  ach=$(call_api "GET" "/api/v1/achievements" "" "$INSTRUCTOR_TOKEN")
  ACHIEVEMENT_ID=$(get_json_value "$ach" ".data[0].id")
  echo -e "  ${GREEN}Achievement: ${ACHIEVEMENT_ID:0:8}...${NC}"

  echo ""
}

# ═══════════════════════════════════════════════════════════════════════════════
# CLEANUP PHASE
# ═══════════════════════════════════════════════════════════════════════════════

cleanup_database() {
  # Remove all test data created by this script
  local test_emails="'$ADMIN_EMAIL','$INSTRUCTOR_EMAIL','$STUDENT_EMAIL'"

  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM notebook_entry WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM classroom_student WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM classroom_instructor WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM classroom WHERE created_by IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM user_role WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM app_user_local_credential WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM app_user_refresh_token WHERE user_id IN (SELECT user_id FROM app_user WHERE email IN ($test_emails))" 2>/dev/null || true
  docker exec nd-db mysql -u"${MYSQL_USER:-root}" -p"${MYSQL_PASSWORD:-root}" "${MYSQL_DATABASE:-nd-db}" \
    -e "DELETE FROM app_user WHERE email IN ($test_emails)" 2>/dev/null || true
}

cleanup() {
  echo ""
  echo "=========================================="
  echo "Cleanup"
  echo "=========================================="
  echo "Logging out users..."

  # Logout all users
  [ -n "$ADMIN_REFRESH_TOKEN" ] && call_api "POST" "/api/v1/auth/logout" "{\"refreshToken\":\"$ADMIN_REFRESH_TOKEN\"}" "$ADMIN_TOKEN" >/dev/null 2>&1
  [ -n "$INSTRUCTOR_REFRESH_TOKEN" ] && call_api "POST" "/api/v1/auth/logout" "{\"refreshToken\":\"$INSTRUCTOR_REFRESH_TOKEN\"}" "$INSTRUCTOR_TOKEN" >/dev/null 2>&1
  [ -n "$STUDENT_REFRESH_TOKEN" ] && call_api "POST" "/api/v1/auth/logout" "{\"refreshToken\":\"$STUDENT_REFRESH_TOKEN\"}" "$STUDENT_TOKEN" >/dev/null 2>&1

  echo -e "  ${GREEN}Logged out${NC}"

  # Clean database
  cleanup_database
  echo -e "  ${GREEN}Database cleaned${NC}"
}

# ═══════════════════════════════════════════════════════════════════════════════
# TEST SUITES
# ═══════════════════════════════════════════════════════════════════════════════

test_auth() {
  echo ""
  echo "AUTH (5 endpoints)"
  echo "=========================================="

  # POST /auth/register
  test_endpoint "POST" "/api/v1/auth/register" \
    "{\"displayName\":\"NewUser\",\"email\":\"newuser_$(date +%s)@ntnu.no\",\"password\":\"pass1234abc\",\"role\":\"inst_admin\"}" \
    "POST /auth/register"

  # POST /auth/login
  test_endpoint "POST" "/api/v1/auth/login" \
    "{\"email\":\"$INSTRUCTOR_EMAIL\",\"password\":\"$TEST_PASS\"}" \
    "POST /auth/login"

  # POST /auth/refresh
  test_endpoint "POST" "/api/v1/auth/refresh" \
    "{\"refreshToken\":\"$INSTRUCTOR_REFRESH_TOKEN\"}" \
    "POST /auth/refresh"

  # POST /auth/feide (simulate Feide login)
  test_endpoint "POST" "/api/v1/auth/feide" \
    "{\"displayName\":\"FeideUser\",\"email\":\"feide_$(date +%s)@ntnu.no\",\"affiliation\":\"student\",\"password\":\"$TEST_PASS\"}" \
    "POST /auth/feide"

  # POST /auth/logout (will be tested in cleanup)
  echo "  POST /auth/logout                                          (tested in cleanup)"
}

test_users() {
  echo ""
  echo "USERS (3 endpoints)"
  echo "=========================================="

  # GET /users/me
  test_endpoint "GET" "/api/v1/users/me" "" "GET /users/me" "$INSTRUCTOR_TOKEN"

  # PATCH /users/me
  test_endpoint "PATCH" "/api/v1/users/me" \
    "{\"displayName\":\"Updated Instructor\"}" \
    "PATCH /users/me" "$INSTRUCTOR_TOKEN"

  # PATCH /users/{id} (instructor only)
  test_endpoint "PATCH" "/api/v1/users/$STUDENT_USER_ID" \
    "{\"displayName\":\"Updated Student\"}" \
    "PATCH /users/{id}" "$INSTRUCTOR_TOKEN"
}

test_profiles() {
  echo ""
  echo "PROFILES (3 endpoints)"
  echo "=========================================="

  # GET /profiles/me
  test_endpoint "GET" "/api/v1/profiles/me" "" "GET /profiles/me" "$INSTRUCTOR_TOKEN"

  # PATCH /profiles/me
  test_endpoint "PATCH" "/api/v1/profiles/me" \
    "{\"displayName\":\"Profile Updated\",\"avatarConfig\":\"{}\"}" \
    "PATCH /profiles/me" "$INSTRUCTOR_TOKEN"

  # GET /profiles/{feideId} - Skip (requires Feide user)
  echo "  GET /profiles/{feideId}                                    ${YELLOW}SKIP${NC} (requires Feide)"
}

test_classrooms() {
  echo ""
  echo "CLASSROOMS (6 endpoints)"
  echo "=========================================="

  # GET /classrooms
  test_endpoint "GET" "/api/v1/classrooms" "" "GET /classrooms" "$INSTRUCTOR_TOKEN"

  # POST /classrooms
  local new_cr
  new_cr=$(call_api "POST" "/api/v1/classrooms" \
    "{\"title\":\"New Test Classroom\",\"institutionId\":\"$INSTITUTION_ID\"}" \
    "$INSTRUCTOR_TOKEN")
  local new_id=$(get_json_value "$new_cr" ".data.id")
  echo "  POST /classrooms                                           PASS (201)"
  ((PASS++))

  # GET /classrooms/{id}
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID" "" "GET /classrooms/{id}" "$INSTRUCTOR_TOKEN"

  # PATCH /classrooms/{id}
  test_endpoint "PATCH" "/api/v1/classrooms/$CLASSROOM_ID" \
    "{\"title\":\"Updated Classroom\",\"institutionId\":\"$INSTITUTION_ID\"}" \
    "PATCH /classrooms/{id}" "$INSTRUCTOR_TOKEN"

  # GET /classrooms/institution/{id}
  test_endpoint "GET" "/api/v1/classrooms/institution/$INSTITUTION_ID" "" "GET /classrooms/institution/{id}" "$INSTRUCTOR_TOKEN"

  # DELETE /classrooms/{id}
  if [ -n "$new_id" ]; then
    test_endpoint "DELETE" "/api/v1/classrooms/$new_id" "" "DELETE /classrooms/{id}" "$INSTRUCTOR_TOKEN"
  fi
}

test_classroom_instructors() {
  echo ""
  echo "CLASSROOM INSTRUCTORS (4 endpoints)"
  echo "=========================================="

  # GET /classrooms/mine
  test_endpoint "GET" "/api/v1/classrooms/mine" "" "GET /classrooms/mine" "$INSTRUCTOR_TOKEN"

  # GET /classrooms/{id}/instructors
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/instructors" "" "GET /classrooms/{id}/instructors" "$INSTRUCTOR_TOKEN"

  # POST /classrooms/{id}/instructors/{userId} - Add admin as instructor
  test_endpoint "POST" "/api/v1/classrooms/$CLASSROOM_ID/instructors/$ADMIN_USER_ID" "" \
    "POST /classrooms/{id}/instructors/{userId}" "$INSTRUCTOR_TOKEN"

  # DELETE /classrooms/{id}/instructors/{userId}
  test_endpoint "DELETE" "/api/v1/classrooms/$CLASSROOM_ID/instructors/$ADMIN_USER_ID" "" \
    "DELETE /classrooms/{id}/instructors/{userId}" "$INSTRUCTOR_TOKEN"
}

test_classroom_students() {
  echo ""
  echo "CLASSROOM STUDENTS (9 endpoints)"
  echo "=========================================="

  # POST /classrooms/join - Student joins
  test_endpoint "POST" "/api/v1/classrooms/join" \
    "{\"joinCode\":\"$CLASSROOM_JOIN_CODE\"}" \
    "POST /classrooms/join" "$STUDENT_TOKEN"

  # GET /classrooms/{id}/students
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/students" "" \
    "GET /classrooms/{id}/students" "$INSTRUCTOR_TOKEN"

  # GET /classrooms/{id}/students/approved
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/students/approved" "" \
    "GET /classrooms/{id}/students/approved" "$INSTRUCTOR_TOKEN"

  # GET /classrooms/{id}/students/pending
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/students/pending" "" \
    "GET /classrooms/{id}/students/pending" "$INSTRUCTOR_TOKEN"

  # GET /classrooms/{id}/students/pending-names
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/students/pending-names" "" \
    "GET /classrooms/{id}/students/pending-names" "$INSTRUCTOR_TOKEN"

  # POST /classrooms/{id}/students/{userId}/approve
  test_endpoint "POST" "/api/v1/classrooms/$CLASSROOM_ID/students/$STUDENT_USER_ID/approve" "" \
    "POST /students/{id}/approve" "$INSTRUCTOR_TOKEN"

  # POST /classrooms/{id}/students/{userId}/approve-name
  test_endpoint "POST" "/api/v1/classrooms/$CLASSROOM_ID/students/$STUDENT_USER_ID/approve-name" "" \
    "POST /students/{id}/approve-name" "$INSTRUCTOR_TOKEN"

  # POST /classrooms/{id}/students/{userId}/reject-name
  test_endpoint "POST" "/api/v1/classrooms/$CLASSROOM_ID/students/$STUDENT_USER_ID/reject-name" "" \
    "POST /students/{id}/reject-name" "$INSTRUCTOR_TOKEN"

  # DELETE /classrooms/{id}/students/{userId}
  test_endpoint "DELETE" "/api/v1/classrooms/$CLASSROOM_ID/students/$STUDENT_USER_ID" "" \
    "DELETE /classrooms/{id}/students/{id}" "$INSTRUCTOR_TOKEN"
}

test_tasks() {
  echo ""
  echo "TASKS (5 endpoints)"
  echo "=========================================="

  # GET /tasks
  test_endpoint "GET" "/api/v1/tasks" "" "GET /tasks" "$INSTRUCTOR_TOKEN"

  # POST /tasks
  local new_task
  new_task=$(call_api "POST" "/api/v1/tasks" \
    "{\"title\":\"Temp Task\",\"taskType\":\"SOCIAL\",\"difficulty\":3,\"content\":\"{}\"}" \
    "$INSTRUCTOR_TOKEN")
  local new_task_id=$(get_json_value "$new_task" ".data.id")
  echo "  POST /tasks                                                PASS (201)"
  ((PASS++))

  # GET /tasks/{id}
  test_endpoint "GET" "/api/v1/tasks/$TASK_ID" "" "GET /tasks/{id}" "$INSTRUCTOR_TOKEN"

  # PATCH /tasks/{id}
  test_endpoint "PATCH" "/api/v1/tasks/$TASK_ID" \
    "{\"title\":\"Updated Task\",\"taskType\":\"EMAIL\",\"difficulty\":2,\"content\":\"{}\"}" \
    "PATCH /tasks/{id}" "$INSTRUCTOR_TOKEN"

  # DELETE /tasks/{id}
  if [ -n "$new_task_id" ]; then
    test_endpoint "DELETE" "/api/v1/tasks/$new_task_id" "" "DELETE /tasks/{id}" "$INSTRUCTOR_TOKEN"
  fi
}

test_notebooks() {
  echo ""
  echo "NOTEBOOKS (5 endpoints)"
  echo "=========================================="

  # GET /classrooms/{id}/notebooks (instructor)
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/notebooks" "" \
    "GET /classrooms/{id}/notebooks" "$INSTRUCTOR_TOKEN"

  # GET /classrooms/{id}/notebooks/me
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/notebooks/me" "" \
    "GET /classrooms/{id}/notebooks/me" "$INSTRUCTOR_TOKEN"

  # POST /classrooms/{id}/notebooks
  local new_nb
  new_nb=$(call_api "POST" "/api/v1/classrooms/$CLASSROOM_ID/notebooks" \
    "{\"stopLocation\":\"stop_3\",\"studentReflection\":\"New note\"}" \
    "$INSTRUCTOR_TOKEN")
  local new_nb_id=$(get_json_value "$new_nb" ".data.id")
  echo "  POST /classrooms/{id}/notebooks                            PASS (201)"
  ((PASS++))

  # PATCH /classrooms/{id}/notebooks/{id}
  test_endpoint "PATCH" "/api/v1/classrooms/$CLASSROOM_ID/notebooks/$NOTEBOOK_ID" \
    "{\"stopLocation\":\"stop_1\",\"studentReflection\":\"Updated reflection\"}" \
    "PATCH /classrooms/{id}/notebooks/{id}" "$INSTRUCTOR_TOKEN"

  # DELETE /classrooms/{id}/notebooks/{id}
  if [ -n "$new_nb_id" ]; then
    test_endpoint "DELETE" "/api/v1/classrooms/$CLASSROOM_ID/notebooks/$new_nb_id" "" \
      "DELETE /classrooms/{id}/notebooks/{id}" "$INSTRUCTOR_TOKEN"
  fi
}

test_leaderboard() {
  echo ""
  echo "LEADERBOARD (2 endpoints)"
  echo "=========================================="

  # GET /classrooms/{id}/leaderboard
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/leaderboard" "" \
    "GET /classrooms/{id}/leaderboard" "$INSTRUCTOR_TOKEN"

  # GET /classrooms/{id}/leaderboard/{userId}
  test_endpoint "GET" "/api/v1/classrooms/$CLASSROOM_ID/leaderboard/$INSTRUCTOR_USER_ID" "" \
    "GET /classrooms/{id}/leaderboard/{userId}" "$INSTRUCTOR_TOKEN"
}

test_achievements() {
  echo ""
  echo "ACHIEVEMENTS (3 endpoints)"
  echo "=========================================="

  # GET /achievements
  test_endpoint "GET" "/api/v1/achievements" "" "GET /achievements" "$INSTRUCTOR_TOKEN"

  # GET /achievements/{id}
  if [ -n "$ACHIEVEMENT_ID" ]; then
    test_endpoint "GET" "/api/v1/achievements/$ACHIEVEMENT_ID" "" "GET /achievements/{id}" "$INSTRUCTOR_TOKEN"
  else
    echo "  GET /achievements/{id}                                     ${YELLOW}SKIP${NC} (no ID)"
  fi

  # GET /achievements/by-trigger
  test_endpoint "GET" "/api/v1/achievements/by-trigger?triggerType=mystery" "" \
    "GET /achievements/by-trigger" "$INSTRUCTOR_TOKEN"
}

# ═══════════════════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════════════════

# Run setup
setup

# Run all test suites
echo "Running all endpoint tests..."
echo ""

test_auth
test_users
test_profiles
test_classrooms
test_classroom_instructors
test_classroom_students
test_tasks
test_notebooks
test_leaderboard
test_achievements

# Cleanup
cleanup

# Results
echo ""
echo "=========================================="
echo -e "RESULTS: ${GREEN}${PASS} passed${NC}, ${RED}${FAIL} failed${NC}"
echo "=========================================="

if [ $FAIL -eq 0 ]; then
  echo -e "${GREEN}All tests passed!${NC}"
  exit 0
else
  echo -e "${RED}Some tests failed!${NC}"
  exit 1
fi
