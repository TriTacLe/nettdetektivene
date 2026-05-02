#!/bin/bash
# Full-reset deploy. Stops the stack, drops the MySQL volume so the
# DataSeeder reseeds on next boot, pulls fresh images, brings everything
# back up. The uploads volume is preserved by default; set
# WIPE_UPLOADS=true to drop that one too.

set -e

COMPOSE_FILE="compose.prod.yaml"
WIPE_UPLOADS="${WIPE_UPLOADS:-false}"

echo "Stopping stack and removing containers + orphans..."
docker compose -f "$COMPOSE_FILE" down --remove-orphans

echo "Removing mysql volume (DataSeeder will reseed on boot)..."
docker volume rm -f nettdetektivene_mysql_data 2>/dev/null || true

if [ "$WIPE_UPLOADS" = "true" ]; then
  echo "Removing uploads volume..."
  docker volume rm -f nettdetektivene_uploads 2>/dev/null || true
else
  echo "Preserving uploads volume."
fi

echo "Pulling latest images..."
docker compose -f "$COMPOSE_FILE" pull

echo "Starting services..."
docker compose -f "$COMPOSE_FILE" up -d

echo "Status:"
docker compose -f "$COMPOSE_FILE" ps

echo "Clean deploy complete."
