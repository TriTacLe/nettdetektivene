#!/usr/bin/env bash
# factory.sh
#
# Single seed script. Idempotent. Replaces the old scripts/testdata/* and
# the seed-related root scripts (setup-classroom, create-all-tasks, enable-game,
# unlock-first-task, cleanup-fotografen-tasks, deactivate-tasks).
#
# Usage:
#   bash scripts/factory.sh

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
DB_CONTAINER="${DB_CONTAINER:-nd-db}"
MYSQL_DB="${MYSQL_DATABASE:-nd-db}"
MYSQL_ROOT_PW="${MYSQL_ROOT_PASSWORD:-root}"

INSTITUTION_ID="aaaaaaaa-0000-0000-0000-000000000001"
INSTITUTION_NAME="NTNU"
PASSWORD="password123"
CLASSROOM_TITLE="Testklasse 1"

# Local source for task images. Files here are uploaded to the backend
# via the multipart task endpoint so created tasks reference
# /uploads/tasks/<batchId>/... URLs served by the backend, not direct
# paths into the frontend's public/ directory.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ASSETS_ROOT="${ASSETS_ROOT:-${REPO_ROOT}/frontend/public}"
NEWS_FAKE_1="${ASSETS_ROOT}/news/fake-article-1.jpg"
NEWS_REAL_1="${ASSETS_ROOT}/news/real-article-1.png"
PHOTO_REAL_1="${ASSETS_ROOT}/photos/real-photo-1.jpeg"
PHOTO_FAKE_1="${ASSETS_ROOT}/photos/fake-photo-1.jpg"

GREEN='\033[0;32m'; BLUE='\033[0;34m'; RED='\033[0;31m'; YELLOW='\033[0;33m'; NC='\033[0m'
# All logging goes to stderr so command-substitution (id capture) is clean.
info()  { echo -e "${BLUE}[INFO]${NC} $*" >&2; }
ok()    { echo -e "  ${GREEN}OK${NC}   $*" >&2; }
skip()  { echo -e "  ${YELLOW}SKIP${NC} $*" >&2; }
fail()  { echo -e "  ${RED}FAIL${NC} $*" >&2; }
die()   { echo -e "${RED}[FATAL]${NC} $*" >&2; exit 1; }

command -v jq    >/dev/null || die "jq is required"
command -v curl  >/dev/null || die "curl is required"
command -v docker>/dev/null || die "docker is required"

api() {
  local method=$1 endpoint=$2 data=${3:-} token=${4:-}
  local args=(-s -w "\n%{http_code}" -X "$method")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data"  ] && args+=(-H "Content-Type: application/json" -d "$data")
  curl "${args[@]}" "${BASE_URL}${endpoint}"
}

http_code() { tail -n1 <<<"$1"; }
http_body() { sed '$d' <<<"$1"; }

wait_for_backend() {
  info "Waiting for backend at ${BASE_URL}..."
  local retries=60
  until curl -s --max-time 2 "${BASE_URL}/api/v1/auth/login" >/dev/null 2>&1; do
    retries=$((retries-1))
    [ $retries -le 0 ] && die "Backend not reachable"
    sleep 1
  done
  ok "Backend reachable"
}

mysql_exec() {
  docker exec "$DB_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PW" "$MYSQL_DB" -e "$1" 2>/dev/null
}

seed_institution() {
  info "Seeding institution '${INSTITUTION_NAME}'..."
  mysql_exec "INSERT IGNORE INTO institution (institution_id, inst_name) VALUES ('${INSTITUTION_ID}', '${INSTITUTION_NAME}');"
  ok "Institution ready"
}

# Create-or-login via simulated Feide. Returns token on stdout.
feide_login() {
  local email=$1 affiliation=$2 display_name=$3
  local body
  body=$(jq -n --arg email "$email" --arg pw "$PASSWORD" --arg aff "$affiliation" --arg dn "$display_name" \
    '{email:$email, password:$pw, affiliation:$aff, displayName:$dn}')
  local resp; resp=$(api POST /api/v1/auth/feide "$body")
  local code; code=$(http_code "$resp")
  if [ "$code" != "200" ] && [ "$code" != "201" ]; then
    fail "Feide login for $email failed ($code): $(http_body "$resp" | head -c 200)"
    return 1
  fi
  http_body "$resp" | jq -r '.token'
}

grant_role() {
  local user_id=$1 role=$2
  mysql_exec "INSERT IGNORE INTO user_role (user_id, role_id) SELECT '${user_id}', role_id FROM role WHERE role_name = '${role}';"
}

user_id_from_token() {
  local token=$1
  local resp; resp=$(api GET /api/v1/users/me "" "$token")
  http_body "$resp" | jq -r '.id // .data.id // empty'
}

seed_users() {
  info "Seeding users..."

  # Default teacher (matches TeacherLoginView default).
  local laerer_token; laerer_token=$(feide_login "laerer@ntnu.no" "instructor" "Lærer Lærersen")
  local laerer_id;    laerer_id=$(user_id_from_token "$laerer_token")
  grant_role "$laerer_id" "instructor"
  ok "instructor laerer@ntnu.no"

  # Extra accounts for legacy scripts/tests.
  local inst_token; inst_token=$(feide_login "instructor@test.no" "instructor" "Test Instructor")
  local inst_id;    inst_id=$(user_id_from_token "$inst_token")
  grant_role "$inst_id" "instructor"
  ok "instructor instructor@test.no"

  local admin_token; admin_token=$(feide_login "admin@test.no" "instructor" "Test Admin")
  local admin_id;    admin_id=$(user_id_from_token "$admin_token")
  grant_role "$admin_id" "instructor"
  grant_role "$admin_id" "admin"
  ok "admin admin@test.no"

  # Students.
  for n in 1 2 3 4 5; do
    local s_token; s_token=$(feide_login "student${n}@test.no" "student" "Student ${n}")
    local s_id;    s_id=$(user_id_from_token "$s_token")
    grant_role "$s_id" "student"
    ok "student student${n}@test.no"
  done

  echo "$laerer_token" > /tmp/factory_laerer_token
}

ensure_classroom() {
  local token; token=$(cat /tmp/factory_laerer_token)
  info "Ensuring classroom '${CLASSROOM_TITLE}'..."

  local resp; resp=$(api GET /api/v1/classrooms "" "$token")
  local body; body=$(http_body "$resp")
  local existing_id; existing_id=$(echo "$body" | jq -r --arg t "$CLASSROOM_TITLE" '.data[]? | select(.title==$t) | .id' | head -n1)

  if [ -n "$existing_id" ] && [ "$existing_id" != "null" ]; then
    skip "Classroom already exists (${existing_id:0:8}...)"
    local jc; jc=$(echo "$body" | jq -r --arg t "$CLASSROOM_TITLE" '.data[]? | select(.title==$t) | .joinCode' | head -n1)
    echo "$existing_id" > /tmp/factory_classroom_id
    echo "$jc"          > /tmp/factory_join_code
    return 0
  fi

  local payload
  payload=$(jq -n --arg title "$CLASSROOM_TITLE" --arg inst "$INSTITUTION_ID" \
    '{title:$title, classDesc:"Auto-seeded test classroom", institutionId:$inst, gameEnabled:true}')
  resp=$(api POST /api/v1/classrooms "$payload" "$token")
  local code; code=$(http_code "$resp"); body=$(http_body "$resp")
  if [ "$code" != "200" ] && [ "$code" != "201" ]; then
    die "Could not create classroom ($code): $(echo "$body" | head -c 200)"
  fi
  local cid; cid=$(echo "$body" | jq -r '.data.id')
  local jc;  jc=$(echo "$body" | jq -r '.data.joinCode')
  ok "Classroom created (${cid:0:8}..., join code $jc)"
  echo "$cid" > /tmp/factory_classroom_id
  echo "$jc"  > /tmp/factory_join_code
}

join_students() {
  local jc; jc=$(cat /tmp/factory_join_code)
  info "Joining students with code ${jc}..."
  for n in 1 2 3 4 5; do
    local email="student${n}@test.no"
    local stu_token; stu_token=$(feide_login "$email" "student" "Student ${n}")

    # If student already has a membership, skip — backend rejects multi-classroom.
    local m_resp; m_resp=$(api GET /api/v1/classrooms/me "" "$stu_token")
    local m_code; m_code=$(http_code "$m_resp")
    if [ "$m_code" = "200" ]; then
      skip "$email already in a classroom"
      continue
    fi

    local resp; resp=$(api POST /api/v1/classrooms/join "{\"joinCode\":\"$jc\"}" "$stu_token")
    local code; code=$(http_code "$resp")
    local body; body=$(http_body "$resp")
    if [[ "$code" =~ ^20[01]$ ]]; then
      ok "$email joined"
    else
      fail "$email join failed ($code): $(echo "$body" | head -c 160)"
    fi
  done
}

# Teacher approves each student so they become full members (isAllowed=1).
# Required before students can post mysteries / notebook entries.
approve_students() {
  local token; token=$(cat /tmp/factory_laerer_token)
  local cid;   cid=$(cat /tmp/factory_classroom_id)
  info "Approving pending students..."
  for n in 1 2 3 4 5; do
    local email="student${n}@test.no"
    local stu_token; stu_token=$(feide_login "$email" "student" "Student ${n}")
    local stu_id;    stu_id=$(user_id_from_token "$stu_token")
    if [ -z "$stu_id" ] || [ "$stu_id" = "null" ]; then
      fail "Could not resolve user id for $email"
      continue
    fi
    local resp; resp=$(api POST "/api/v1/classrooms/${cid}/students/${stu_id}/approve" "" "$token")
    local code; code=$(http_code "$resp")
    local body; body=$(http_body "$resp")
    if [[ "$code" =~ ^20[01]$ ]]; then
      ok "Approved $email"
    elif [ "$code" = "409" ] || echo "$body" | grep -qi "already"; then
      skip "$email already approved"
    else
      fail "Approve $email failed ($code): $(echo "$body" | head -c 160)"
    fi
  done
}

create_task_if_missing() {
  local token=$1 title=$2 type=$3 stop=$4 difficulty=$5 content=$6
  shift 6
  # Remaining args are absolute paths to image files referenced from
  # $content via "upload://<filename>". They are uploaded as multipart
  # parts so the backend stores them under /uploads/tasks/<batchId>/...
  # and rewrites the content paths accordingly.
  local image_paths=("$@")

  local list; list=$(http_body "$(api GET /api/v1/tasks "" "$token")")
  local existing; existing=$(echo "$list" | jq -r --arg t "$title" '(.data // .) | (if type=="array" then . else (.[] // []) end) | .[]? | select(.title==$t) | .id' | head -n1)
  if [ -n "$existing" ] && [ "$existing" != "null" ]; then
    skip "Task '$title' exists"
    echo "$existing"
    return
  fi

  local payload
  payload=$(jq -n \
    --arg title "$title" --arg type "$type" --arg stop "$stop" \
    --arg content "$content" --argjson difficulty "$difficulty" \
    '{title:$title, taskType:$type, difficulty:$difficulty, isActive:true, stopLocation:$stop, content:$content}')

  local resp code body
  if [ ${#image_paths[@]} -eq 0 ]; then
    resp=$(api POST /api/v1/tasks "$payload" "$token")
  else
    # Multipart upload: backend expects a "task" JSON part and one or
    # more "images" file parts whose original filenames match the
    # upload://<filename> references in content.
    local task_json; task_json=$(mktemp -t factory_task.XXXXXX.json)
    printf '%s' "$payload" > "$task_json"

    local curl_args=(-s -w "\n%{http_code}" -X POST
      -H "Authorization: Bearer $token"
      -F "task=@${task_json};type=application/json")

    # Deduplicate so a filename referenced multiple times is uploaded once.
    local seen=""
    local img
    for img in "${image_paths[@]}"; do
      [ -z "$img" ] && continue
      if [ ! -f "$img" ]; then
        rm -f "$task_json"
        fail "Task '$title' image not found: $img"
        return 1
      fi
      local base; base=$(basename "$img")
      case ",$seen," in
        *",$base,"*) continue ;;
      esac
      seen="$seen,$base"
      curl_args+=(-F "images=@${img}")
    done

    resp=$(curl "${curl_args[@]}" "${BASE_URL}/api/v1/tasks")
    rm -f "$task_json"
  fi

  code=$(http_code "$resp"); body=$(http_body "$resp")
  if [ "$code" != "200" ] && [ "$code" != "201" ]; then
    fail "Task '$title' create failed ($code): $(echo "$body" | head -c 200)"
    return 1
  fi
  local id; id=$(echo "$body" | jq -r '.data.id // .id')
  ok "Task created: $title (${id:0:8}...)"
  echo "$id"
}

attach_task_to_classroom() {
  local token=$1 classroom_id=$2 task_id=$3 sort_order=$4
  if [ -z "$task_id" ] || [ "$task_id" = "null" ]; then
    fail "Skip link: empty task id"
    return
  fi
  local payload; payload=$(jq -n --arg tid "$task_id" --argjson so "$sort_order" '{taskId:$tid, sortOrder:$so, isActive:true}')
  local resp; resp=$(api POST "/api/v1/classrooms/${classroom_id}/tasks" "$payload" "$token")
  local code; code=$(http_code "$resp")
  local body; body=$(http_body "$resp")
  if [[ "$code" =~ ^20[01]$ ]]; then
    ok "Linked task ${task_id:0:8}... to classroom"
  elif [ "$code" = "409" ] || echo "$body" | grep -qi "already"; then
    skip "Task ${task_id:0:8}... already linked"
  else
    fail "Link failed ($code): $(echo "$body" | head -c 160)"
  fi
}

seed_tasks() {
  local token; token=$(cat /tmp/factory_laerer_token)
  local cid;   cid=$(cat /tmp/factory_classroom_id)
  info "Seeding tasks..."

  local sort_order=1

  # ----- Nyhetskvartalet (NEWS) -----
  for level in 1 2 3; do
    local content
    content=$(jq -c -n '{
      question: "Hvilken av disse nyhetene er ekte?",
      hint: "Se etter kildehenvisninger og datoer i artiklene.",
      articles: [
        { imagePath: "upload://fake-article-1.jpg", isReal: false },
        { imagePath: "upload://real-article-1.png", isReal: true  }
      ]
    }')
    local title="Ekte eller falsk nyhet (nivå ${level})"
    local task_id; task_id=$(create_task_if_missing "$token" "$title" "NEWS" "nyhetskvartalet" "$level" "$content" \
      "$NEWS_FAKE_1" "$NEWS_REAL_1")
    if [ -n "$task_id" ]; then
      attach_task_to_classroom "$token" "$cid" "$task_id" "$sort_order"
      sort_order=$((sort_order+1))
    fi
  done

  # ----- Fotografen (PHOTO) -----
  for level in 1 2 3; do
    local content
    content=$(jq -c -n '{
      description: "Hvilken type bilde er dette?",
      alternatives: [
        { imagePath: "upload://real-photo-1.jpeg", description: "Bilde av en by ved solnedgang", correctType: "ekte",
          begrunnelser: ["Naturlig lysforhold","Konsistente skygger","Ingen synlige artefakter"] },
        { imagePath: "upload://fake-photo-1.jpg",  description: "Portrett av en person utendørs", correctType: "ai-generert",
          begrunnelser: ["Urealistiske detaljer i bakgrunnen","Feil antall fingre","Unaturlig tekstur på huden"] },
        { imagePath: "upload://fake-photo-1.jpg",  description: "Folkemengde på et torg", correctType: "manipulert",
          begrunnelser: ["Inkonsistente skygger","Synlig kloning av personer","Feil perspektiv"] }
      ]
    }')
    local title="Ekte, AI eller manipulert (nivå ${level})"
    local task_id; task_id=$(create_task_if_missing "$token" "$title" "PHOTO" "fotografen" "$level" "$content" \
      "$PHOTO_REAL_1" "$PHOTO_FAKE_1")
    if [ -n "$task_id" ]; then
      attach_task_to_classroom "$token" "$cid" "$task_id" "$sort_order"
      sort_order=$((sort_order+1))
    fi
  done

  # ----- Datasenteret — final boss area, 5 mixed tasks -----
  # Reuses NEWS and PHOTO content shapes so the existing renderers in
  # NyhetskvartaletComponent / FotografenComponent can drive them. Mix is
  # 3 NEWS + 2 PHOTO with high difficulty (4-5).

  # NEWS content for the boss area — same shape as nyhetskvartalet seeds.
  local news_content
  news_content=$(jq -c -n '{
    question: "Datatyven har plantet en falsk nyhet. Hvilken er ekte?",
    hint: "Sjekk kilden, forfatteren og datoen — de svakeste signalene avslører løgnen.",
    articles: [
      { imagePath: "upload://fake-article-1.jpg", isReal: false },
      { imagePath: "upload://real-article-1.png", isReal: true  }
    ]
  }')

  # PHOTO content for the boss area — same shape as fotografen seeds.
  local photo_content
  photo_content=$(jq -c -n '{
    description: "Datatyven har manipulert bevismateriale. Klassifiser hvert bilde riktig.",
    alternatives: [
      { imagePath: "upload://real-photo-1.jpeg", description: "Bilde fra et serverrom",     correctType: "ekte",
        begrunnelser: ["Naturlig lysforhold","Konsistente skygger","Ingen synlige artefakter"] },
      { imagePath: "upload://fake-photo-1.jpg",  description: "Portrett av en mistenkt",     correctType: "ai-generert",
        begrunnelser: ["Urealistiske detaljer","Feil antall fingre","Unaturlig hudtekstur"] },
      { imagePath: "upload://fake-photo-1.jpg",  description: "Bilde fra et åsted",          correctType: "manipulert",
        begrunnelser: ["Inkonsistente skygger","Synlig kloning","Feil perspektiv"] }
    ]
  }')

  # 3 NEWS + 2 PHOTO, difficulty 4-5.
  local boss_specs=(
    "Datatyven sin første løgn|NEWS|4|$news_content"
    "Avslør den falske rapporten|NEWS|5|$news_content"
    "Manipulert bevismateriale|PHOTO|4|$photo_content"
    "Sannhetens øyeblikk|NEWS|5|$news_content"
    "Endelig konfrontasjon|PHOTO|5|$photo_content"
  )

  for spec in "${boss_specs[@]}"; do
    IFS='|' read -r title type difficulty content <<<"$spec"
    local task_id
    if [ "$type" = "NEWS" ]; then
      task_id=$(create_task_if_missing "$token" "$title" "$type" "datasenteret" "$difficulty" "$content" \
        "$NEWS_FAKE_1" "$NEWS_REAL_1")
    else
      task_id=$(create_task_if_missing "$token" "$title" "$type" "datasenteret" "$difficulty" "$content" \
        "$PHOTO_REAL_1" "$PHOTO_FAKE_1")
    fi
    if [ -n "$task_id" ]; then
      attach_task_to_classroom "$token" "$cid" "$task_id" "$sort_order"
      sort_order=$((sort_order+1))
    fi
  done
}

seed_achievements() {
  info "Seeding achievements..."
  mysql_exec "
    INSERT IGNORE INTO achievement (achievement_id, title, desc_ach, icon, trigger_type, trigger_ref) VALUES
      (UUID(), 'Første Oppgave',          'Fullfør din første oppgave',                    NULL, 'task_completed',      'first_task'),
      (UUID(), 'Nyhetsjeger',             'Fullfør alle oppgaver i Nyhetskvartalet',        NULL, 'location_completed',  'nyhetskvartalet'),
      (UUID(), 'Fotoproff',               'Fullfør alle oppgaver hos Fotografen',           NULL, 'location_completed',  'fotografen'),
      (UUID(), 'Datamester',              'Fullfør alle oppgaver i Datasenteret',           NULL, 'location_completed',  'datasenteret'),
      (UUID(), 'Første mysterium løst',   'Løs ditt første ukens mysterium',                NULL, 'mystery_solved',      'first'),
      (UUID(), 'Fem mysterier løst',      'Løs fem ukens mysterier',                        NULL, 'mystery_solved',      '5'),
      (UUID(), 'Sikkerhetsmester',        'Fullfør alle oppgaver i et klasserom',            NULL, 'all_tasks_completed', 'classroom'),
      (UUID(), 'Plettfri Detektiv',       'Fullfør alle oppgaver i et klasserom uten å feile', NULL, 'perfect_classroom',   'classroom'),
      (UUID(), 'Aldri Gi Opp',            'Feil 10 oppgaveforsøk',                          NULL, 'tasks_failed',        '10');
  "
  ok "Achievements seeded"
}

seed_mysteries() {
  local token; token=$(cat /tmp/factory_laerer_token)
  local cid;   cid=$(cat /tmp/factory_classroom_id)
  info "Seeding mystery submissions for classroom ${cid:0:8}..."

  # Idempotent: skip if any submissions already exist in the classroom.
  local existing; existing=$(http_body "$(api GET "/api/v1/mysteries/classroom/${cid}" "" "$token")")
  local existing_count
  existing_count=$(echo "$existing" | jq -r '.data | length // 0' 2>/dev/null || echo 0)
  if [ "$existing_count" != "0" ] && [ -n "$existing_count" ]; then
    skip "Mystery submissions already exist (${existing_count})"
    return
  fi

  # {studentEmail | image | alt0 | alt1 | alt2 | correctIndex}
  local specs=(
    "student1@test.no|${NEWS_FAKE_1}|Artikkelen er ekte og publisert av en kjent avis|Artikkelen er en falsk nyhet med oppdiktede kilder|Artikkelen er satire og ikke ment alvorlig|1"
    "student2@test.no|${NEWS_REAL_1}|Bildet er et AI-generert portrett|Bildet er manipulert i Photoshop|Bildet er et ekte pressefoto|2"
    "student3@test.no|${PHOTO_FAKE_1}|Bildet er ekte og uredigert|Bildet er AI-generert|Bildet er en collage av flere bilder|1"
  )

  for spec in "${specs[@]}"; do
    IFS='|' read -r email img alt0 alt1 alt2 correct <<<"$spec"
    if [ ! -f "$img" ]; then
      fail "Mystery image not found: $img"
      continue
    fi
    local stu_token; stu_token=$(feide_login "$email" "student" "Student ${email%%@*}")

    local submission_json
    submission_json=$(jq -c -n \
      --arg cid "$cid" \
      --arg a0 "$alt0" --arg a1 "$alt1" --arg a2 "$alt2" \
      --argjson ci "$correct" \
      '{classroomId:$cid, alternative1:$a0, alternative2:$a1, alternative3:$a2, correctIndex:$ci}')

    local body_file; body_file=$(mktemp -t factory_mystery.XXXXXX.json)
    printf '%s' "$submission_json" > "$body_file"

    local resp code body
    resp=$(curl -s -w "\n%{http_code}" -X POST \
      -H "Authorization: Bearer $stu_token" \
      -F "submission=@${body_file};type=application/json" \
      -F "image=@${img}" \
      "${BASE_URL}/api/v1/mysteries")
    rm -f "$body_file"

    code=$(http_code "$resp"); body=$(http_body "$resp")
    if [[ "$code" =~ ^20[01]$ ]]; then
      ok "Mystery submitted by $email (PENDING)"
    else
      fail "Mystery submit for $email failed ($code): $(echo "$body" | head -c 200)"
    fi
  done
}

activate_all_tasks() {
  info "Activating all tasks (DB)..."
  mysql_exec "UPDATE task SET is_active = TRUE;"
  ok "Tasks activated"
}

summary() {
  local cid jc
  cid=$(cat /tmp/factory_classroom_id 2>/dev/null || echo "")
  jc=$(cat /tmp/factory_join_code     2>/dev/null || echo "")
  echo
  echo "=========================================="
  echo -e "${GREEN}factory.sh complete${NC}"
  echo "=========================================="
  echo "  Backend:    ${BASE_URL}"
  echo "  Classroom:  ${CLASSROOM_TITLE}  (id ${cid:0:8}..., join $jc)"
  echo
  echo "  Default teacher:"
  echo "    laerer@ntnu.no / ${PASSWORD}"
  echo "  Extra teachers:"
  echo "    instructor@test.no / ${PASSWORD}"
  echo "    admin@test.no      / ${PASSWORD}"
  echo "  Students:"
  for n in 1 2 3 4 5; do echo "    student${n}@test.no / ${PASSWORD}"; done
  echo "=========================================="
}

main() {
  wait_for_backend
  seed_institution
  seed_users
  ensure_classroom
  join_students
  approve_students
  seed_tasks
  activate_all_tasks
  seed_achievements
  seed_mysteries
  summary
}

main "$@"
