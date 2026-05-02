#!/bin/bash
# Full-reset deploy. Stops the stack, drops every named volume the compose
# project owns (so DataSeeder reseeds on next boot and uploads are wiped),
# force-removes any leftover named volumes whose names match this project,
# pulls fresh images, brings everything back up.

set -e

COMPOSE_FILE="compose.prod.yaml"

echo "Stopping stack, removing containers, volumes, and orphans..."
docker compose -f "$COMPOSE_FILE" down -v --remove-orphans

echo "Removing any leftover named volumes for this project..."
docker volume ls --format '{{.Name}}' \
  | grep -E 'nettdetektivene_(mysql_data|uploads)$' \
  | xargs -r docker volume rm -f

echo "Pulling latest images..."
docker compose -f "$COMPOSE_FILE" pull

echo "Starting services..."
docker compose -f "$COMPOSE_FILE" up -d

echo "Status:"
docker compose -f "$COMPOSE_FILE" ps

echo "Clean deploy complete."
