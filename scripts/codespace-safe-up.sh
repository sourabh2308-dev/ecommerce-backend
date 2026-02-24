#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/codespace-safe-up.sh            # start core stack (no rebuild)
#   ./scripts/codespace-safe-up.sh --build    # rebuild first with low parallelism
#   ./scripts/codespace-safe-up.sh --full     # include frontend/monitoring profile
#   ./scripts/codespace-safe-up.sh --build --full

BUILD=0
FULL=0

for arg in "$@"; do
  case "$arg" in
    --build) BUILD=1 ;;
    --full) FULL=1 ;;
    *)
      echo "Unknown argument: $arg"
      exit 1
      ;;
  esac
done

export COMPOSE_PARALLEL_LIMIT=2

PROFILE_ARGS=()
if [[ "$FULL" -eq 1 ]]; then
  PROFILE_ARGS+=(--profile full)
fi

if [[ "$BUILD" -eq 1 ]]; then
  docker compose "${PROFILE_ARGS[@]}" build
fi

# Clean up any lingering containers from previous profile runs (e.g. --profile full)
docker compose --profile full down --remove-orphans >/dev/null 2>&1 || true

docker compose "${PROFILE_ARGS[@]}" up -d --remove-orphans

echo "Compose started successfully."
docker compose "${PROFILE_ARGS[@]}" ps
