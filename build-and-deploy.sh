#!/usr/bin/env bash
set -euo pipefail

# Unified build-and-deploy script
# Usage: ./build-and-deploy.sh [--build-only]

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

BUILD_ONLY=false
if [ "${1:-}" = "--build-only" ]; then
  BUILD_ONLY=true
fi

# Ensure docker is available
if ! command -v docker >/dev/null 2>&1; then
  echo "docker not found. Please install Docker / Docker Desktop and ensure it's running."
  exit 1
fi

# Prefer docker compose plugin
DC_COMMAND="docker compose"

echo "1/4 - Bringing down any existing compose stack (if present)..."
$DC_COMMAND down --remove-orphans || true

echo "2/4 - Building images (backend + frontend)..."
$DC_COMMAND build --pull

if [ "$BUILD_ONLY" = true ]; then
  echo "Build finished. Exiting because --build-only was specified."
  exit 0
fi

echo "3/4 - Starting services..."
$DC_COMMAND up -d

# Wait for DB to become healthy (use the compose service name 'db')
echo "Waiting for Postgres DB to report healthy (timeout 120s)..."
DB_CONTAINER=$($DC_COMMAND ps -q db || true)
if [ -n "$DB_CONTAINER" ]; then
  SECS=0
  while [ $SECS -lt 120 ]; do
    status=$(docker inspect --format='{{.State.Health.Status}}' "$DB_CONTAINER" 2>/dev/null || true)
    if [ "$status" = "healthy" ]; then
      echo "DB is healthy"
      break
    fi
    echo "DB health: ${status:-unknown}; waiting..."
    sleep 2
    SECS=$((SECS+2))
  done
  if [ $SECS -ge 120 ]; then
    echo "Warning: DB did not report healthy within timeout; continuing anyway."
  fi
else
  echo "Warning: could not determine db container id from 'docker compose ps -q db' - skipping health check."
fi

echo "4/4 - Deployment complete. To follow logs run: docker compose logs -f"

docker compose logs -f

exit 0
