#!/usr/bin/env sh

set -eu

APP_DIR="${APP_DIR:-/opt/nexatalk}"
ENV_FILE="${ENV_FILE:-deploy/.env.prod}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"

cd "$APP_DIR"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $APP_DIR/$ENV_FILE" >&2
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}
