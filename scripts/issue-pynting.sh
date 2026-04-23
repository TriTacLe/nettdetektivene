#!/bin/bash
# fix issue emojis based on labels

DRY_RUN=false

# emojis
PINK="🩷"    # backend
YELLOW="💛"  # frontend
ORANGE="🧡"  # other
RED="🔴"
ORANGE_C="🟠"
YELLOW_C="🟡"
GREEN="🟢"

ALL_EMOJIS="${PINK}${YELLOW}${ORANGE}${RED}${ORANGE_C}${YELLOW_C}${GREEN}🐛🧪✨"

get_type() {
    local labels="$1"
    if echo "$labels" | grep -qi "backend"; then
        echo "$PINK"
    elif echo "$labels" | grep -qi "frontend"; then
        echo "$YELLOW"
    else
        echo "$ORANGE"
    fi
}

get_prio() {
    local labels="$1"
    if echo "$labels" | grep -qi "P0"; then
        echo "$RED"
    elif echo "$labels" | grep -qi "P1"; then
        echo "$ORANGE_C"
    elif echo "$labels" | grep -qi "P2"; then
        echo "$YELLOW_C"
    elif echo "$labels" | grep -qi "P3"; then
        echo "$GREEN"
    fi
}

get_expected() {
    local labels="$1"
    local prio=$(get_prio "$labels")
    local type=$(get_type "$labels")

    if [ -n "$prio" ]; then
        echo "$prio $type"
    else
        echo "$type"
    fi
}

strip_emojis() {
    echo "$1" | sed -E "s/^[${ALL_EMOJIS} ]+//"
}

get_existing() {
    echo "$1" | grep -oE "^[${ALL_EMOJIS}]( [${ALL_EMOJIS}])?" || true
}

has_wrong_emoji() {
    local title="$1"
    local expected="$2"
    local existing=$(get_existing "$title")

    # check if title starts with any emoji that's not the expected one
    if [ -n "$existing" ] && [ "$existing" != "$expected" ]; then
        return 0  # true - has wrong emoji
    fi

    # check if there are emojis in the middle or unexpected patterns
    local clean=$(strip_emojis "$title")
    if [ "$clean" != "$title" ] && [ "$existing" = "" ]; then
        return 0  # true - has emojis but couldn't extract them properly
    fi

    return 1  # false - no wrong emoji
}

fix_issue() {
    local num="$1"

    local data=$(gh issue view "$num" --json title,labels --jq '[.title, (.labels | map(.name) | join(","))] | @tsv' 2>/dev/null)
    if [ -z "$data" ]; then
        echo "#$num not found"
        return
    fi

    local title=$(echo "$data" | cut -f1)
    local labels=$(echo "$data" | cut -f2)

    local expected=$(get_expected "$labels")
    local existing=$(get_existing "$title")
    local clean=$(strip_emojis "$title")

    # skip if already correct
    if [ "$existing" = "$expected" ]; then
        return
    fi

    # handle wrong emojis - strip all emojis and rebuild
    if has_wrong_emoji "$title" "$expected"; then
        local new="$expected $clean"
        echo "#$num: $title"
        echo "  wrong emoji(s) found, fixing -> $new"
    else
        # missing emojis only
        local new="$expected $clean"
        echo "#$num: $title"
        echo "  -> $new"
    fi

    if [ "$DRY_RUN" = false ]; then
        gh issue edit "$num" --title "$new" >/dev/null
        echo "  updated"
    fi
}

# parse args
for arg in "$@"; do
    case "$arg" in
        --dry-run) DRY_RUN=true ;;
        [0-9]*) single="$arg" ;;
    esac
done

echo "fixing issue emojis..."
[ "$DRY_RUN" = true ] && echo "(dry run)"

if [ -n "$single" ]; then
    fix_issue "$single"
else
    for num in $(gh issue list --state open --json number --jq '.[].number'); do
        fix_issue "$num"
    done
fi

echo "done"
