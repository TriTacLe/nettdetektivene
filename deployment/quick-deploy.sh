#!/bin/bash
# Quick deploy script for CI/CD

set -e

echo "Deploying Nettdetektivene..."

DEPLOY_DIR="/home/orbit/services.devops/nettdetektivene"

cd "$DEPLOY_DIR"

echo "Pulling latest images..."
docker compose -f compose.prod.yaml pull

echo "Starting services..."
docker compose -f compose.prod.yaml up -d

echo "Checking status..."
docker compose -f compose.prod.yaml ps

echo "Deployment complete!"