#!/bin/bash

# Uploads an image through POST /api/v1/tasks and verifies the saved path is reachable.
# Usage:
#   ./scripts/test-task-image-upload.sh [email] [password] [image_path]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL="${1:-instructor@test.no}"
PASSWORD="${2:-password123}"
IMAGE_PATH_INPUT="${3:-frontend/public/news/real-article-1.png}"

if [[ "$IMAGE_PATH_INPUT" = /* ]]; then
  IMAGE_PATH="$IMAGE_PATH_INPUT"
else
  IMAGE_PATH="${REPO_ROOT}/${IMAGE_PATH_INPUT}"
fi

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

TASK_ID=""
TASK_JSON_FILE=""

cleanup() {
  if [ -n "$TASK_ID" ] && [ -n "$TOKEN" ]; then
    curl -s -X DELETE \
      -H "Authorization: Bearer $TOKEN" \
      "${BASE_URL}/api/v1/tasks/${TASK_ID}" >/dev/null 2>&1 || true
  fi
  if [ -n "$TASK_JSON_FILE" ] && [ -f "$TASK_JSON_FILE" ]; then
    rm -f "$TASK_JSON_FILE"
  fi
}

trap cleanup EXIT

fail() {
  echo -e "${RED}[FAIL]${NC} $1"
  exit 1
}

info() {
  echo -e "${BLUE}[INFO]${NC} $1"
}

pass() {
  echo -e "${GREEN}[OK]${NC} $1"
}

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    fail "Missing required command: $1"
  fi
}

check_server() {
  if ! curl -s --max-time 5 "${BASE_URL}/api/v1/tasks" >/dev/null 2>&1; then
    fail "Backend not running at ${BASE_URL}"
  fi
}

login() {
  info "Logging in as ${EMAIL}..."
  local response
  response=$(curl -s -X POST \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}" \
    "${BASE_URL}/api/v1/auth/login")

  TOKEN=$(echo "$response" | jq -r '.data.token // .token // empty')
  if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "$response" | jq . 2>/dev/null || echo "$response"
    fail "Login failed"
  fi

  local roles
  roles=$(echo "$response" | jq -r '.data.roles // .roles // [] | join(", ")')
  pass "Login succeeded"
  [ -n "$roles" ] && info "Roles: ${roles}"
}

create_task_with_upload() {
  local task_json
  task_json=$(jq -cn \
    --arg title "Upload image test $(date +%s)" \
    --arg taskType "NEWS" \
    --arg stopLocation "nyhetskvartalet" \
    --arg content "$(jq -cn --arg imageRef "upload://$(basename "$IMAGE_PATH")" '{
      question: "Hvilken artikkel er ekte?",
      hint: "Test upload",
      articles: [
        { imagePath: $imageRef, isReal: true },
        { imagePath: $imageRef, isReal: false }
      ]
    }')" \
    '{
      title: $title,
      taskType: $taskType,
      stopLocation: $stopLocation,
      difficulty: 1,
      content: $content
    }')

  TASK_JSON_FILE=$(mktemp)
  printf '%s' "$task_json" > "$TASK_JSON_FILE"

  info "Creating task with multipart image upload..."
  local response body status
  response=$(curl -s -X POST \
    -w "\n%{http_code}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -F "task=@${TASK_JSON_FILE};type=application/json" \
    -F "images=@${IMAGE_PATH}" \
    "${BASE_URL}/api/v1/tasks")

  status=$(echo "$response" | tail -n1 | tr -d '\r')
  body=$(echo "$response" | sed '$d')

  TASK_ID=$(echo "$body" | jq -r '.data.id // empty')
  SAVED_CONTENT=$(echo "$body" | jq -r '.data.content // empty')

  if [ -z "$TASK_ID" ] || [ "$TASK_ID" = "null" ]; then
    echo -e "${YELLOW}[DEBUG]${NC} HTTP status: ${status}"
    echo "$body" | jq . 2>/dev/null || echo "$body"
    fail "Task creation failed"
  fi

  pass "Task created: ${TASK_ID}"
}

verify_saved_path() {
  info "Reading saved image path from task content..."

  SAVED_IMAGE_PATH=$(echo "$SAVED_CONTENT" | jq -r '.articles[0].imagePath // empty')
  if [ -z "$SAVED_IMAGE_PATH" ] || [ "$SAVED_IMAGE_PATH" = "null" ]; then
    echo "$SAVED_CONTENT" | jq . 2>/dev/null || echo "$SAVED_CONTENT"
    fail "Task content does not contain a saved imagePath"
  fi

  if [[ "$SAVED_IMAGE_PATH" != /uploads/* ]]; then
    echo -e "${YELLOW}[WARN]${NC} Saved path is not under /uploads: ${SAVED_IMAGE_PATH}"
  fi

  pass "Saved image path: ${SAVED_IMAGE_PATH}"
}

verify_uploaded_asset() {
  info "Requesting uploaded image URL..."

  local headers body status content_type
  headers=$(mktemp)
  body=$(mktemp)

  curl -s -D "$headers" -o "$body" "${BASE_URL}${SAVED_IMAGE_PATH}"
  status=$(awk 'NR==1 {print $2}' "$headers")
  content_type=$(awk 'BEGIN{IGNORECASE=1} /^Content-Type:/ {print $2}' "$headers" | tr -d '\r')

  if [ "$status" != "200" ]; then
    rm -f "$headers" "$body"
    fail "Uploaded file URL returned HTTP ${status:-unknown}"
  fi

  if [ ! -s "$body" ]; then
    rm -f "$headers" "$body"
    fail "Uploaded file response body was empty"
  fi

  pass "Uploaded file is reachable (HTTP 200, Content-Type: ${content_type:-unknown})"
  rm -f "$headers" "$body"
}

require_command curl
require_command jq

[ -f "$IMAGE_PATH" ] || fail "Image file not found: $IMAGE_PATH"

echo "========================================="
echo "Task Image Upload Test"
echo "========================================="
echo "Base URL: ${BASE_URL}"
echo "Image:    ${IMAGE_PATH}"
echo ""

check_server
login
create_task_with_upload
verify_saved_path
verify_uploaded_asset

echo ""
pass "Upload test completed successfully"
