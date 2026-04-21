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

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

env_value() {
  key="$1"

  awk -v search_key="$key" '
    /^[[:space:]]*#/ { next }
    /^[[:space:]]*$/ { next }
    {
      line = $0
      sub(/\r$/, "", line)
      pos = index(line, "=")
      if (pos == 0) {
        next
      }

      name = substr(line, 1, pos - 1)
      gsub(/^[[:space:]]+|[[:space:]]+$/, "", name)
      if (name != search_key) {
        next
      }

      value = substr(line, pos + 1)
      print value
      exit
    }
  ' "$ENV_FILE"
}

env_value_or_default() {
  key="$1"
  default_value="${2:-}"
  value="$(env_value "$key")"

  if [ -n "$value" ]; then
    printf '%s\n' "$value"
  else
    printf '%s\n' "$default_value"
  fi
}
