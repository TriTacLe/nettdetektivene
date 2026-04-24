#!/bin/bash

# Creates a classroom and assigns all tasks to it (news + photo tasks).
# Usage: ./setup-classroom.sh [email] [password] [classroom_title]
#
# Defaults: instructor@test.no / password123 / "Nettdetektivene"

BASE_URL="http://localhost:8080"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-root}"
MYSQL_DATABASE="${MYSQL_DATABASE:-nd-db}"

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

EMAIL="${1:-instructor@test.no}"
PASSWORD="${2:-password123}"
CLASSROOM_TITLE="${3:-Nettdetektivene}"

db() { docker exec nd-db mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -sN -e "$1" 2>/dev/null; }

api() {
    local method=$1 endpoint=$2 data=${3:-} token=${4:-}
    local args=(-s -w "\n%{http_code}" -X "$method")
    [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
    [ -n "$data" ] && args+=(-H "Content-Type: application/json" -d "$data")
    curl "${args[@]}" "${BASE_URL}${endpoint}"
}
body()      { echo "$1" | sed '$d'; }
http_code() { echo "$1" | tail -n1 | tr -d '\n\r'; }

echo "========================================="
echo "Classroom setup"
echo "========================================="
echo ""

# ---- check backend ----
if ! curl -s --max-time 5 "${BASE_URL}/api/v1/auth/login" >/dev/null 2>&1; then
    echo -e "${RED}[ERROR]${NC} Backend not reachable at $BASE_URL"
    exit 1
fi

# ---- ensure institution exists ----
INSTITUTION_ID=$(db "SELECT institution_id FROM institution LIMIT 1")
if [ -z "$INSTITUTION_ID" ]; then
    echo -e "${YELLOW}[INFO]${NC} No institution found — creating NTNU..."
    INSTITUTION_ID="aaaaaaaa-0000-0000-0000-000000000001"
    db "INSERT IGNORE INTO institution (institution_id, inst_name) VALUES ('$INSTITUTION_ID', 'NTNU')"
    echo -e "${GREEN}[OK]${NC} Institution created"
fi

# ---- login (register first if needed) ----
echo -e "${BLUE}[INFO]${NC} Logging in as $EMAIL..."
login=$(api "POST" "/api/v1/auth/login" "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
TOKEN=$(body "$login" | jq -r '.token' 2>/dev/null)
if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo -e "${YELLOW}[INFO]${NC} Login failed — registering instructor account..."
    reg=$(api "POST" "/api/v1/auth/register" \
        "{\"displayName\":\"Instructor\",\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"role\":\"inst_admin\"}")
    reg_code=$(http_code "$reg")
    if [ "$reg_code" != "200" ] && [ "$reg_code" != "201" ]; then
        echo -e "${RED}[ERROR]${NC} Registration failed (HTTP $reg_code): $(body "$reg")"
        exit 1
    fi
    echo -e "${GREEN}[OK]${NC} Instructor registered"
    login=$(api "POST" "/api/v1/auth/login" "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
    TOKEN=$(body "$login" | jq -r '.token' 2>/dev/null)
    if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
        echo -e "${RED}[ERROR]${NC} Login after registration failed: $(body "$login")"
        exit 1
    fi
fi
echo -e "${GREEN}[OK]${NC} Logged in"
echo ""

# ---- create classroom ----
echo -e "${BLUE}[INFO]${NC} Creating classroom \"$CLASSROOM_TITLE\"..."
cr=$(api "POST" "/api/v1/classrooms" \
    "{\"title\":\"$CLASSROOM_TITLE\",\"classDesc\":\"Auto-created classroom\",\"institutionId\":\"$INSTITUTION_ID\"}" "$TOKEN")
cr_code=$(http_code "$cr")
cr_body=$(body "$cr")

if [ "$cr_code" != "200" ] && [ "$cr_code" != "201" ]; then
    echo -e "${RED}[ERROR]${NC} Failed to create classroom (HTTP $cr_code): $cr_body"
    exit 1
fi

CLASSROOM_ID=$(echo "$cr_body" | jq -r '.data.id' 2>/dev/null)
JOIN_CODE=$(echo "$cr_body" | jq -r '.data.joinCode' 2>/dev/null)

echo -e "${GREEN}[OK]${NC} Classroom created"
echo -e "     ID:        $CLASSROOM_ID"
echo -e "     Join code: $JOIN_CODE"
echo ""

# ---- enable game mode ----
db "UPDATE classroom SET is_game_enabled = true WHERE classroom_id = '$CLASSROOM_ID'"

# ---- create and assign tasks ----
echo -e "${BLUE}[INFO]${NC} Creating and assigning tasks..."

create_and_assign() {
    local title=$1 task_type=$2 content=$3 difficulty=$4 stop=$5 sort_order=$6

    local payload
    payload=$(jq -n \
        --arg title "$title" \
        --arg taskType "$task_type" \
        --arg content "$content" \
        --argjson difficulty "$difficulty" \
        --arg stopLocation "$stop" \
        '{title:$title,taskType:$taskType,difficulty:$difficulty,isActive:true,stopLocation:$stopLocation,content:$content}')

    local resp code task_id
    resp=$(api "POST" "/api/v1/tasks" "$payload" "$TOKEN")
    code=$(http_code "$resp")
    task_id=$(body "$resp" | jq -r '.data.id' 2>/dev/null)

    if [ "$code" != "200" ] && [ "$code" != "201" ] || [ -z "$task_id" ] || [ "$task_id" = "null" ]; then
        echo -e "  ${RED}[FAIL]${NC} Could not create task: $title (HTTP $code)"
        return
    fi

    db "INSERT IGNORE INTO classroom_task (classroom_id, task_id, sort_order, is_active)
        VALUES ('$CLASSROOM_ID', '$task_id', $sort_order, 1)"

    echo -e "  ${GREEN}[OK]${NC} [$sort_order] $title"
}

NEWS1='{"articles":[{"imagePath":"/news/fake-article-1.jpg","isReal":false},{"imagePath":"/news/real-article-1.png","isReal":true}]}'
NEWS2='{"articles":[{"imagePath":"/news/fake-article-1.jpg","isReal":false},{"imagePath":"/news/real-article-1.png","isReal":true}]}'
NEWS3='{"articles":[{"imagePath":"/news/fake-article-1.jpg","isReal":false},{"imagePath":"/news/real-article-1.png","isReal":true}]}'

PHOTO1=$(jq -c -n '{
  description: "Hvilken type bilde er dette?",
  alternatives: [
    {
      imagePath: "/photos/real-photo-1.jpeg",
      description: "Bilde av en by ved solnedgang",
      correctType: "ekte",
      begrunnelser: ["Naturlig lysforhold", "Konsistente skygger", "Ingen synlige artefakter"]
    },
    {
      imagePath: "/photos/fake-photo-1.jpg",
      description: "Portrett av en person utendors",
      correctType: "ai-generert",
      begrunnelser: ["Urealistiske detaljer i bakgrunnen", "Feil antall fingre", "Unaturlig tekstur pa huden"]
    },
    {
      imagePath: "/photos/fake-photo-1.jpg",
      description: "Folkemengde pa et torg",
      correctType: "manipulert",
      begrunnelser: ["Inkonsistente skygger", "Synlig kloning av personer", "Feil perspektiv"]
    }
  ]
}')

PHOTO2=$(jq -c -n '{
  description: "Sorter disse bildene etter type.",
  alternatives: [
    {
      imagePath: "/photos/fake-photo-1.jpg",
      description: "Dyr i naturen",
      correctType: "ai-generert",
      begrunnelser: ["Uvanlig anatomisk proporsjoner", "For perfekt pelstekstur", "Bakgrunnen ser generert ut"]
    },
    {
      imagePath: "/photos/real-photo-1.jpeg",
      description: "Landskap med fjell",
      correctType: "ekte",
      begrunnelser: ["Naturlig gradering i himmelen", "Realistisk dybdeskarphet", "Konsistente lysforhold"]
    },
    {
      imagePath: "/photos/fake-photo-1.jpg",
      description: "Politiker pa talerstol",
      correctType: "manipulert",
      begrunnelser: ["Objektet er lagt til i ettertid", "Kanten rundt personen ser urealistisk ut", "Forskjellig bildekvalitet"]
    }
  ]
}')

PHOTO3=$(jq -c -n '{
  description: "Klarer du a identifisere alle tre typer bilder?",
  alternatives: [
    {
      imagePath: "/photos/fake-photo-1.jpg",
      description: "Nyhetsbilde fra en hendelse",
      correctType: "manipulert",
      begrunnelser: ["Tekst er lagt til i bildet", "Fargebalansen stemmer ikke overens", "Piksler rundt endringene er synlige"]
    },
    {
      imagePath: "/photos/fake-photo-1.jpg",
      description: "Arkitektur og bygninger",
      correctType: "ai-generert",
      begrunnelser: ["Geometrien er ikke konsistent", "Vinduer og dorer har feil proporsjoner", "Urealistiske materialteksturer"]
    },
    {
      imagePath: "/photos/real-photo-1.jpeg",
      description: "Barn som leker i park",
      correctType: "ekte",
      begrunnelser: ["Naturlig bevegelsesuskarphet", "Autentiske ansiktsuttrykk", "Konsistent belysning"]
    }
  ]
}')

create_and_assign "Ekte eller falsk nyhet? (nivå 1)"       "NEWS"  "$NEWS1"  1 "nyhetskvartalet" 1
create_and_assign "Identifiser manipulert nyhet (nivå 2)"  "NEWS"  "$NEWS2"  2 "nyhetskvartalet" 2
create_and_assign "Finn den falske artikkelen (nivå 3)"    "NEWS"  "$NEWS3"  3 "nyhetskvartalet" 3
create_and_assign "Ekte eller falsk bilde? (nivå 1)"       "PHOTO" "$PHOTO1" 1 "fotografen"      4
create_and_assign "Identifiser manipulert bilde (nivå 2)"  "PHOTO" "$PHOTO2" 2 "fotografen"      5
create_and_assign "Finn det falske bildet (nivå 3)"        "PHOTO" "$PHOTO3" 3 "fotografen"      6

TASK_COUNT=$(db "SELECT COUNT(*) FROM classroom_task WHERE classroom_id = '$CLASSROOM_ID'")
echo ""
echo -e "${BLUE}[INFO]${NC} Tasks in classroom: $TASK_COUNT / 6"
echo ""
echo "========================================="
echo -e "${GREEN}Done${NC}"
echo "  Classroom: $CLASSROOM_TITLE"
echo "  ID:        $CLASSROOM_ID"
echo "  Join code: $JOIN_CODE"
echo "========================================="
echo ""
echo "Next steps:"
echo "  • Share join code '$JOIN_CODE' with students"
echo "  • Run: ./check-first-task-unlock.sh"
