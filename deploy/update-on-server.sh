#!/usr/bin/env sh

set -eu

APP_DIR="${APP_DIR:-/opt/nexatalk}"
ENV_FILE="${ENV_FILE:-deploy/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

cd "$APP_DIR"

git fetch --all --prune
git pull --ff-only

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build
docker image prune -f
