#!/bin/bash
# Decommission script. Wipes the production deployment of Nettdetektivene
# from the host. Run when the project is being shut down for good.
#
# Stops + disables the systemd unit, removes its file and any wants/
# symlinks, drops the docker stack with all volumes, removes project
# images, removes the nginx site config, logs out of GHCR, and deletes
# the deployment directory itself.
#
# Designed to be streamed over ssh stdin so it does not depend on the
# script existing on the host. Idempotent: re-running on an already
# torn-down host is a no-op.

set -e

DEPLOY_DIR="/home/orbit/services.devops/nettdetektivene"
SERVICE_NAME="nettdetektivene"
SYSTEMD_UNIT="/etc/systemd/system/${SERVICE_NAME}.service"
NGINX_CONF="/etc/nginx/conf.d/nettdetektivene.conf"

echo "Stopping and disabling systemd unit (if present)..."
sudo -n systemctl stop "$SERVICE_NAME" 2>/dev/null || true
sudo -n systemctl disable "$SERVICE_NAME" 2>/dev/null || true
sudo -n rm -f "$SYSTEMD_UNIT"
# disable normally drops the wants/ symlink, but sweep stragglers in case
# the unit was Wants= referenced from elsewhere.
sudo -n find /etc/systemd/system -lname "*${SERVICE_NAME}.service" -delete 2>/dev/null || true
sudo -n systemctl daemon-reload
sudo -n systemctl reset-failed "$SERVICE_NAME" 2>/dev/null || true

echo "Stopping docker stack and removing volumes..."
if [ -f "$DEPLOY_DIR/compose.prod.yaml" ]; then
  (cd "$DEPLOY_DIR" && docker compose -f compose.prod.yaml down -v --remove-orphans) || true
fi

echo "Removing leftover named volumes for this project..."
docker volume ls --format '{{.Name}}' \
  | grep -E 'nettdetektivene_(mysql_data|uploads)$' \
  | xargs -r docker volume rm -f || true

echo "Removing project images..."
docker image ls --format '{{.Repository}}:{{.Tag}}' \
  | grep -E '^(ghcr\.io/orbitntnu/nettdetektivene|nettdetektivene[-_/]|backend:|frontend:)' \
  | xargs -r docker image rm -f || true

echo "Removing nginx site config (if present)..."
if [ -f "$NGINX_CONF" ]; then
  sudo -n rm -f "$NGINX_CONF"
  sudo -n nginx -t && sudo -n systemctl reload nginx || true
fi

echo "Logging out of GHCR..."
docker logout ghcr.io 2>/dev/null || true

echo "Removing deployment directory..."
cd /
sudo -n rm -rf "$DEPLOY_DIR"

echo "Teardown complete."
