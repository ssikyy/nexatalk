#!/usr/bin/env sh

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
. "$SCRIPT_DIR/common.sh"

SERVICE="${1:-backend}"
TAIL_LINES="${TAIL_LINES:-200}"

compose logs --tail "$TAIL_LINES" -f "$SERVICE"
