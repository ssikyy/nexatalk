#!/usr/bin/env sh

set -eu

APP_DIR="${APP_DIR:-/opt/nexatalk}"
ENV_FILE="${ENV_FILE:-deploy/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
APP_BRANCH="${APP_BRANCH:-}"

cd "$APP_DIR"

git fetch --all --prune

if [ -n "$APP_BRANCH" ]; then
  git checkout "$APP_BRANCH"
  git pull --ff-only origin "$APP_BRANCH"
else
  git pull --ff-only
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --build
docker image prune -f
