#!/bin/bash

# Creates sample NEWS tasks for the Nyhetskvartalet stop
# Usage: ./create-nyhetskvartalet-tasks.sh [email] [password]
# Default: ./create-nyhetskvartalet-tasks.sh instructor@test.no password123

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
            taskType: "NEWS",
            difficulty: $difficulty,
            isActive: true,
            stopLocation: "nyhetskvartalet",
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

echo -e "${BLUE}[INFO]${NC} Creating Nyhetskvartalet tasks..."
echo ""

TASK1_CONTENT=$(jq -c -n '{
    question: "Hvilken av disse nyhetene er ekte?",
    hint: "Se etter kildehenvisninger og datoer i artiklene.",
    articles: [
        {
            imagePath: "/news/fake-article-1.jpg",
            isReal: false
        },
        {
            imagePath: "/news/real-article-1.png",
            isReal: true
        }
    ]
}')

TASK2_CONTENT=$(jq -c -n '{
    question: "Kan du identifisere hvilken nyhetssak som er manipulert?",
    hint: "Vær oppmerksom på sensasjonelle overskrifter og manglende kilder.",
    articles: [
        {
            imagePath: "/news/real-article-1.png",
            isReal: true
        },
        {
            imagePath: "/news/fake-article-1.jpg",
            isReal: false
        }
    ]
}')

TASK3_CONTENT=$(jq -c -n '{
    question: "Finn den falske nyhetsartikkelen blant disse to.",
    hint: "Sjekk om nettstedet er kjent og pålitelig.",
    articles: [
        {
            imagePath: "/news/fake-article-1.jpg",
            isReal: false
        },
        {
            imagePath: "/news/real-article-1.png",
            isReal: true
        }
    ]
}')

create_task "Ekte eller falsk nyhet? (nivå 1)" "$TASK1_CONTENT" 1
create_task "Identifiser manipulert nyhet (nivå 2)" "$TASK2_CONTENT" 2
create_task "Finn den falske artikkelen (nivå 3)" "$TASK3_CONTENT" 3

echo ""
echo -e "${GREEN}Done.${NC}"
