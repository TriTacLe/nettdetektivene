#!/bin/bash

# Fixes broken image paths in existing task content to use only images that exist in public/
# Run this once after the image rename/reorganization.

GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m'

if ! docker ps --format '{{.Names}}' | grep -q '^nd-db$'; then
    echo -e "${RED}[FAIL] nd-db container is not running. Run: docker-compose up -d${NC}"
    exit 1
fi

run_sql() {
    docker exec nd-db mysql -uroot -proot nd-db -e "$1" 2>/dev/null
}

echo -e "${BLUE}[INFO]${NC} Fixing nyhetskvartalet image paths..."

run_sql "
UPDATE task SET content =
  REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(content,
    '/news/fake-article-1.jpg', '/news/real-article-1.jpg'),
    '/news/fake-article-2.jpg', '/news/real-article-1.jpg'),
    '/news/fake-article-3.jpg', '/news/real-article-1.jpg'),
    '/news/real-article-2.jpg', '/news/real-article-1.jpg'),
    '/news/real-article-3.jpg', '/news/real-article-1.jpg')
WHERE stop_location = 'nyhetskvartalet';
"

ROWS=$(docker exec nd-db mysql -uroot -proot nd-db -se "SELECT ROW_COUNT();" 2>/dev/null)
echo -e "${GREEN}[OK]${NC} Updated $ROWS nyhetskvartalet task(s)"

echo -e "${BLUE}[INFO]${NC} Fixing fotografen image paths..."

run_sql "
UPDATE task SET content =
  REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(content,
    '/photos/real-photo-1.jpg', '/photos/ai-photo-1.jpg'),
    '/photos/real-photo-2.jpg', '/photos/ai-photo-1.jpg'),
    '/photos/real-photo-3.jpg', '/photos/ai-photo-1.jpg'),
    '/photos/manipulated-photo-1.jpg', '/photos/ai-photo-1.jpg'),
    '/photos/manipulated-photo-2.jpg', '/photos/ai-photo-1.jpg'),
    '/photos/manipulated-photo-3.jpg', '/photos/ai-photo-1.jpg')
WHERE stop_location = 'fotografen';
"

ROWS=$(docker exec nd-db mysql -uroot -proot nd-db -se "SELECT ROW_COUNT();" 2>/dev/null)
echo -e "${GREEN}[OK]${NC} Updated $ROWS fotografen task(s)"

echo ""
echo -e "${GREEN}Done.${NC}"
