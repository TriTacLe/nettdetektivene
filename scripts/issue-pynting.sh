#!/bin/bash
#
# Add emoji prefixes to GitHub issues based on labels
# Usage: ./add-issue-emojis.sh [--dry-run] [issue-number]
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

DRY_RUN=false

# Emoji mappings (based on your team's convention)
declare -A EMOJI_MAP=(
  ["backend"]="🩷"
  ["frontend"]="💛"
  ["documentation"]="🧡"
  ["bug"]="🐛"
  ["Testing"]="🧪"
  ["enhancement"]="✨"
  ["P0 - critical"]="🔴"
  ["P1 - high"]="🟠"
  ["P2 - medium"]="🟡"
  ["P3 - low"]="🟢"
)

# Get emoji for an issue based on its labels
get_emoji() {
  local labels="$1"
  local emoji=""

  # Check priority first (prepend later)
  local priority=""
  for p in "P0 - critical" "P1 - high" "P2 - medium" "P3 - low"; do
    if echo "$labels" | grep -qi "$p"; then
      priority="${EMOJI_MAP[$p]}"
      break
    fi
  done

  # Check type labels
  local type=""
  if echo "$labels" | grep -qi "backend"; then
    type="${EMOJI_MAP["backend"]}"
  elif echo "$labels" | grep -qi "frontend"; then
    type="${EMOJI_MAP["frontend"]}"
  elif echo "$labels" | grep -qi "bug"; then
    type="${EMOJI_MAP["bug"]}"
  elif echo "$labels" | grep -qi "documentation"; then
    type="${EMOJI_MAP["documentation"]}"
  elif echo "$labels" | grep -qi "Testing"; then
    type="${EMOJI_MAP["Testing"]}"
  elif echo "$labels" | grep -qi "enhancement"; then
    type="${EMOJI_MAP["enhancement"]}"
  fi

  # Combine priority + type
  if [[ -n "$priority" && -n "$type" ]]; then
    echo "$priority $type"
  elif [[ -n "$priority" ]]; then
    echo "$priority"
  elif [[ -n "$type" ]]; then
    echo "$type"
  fi
}

# Update a single issue
update_issue() {
  local num="$1"

  # Get current issue data
  local data=$(gh issue view "$num" --json number,title,labels --jq '[.number, .title, (.labels | map(.name) | join(","))] | @tsv' 2>/dev/null)

  if [[ -z "$data" ]]; then
    echo "${RED}Issue #$num not found${NC}"
    return 1
  fi

  local title=$(echo "$data" | cut -f2)
  local labels=$(echo "$data" | cut -f3)

  # Skip if already has emoji
  if [[ "$title" =~ ^(🩷|💛|🧡|🐛|✨|🧪|🔴|🟠|🟡|🟢) ]]; then
    echo "  #$num already has emoji: $title"
    return 0
  fi

  local emoji=$(get_emoji "$labels")

  if [[ -z "$emoji" ]]; then
    echo "  #$num no matching label: $title"
    return 0
  fi

  local new_title="$emoji $title"

  echo "  ${YELLOW}#$num:${NC} $title"
  echo "      ${GREEN}→${NC} $new_title"

  if [[ "$DRY_RUN" == false ]]; then
    gh issue edit "$num" --title "$new_title" >/dev/null
    echo "      ${GREEN}✓ updated${NC}"
  fi
}

# Main
main() {
  local specific_issue=""

  # Parse args
  for arg in "$@"; do
    case "$arg" in
    --dry-run)
      DRY_RUN=true
      echo "${YELLOW}DRY RUN MODE - No changes will be made${NC}"
      ;;
    [0-9]*)
      specific_issue="$arg"
      ;;
    esac
  done

  echo "${BLUE}Adding emoji prefixes to issues...${NC}"
  echo ""

  if [[ -n "$specific_issue" ]]; then
    update_issue "$specific_issue"
  else
    # Get all open issues
    local issues=$(gh issue list --state all --limit 1000 --json number --jq '.[].number')

    if [[ -z "$issues" ]]; then
      echo "No issues found"
      exit 0
    fi

    for num in $issues; do
      update_issue "$num"
    done
  fi

  echo ""
  echo "${GREEN}Done!${NC}"
}

# Help
if [[ "$1" == "--help" || "$1" == "-h" ]]; then
  echo "Add emoji prefixes to GitHub issues based on labels"
  echo ""
  echo "Usage: $0 [--dry-run] [issue-number]"
  echo ""
  echo "Options:"
  echo "  --dry-run      Preview changes without applying"
  echo "  issue-number   Update only a specific issue"
  echo ""
  echo "Examples:"
  echo "  $0                    # Update all issues"
  echo "  $0 --dry-run          # Preview all changes"
  echo "  $0 265                # Update issue #265"
  echo "  $0 --dry-run 265      # Preview issue #265"
  echo ""
  echo "Emoji mapping:"
  echo "  🩷  backend"
  echo "  💛  frontend"
  echo "  🧡  documentation"
  echo "  🐛  bug"
  echo "  🧪  Testing"
  echo "  ✨  enhancement"
  echo "  🔴  P0 - critical"
  echo "  🟠  P1 - high"
  echo "  🟡  P2 - medium"
  echo "  🟢  P3 - low"
  exit 0
fi

main "$@"
