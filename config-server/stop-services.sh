#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
PROJECT_NAME="banking-application"
CONTAINERS="config-server service-registry customer-service account-service api-gateway"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is not installed or not available in PATH." >&2
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not running. Start Docker and retry." >&2
  exit 1
fi

echo "Using compose file: $COMPOSE_FILE"
echo "Using project name: $PROJECT_NAME"
echo "Stopping banking microservices..."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down --remove-orphans || true

echo "Removing stale containers with fixed names, if any..."
for container in $CONTAINERS; do
  docker rm -f "$container" >/dev/null 2>&1 || true
done

echo
echo "Remaining compose status:"
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" ps || true

echo
echo "Banking microservices stopped."

