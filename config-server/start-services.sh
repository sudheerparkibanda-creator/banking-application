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
echo "Removing stale containers with fixed names, if any..."
for container in $CONTAINERS; do
  docker rm -f "$container" >/dev/null 2>&1 || true
done

echo "Cleaning previous compose resources..."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" down --remove-orphans >/dev/null 2>&1 || true

echo "Building and starting banking microservices..."
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" up -d --build

echo
echo "Current container status:"
docker compose -p "$PROJECT_NAME" -f "$COMPOSE_FILE" ps

echo
echo "Endpoints after startup:"
echo "  Config Server    -> http://localhost:8888/actuator/health"
echo "  Service Registry -> http://localhost:8761"
echo "  API Gateway      -> http://localhost:8080/actuator/health"
echo
echo "Tip: for local non-Docker service-registry runs, set CONFIG_SERVER_URL=http://localhost:8888"

