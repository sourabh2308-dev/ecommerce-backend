#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/codespace-safe-down.sh
#   ./scripts/codespace-safe-down.sh --volumes

VOLUMES=0
if [[ "${1:-}" == "--volumes" ]]; then
  VOLUMES=1
fi

if [[ "$VOLUMES" -eq 1 ]]; then
  docker compose down --remove-orphans -v
else
  docker compose down --remove-orphans
fi

echo "Compose stopped."
