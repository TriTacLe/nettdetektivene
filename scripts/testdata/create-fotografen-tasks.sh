#!/bin/bash

# Creates sample PHOTO tasks for the Fotografen stop
# Usage: ./create-fotografen-tasks.sh [email] [password]
# Default: ./create-fotografen-tasks.sh instructor@test.no password123

BASE_URL="http://localhost:8080"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
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

check_server() {
    if ! curl -s --max-time 3 "${BASE_URL}/api/v1/tasks" >/dev/null 2>&1; then
        echo -e "${RED}[FAIL] Backend not running. Run: docker-compose up -d${NC}"
        exit 1
    fi
}

check_server

EMAIL="${1:-instructor@test.no}"
PASSWORD="${2:-password123}"

echo -e "${BLUE}[INFO]${NC} Logging in as $EMAIL..."
login_response=$(make_api_call "POST" "/api/v1/auth/login" "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
token=$(echo "$login_response" | jq -r '.token' 2>/dev/null)

if [ "$token" = "null" ] || [ -z "$token" ]; then
    echo -e "${RED}[ERROR]${NC} Login failed. Check credentials."
    exit 1
fi

echo -e "${GREEN}Logged in successfully${NC}"
echo ""

create_task() {
    local title=$1
    local content=$2
    local difficulty=$3

    payload=$(jq -n \
        --arg title "$title" \
        --arg content "$content" \
        --argjson difficulty "$difficulty" \
        '{
            title: $title,
            taskType: "PHOTO",
            difficulty: $difficulty,
            isActive: true,
            stopLocation: "fotografen",
            content: $content
        }')

    response=$(make_api_call "POST" "/api/v1/tasks" "$payload" "$token")
    task_id=$(echo "$response" | jq -r '.data.id' 2>/dev/null)

    if [ -z "$task_id" ] || [ "$task_id" = "null" ]; then
        echo -e "${RED}[FAIL]${NC} Could not create task: $title"
        echo "Response: $response"
    else
        echo -e "${GREEN}[OK]${NC} Created: $title (id: ${task_id:0:8}...)"
    fi
}

echo -e "${BLUE}[INFO]${NC} Creating Fotografen tasks..."
echo ""

TASK1_CONTENT=$(jq -c -n '{
    description: "Hvilken type bilde er dette?",
    alternatives: [
        {
            imagePath: "/photos/real-photo-1.jpeg",
            description: "Bilde av en by ved solnedgang",
            correctType: "ekte",
            begrunnelser: [
                "Naturlig lysforhold",
                "Konsistente skygger",
                "Ingen synlige artefakter"
            ]
        },
        {
            imagePath: "/photos/fake-photo-1.jpg",
            description: "Portrett av en person utendors",
            correctType: "ai-generert",
            begrunnelser: [
                "Urealistiske detaljer i bakgrunnen",
                "Feil antall fingre",
                "Unaturlig tekstur pa huden"
            ]
        },
        {
            imagePath: "/photos/fake-photo-1.jpg",
            description: "Folkemengde pa et torg",
            correctType: "manipulert",
            begrunnelser: [
                "Inkonsistente skygger",
                "Synlig kloning av personer",
                "Feil perspektiv"
            ]
        }
    ]
}')

TASK2_CONTENT=$(jq -c -n '{
    description: "Sorter disse bildene etter type.",
    alternatives: [
        {
            imagePath: "/photos/fake-photo-1.jpg",
            description: "Dyr i naturen",
            correctType: "ai-generert",
            begrunnelser: [
                "Uvanlig anatomisk proporsjoner",
                "For perfekt pelstekstur",
                "Bakgrunnen ser generert ut"
            ]
        },
        {
            imagePath: "/photos/real-photo-1.jpeg",
            description: "Landskap med fjell",
            correctType: "ekte",
            begrunnelser: [
                "Naturlig gradering i himmelen",
                "Realistisk dybdeskarphet",
                "Konsistente lysforhold"
            ]
        },
        {
            imagePath: "/photos/fake-photo-1.jpg",
            description: "Politiker pa talerstol",
            correctType: "manipulert",
            begrunnelser: [
                "Objektet er lagt til i ettertid",
                "Kanten rundt personen ser urealistisk ut",
                "Forskjellig bildekvalitet"
            ]
        }
    ]
}')

TASK3_CONTENT=$(jq -c -n '{
    description: "Klarer du a identifisere alle tre typer bilder?",
    alternatives: [
        {
            imagePath: "/photos/fake-photo-1.jpg",
            description: "Nyhetsbilde fra en hendelse",
            correctType: "manipulert",
            begrunnelser: [
                "Tekst er lagt til i bildet",
                "Fargebalansen stemmer ikke overens",
                "Piksler rundt endringene er synlige"
            ]
        },
        {
            imagePath: "/photos/fake-photo-1.jpg",
            description: "Arkitektur og bygninger",
            correctType: "ai-generert",
            begrunnelser: [
                "Geometrien er ikke konsistent",
                "Vinduer og dorer har feil proporsjoner",
                "Urealistiske materialteksturer"
            ]
        },
        {
            imagePath: "/photos/real-photo-1.jpeg",
            description: "Barn som leker i park",
            correctType: "ekte",
            begrunnelser: [
                "Naturlig bevegelsesuskarphet",
                "Autentiske ansiktsuttrykk",
                "Konsistent belysning"
            ]
        }
    ]
}')

create_task "Ekte, AI eller manipulert? (nivå 1)" "$TASK1_CONTENT" 1
create_task "Sorter bildene etter type (nivå 2)" "$TASK2_CONTENT" 2
create_task "Avanser bildegjenkjenning (nivå 3)" "$TASK3_CONTENT" 3

echo ""
echo -e "${GREEN}Done.${NC}"
