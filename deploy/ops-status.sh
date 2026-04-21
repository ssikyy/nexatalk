#!/usr/bin/env sh

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

SITE_HOST="$(env_value SITE_ADDRESS)"
HEALTH_URL="${HEALTH_URL:-}"

if [ -z "$HEALTH_URL" ] && [ -n "$SITE_HOST" ]; then
  HEALTH_URL="https://${SITE_HOST}/health"
fi

echo "== Git =="
git rev-parse --short HEAD
git branch --show-current

echo
echo "== Containers =="
compose ps

echo
echo "== Health =="
if [ -z "$HEALTH_URL" ]; then
  echo "Health URL is not configured"
elif command -v curl >/dev/null 2>&1; then
  curl -fsS --max-time 10 "$HEALTH_URL" || echo "Health check failed: $HEALTH_URL"
else
  echo "curl is not installed"
fi

echo
echo "== Disk =="
df -h "$APP_DIR"

echo
echo "== Docker Disk Usage =="
docker system df
