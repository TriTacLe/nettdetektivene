#!/bin/bash

# Creates sample PHOTO and NEWS tasks for the stops
# Usage: ./create-all-tasks.sh [email] [password] [classroom_id]
# Default: ./create-all-tasks.sh instructor@test.no password123

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
CLASSROOM_ID="${3:-}"

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
    local task_type=$2
    local content=$3
    local difficulty=$4
    local stop_location=$5

    payload=$(jq -n \
        --arg title "$title" \
        --arg taskType "$task_type" \
        --arg content "$content" \
        --argjson difficulty "$difficulty" \
        --arg stopLocation "$stop_location" \
        '{
            title: $title,
            taskType: $taskType,
            difficulty: $difficulty,
            isActive: true,
            stopLocation: $stopLocation,
            content: $content
        }')

    response=$(make_api_call "POST" "/api/v1/tasks" "$payload" "$token")
    task_id=$(echo "$response" | jq -r '.data.id' 2>/dev/null)

    if [ -z "$task_id" ] || [ "$task_id" = "null" ]; then
        echo -e "${RED}[FAIL]${NC} Could not create task: $title" >&2
        echo "Response: $response" >&2
        return 1
    else
        echo -e "${GREEN}[OK]${NC} Created: $title (id: ${task_id:0:8}...)" >&2
        echo "$task_id"
        return 0
    fi
}

get_classroom_id() {
    if [ -n "$CLASSROOM_ID" ]; then
        echo "$CLASSROOM_ID"
        return
    fi

    response=$(make_api_call "GET" "/api/v1/classrooms/mine" "" "$token")
    classroom_id=$(echo "$response" | jq -r '.data[0].id' 2>/dev/null)
    if [ -n "$classroom_id" ] && [ "$classroom_id" != "null" ]; then
        echo "$classroom_id"
    else
        echo ""
    fi
}

assign_task_to_classroom() {
    local task_id=$1
    local classroom_id=$2
    local sort_order=$3

    if [ -z "$classroom_id" ]; then
        echo -e "${YELLOW}[SKIP]${NC} No classroom ID provided, skipping task assignment"
        return
    fi

    docker exec nd-db mysql -uroot -proot nd-db \
        -e "INSERT IGNORE INTO classroom_task (classroom_id, task_id, sort_order, is_active) VALUES ('$classroom_id', '$task_id', $sort_order, 1);" 2>/dev/null
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}[OK]${NC} Assigned task to classroom (sort order: $sort_order)"
    else
        echo -e "${YELLOW}[WARN]${NC} Could not assign task to classroom (already assigned or error)"
    fi
}

unlock_first_news_task() {
    local task_id=$1
    local classroom_id=$2

    if [ -z "$classroom_id" ] || [ -z "$task_id" ]; then
        echo -e "${YELLOW}[SKIP]${NC} Cannot unlock first news task - missing IDs"
        return
    fi

    echo -e "${BLUE}[INFO]${NC} Unlocking first Nyhetskvartalet task for all classroom students..."
    
    # Get all students in the classroom directly from database
    students=$(docker exec nd-db mysql -uroot -proot nd-db -sN \
        -e "SELECT user_id FROM classroom_student WHERE classroom_id = '$classroom_id' AND is_allowed = 1;" 2>/dev/null)

    if [ -z "$students" ]; then
        echo -e "${YELLOW}[WARN]${NC} No approved students found in classroom"
        echo -e "${BLUE}[HINT]${NC} Students may need to be approved by the instructor first"
        return
    fi

    unlock_count=0
    for student_id in $students; do
        docker exec nd-db mysql -uroot -proot nd-db \
            -e "INSERT IGNORE INTO user_task_unlocked (user_id, classroom_id, task_id, unlocked_at) VALUES ('$student_id', '$classroom_id', '$task_id', NOW());" 2>/dev/null
        if [ $? -eq 0 ]; then
            ((unlock_count++))
        fi
    done

    echo -e "${GREEN}[OK]${NC} Unlocked first news task for $unlock_count students"
}

# Store task IDs
declare -a NEWS_TASK_IDS
declare -a PHOTO_TASK_IDS

# Create Nyhetskvartalet NEWS tasks
echo -e "${BLUE}[INFO]${NC} Creating Nyhetskvartalet tasks..."
echo ""

TASK1_CONTENT=$(jq -c -n '{
    question: "Hvilken av disse nyhetene er ekte?",
    hint: "Se etter kildehenvisninger og datoer i artiklene.",
    articles: [
        {
            imagePath: "/news/real-article-1.jpg",
            isReal: false
        },
        {
            imagePath: "/news/real-article-1.jpg",
            isReal: true
        }
    ]
}')

TASK2_CONTENT=$(jq -c -n '{
    question: "Kan du identifisere hvilken nyhetssak som er manipulert?",
    hint: "Vær oppmerksom på sensasjonelle overskrifter og manglende kilder.",
    articles: [
        {
            imagePath: "/news/real-article-1.jpg",
            isReal: true
        },
        {
            imagePath: "/news/real-article-1.jpg",
            isReal: false
        }
    ]
}')

TASK3_CONTENT=$(jq -c -n '{
    question: "Finn den falske nyhetsartikkelen blant disse to.",
    hint: "Sjekk om nettstedet er kjent og pålitelig.",
    articles: [
        {
            imagePath: "/news/real-article-1.jpg",
            isReal: false
        },
        {
            imagePath: "/news/real-article-1.jpg",
            isReal: true
        }
    ]
}')

NEWS_TASK_ID_1=$(create_task "Ekte eller falsk nyhet? (nivå 1)" "NEWS" "$TASK1_CONTENT" 1 "nyhetskvartalet")
if [ -n "$NEWS_TASK_ID_1" ]; then NEWS_TASK_IDS+=("$NEWS_TASK_ID_1"); fi

NEWS_TASK_ID_2=$(create_task "Identifiser manipulert nyhet (nivå 2)" "NEWS" "$TASK2_CONTENT" 2 "nyhetskvartalet")
if [ -n "$NEWS_TASK_ID_2" ]; then NEWS_TASK_IDS+=("$NEWS_TASK_ID_2"); fi

NEWS_TASK_ID_3=$(create_task "Finn den falske artikkelen (nivå 3)" "NEWS" "$TASK3_CONTENT" 3 "nyhetskvartalet")
if [ -n "$NEWS_TASK_ID_3" ]; then NEWS_TASK_IDS+=("$NEWS_TASK_ID_3"); fi

echo ""
echo -e "${BLUE}[INFO]${NC} Creating Fotografen tasks..."
echo ""

# Create Fotografen PHOTO tasks
PHOTO1_CONTENT=$(jq -c -n '{
    description: "Hvilken type bilde er dette?",
    alternatives: [
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Bilde av en by ved solnedgang",
            correctType: "ekte",
            begrunnelser: [
                "Naturlig lysforhold",
                "Konsistente skygger",
                "Ingen synlige artefakter"
            ]
        },
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Portrett av en person utendors",
            correctType: "ai-generert",
            begrunnelser: [
                "Urealistiske detaljer i bakgrunnen",
                "Feil antall fingre",
                "Unaturlig tekstur på huden"
            ]
        },
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Folkemengde på et torg",
            correctType: "manipulert",
            begrunnelser: [
                "Inkonsistente skygger",
                "Synlig kloning av personer",
                "Feil perspektiv"
            ]
        }
    ]
}')

PHOTO2_CONTENT=$(jq -c -n '{
    description: "Sorter disse bildene etter type.",
    alternatives: [
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Dyr i naturen",
            correctType: "ai-generert",
            begrunnelser: [
                "Uvanlig anatomisk proporsjoner",
                "For perfekt pelstekstur",
                "Bakgrunnen ser generert ut"
            ]
        },
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Landskap med fjell",
            correctType: "ekte",
            begrunnelser: [
                "Naturlig gradering i himmelen",
                "Realistisk dybdeskarphet",
                "Konsistente lysforhold"
            ]
        },
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Politiker på talerstol",
            correctType: "manipulert",
            begrunnelser: [
                "Objektet er lagt til i ettertid",
                "Kanten rundt personen ser urealistisk ut",
                "Forskjellig bildekvalitet"
            ]
        }
    ]
}')

PHOTO3_CONTENT=$(jq -c -n '{
    description: "Klarer du å identifisere alle tre typer bilder?",
    alternatives: [
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Nyhetsbilde fra en hendelse",
            correctType: "manipulert",
            begrunnelser: [
                "Tekst er lagt til i bildet",
                "Fargebalansen stemmer ikke overens",
                "Piksler rundt endringene er synlige"
            ]
        },
        {
            imagePath: "/photos/ai-photo-1.jpg",
            description: "Arkitektur og bygninger",
            correctType: "ai-generert",
            begrunnelser: [
                "Geometrien er ikke konsistent",
                "Vinduer og dører har feil proporsjoner",
                "Urealistiske materialteksturer"
            ]
        },
        {
            imagePath: "/photos/ai-photo-1.jpg",
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

PHOTO_TASK_ID_1=$(create_task "Ekte, AI eller manipulert? (nivå 1)" "PHOTO" "$PHOTO1_CONTENT" 1 "fotografen")
if [ -n "$PHOTO_TASK_ID_1" ]; then PHOTO_TASK_IDS+=("$PHOTO_TASK_ID_1"); fi

PHOTO_TASK_ID_2=$(create_task "Sorter bildene etter type (nivå 2)" "PHOTO" "$PHOTO2_CONTENT" 2 "fotografen")
if [ -n "$PHOTO_TASK_ID_2" ]; then PHOTO_TASK_IDS+=("$PHOTO_TASK_ID_2"); fi

PHOTO_TASK_ID_3=$(create_task "Avansert bildegjenkjenning (nivå 3)" "PHOTO" "$PHOTO3_CONTENT" 3 "fotografen")
if [ -n "$PHOTO_TASK_ID_3" ]; then PHOTO_TASK_IDS+=("$PHOTO_TASK_ID_3"); fi

# Assign tasks to classroom if classroom_id is provided
if [ ${#NEWS_TASK_IDS[@]} -gt 0 ] || [ ${#PHOTO_TASK_IDS[@]} -gt 0 ]; then
    CLASSROOM=$(get_classroom_id)
    
    if [ -n "$CLASSROOM" ]; then
        echo ""
        echo -e "${BLUE}[INFO]${NC} Assigning tasks to classroom $CLASSROOM..."
        
        # Assign news tasks with sort order 1-3
        for i in "${!NEWS_TASK_IDS[@]}"; do
            assign_task_to_classroom "${NEWS_TASK_IDS[$i]}" "$CLASSROOM" $((i + 1))
        done
        
        # Assign photo tasks with sort order 4-6
        for i in "${!PHOTO_TASK_IDS[@]}"; do
            assign_task_to_classroom "${PHOTO_TASK_IDS[$i]}" "$CLASSROOM" $((i + 4))
        done
        
        # Unlock first news task for all students
        unlock_first_news_task "${NEWS_TASK_IDS[0]}" "$CLASSROOM"
    else
        echo ""
        echo -e "${YELLOW}[WARN]${NC} No classroom found. Tasks created but not assigned."
        echo -e "${YELLOW}[HINT]${NC} Provide a classroom ID: $0 $EMAIL $PASSWORD <classroom_id>"
    fi
fi

echo ""
echo -e "${GREEN}Done.${NC}"
echo ""
echo -e "${BLUE}[SUMMARY]${NC}"
echo "News tasks created: ${#NEWS_TASK_IDS[@]}"
echo "Photo tasks created: ${#PHOTO_TASK_IDS[@]}"