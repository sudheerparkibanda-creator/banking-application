#!/usr/bin/env bash
# =============================================================================
# build.sh  –  Build and deploy the banking-application on Ubuntu
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"

# ── Colour helpers ─────────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info()    { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── Prerequisites check ────────────────────────────────────────────────────────
check_prerequisites() {
  info "Checking prerequisites..."
  command -v docker  >/dev/null 2>&1 || error "Docker is not installed. Install it from https://docs.docker.com/get-docker/"
  command -v docker compose version >/dev/null 2>&1 || \
    docker-compose version >/dev/null 2>&1 || \
    error "Docker Compose is not installed."
  info "All prerequisites satisfied."
}

# ── Detect compose command ─────────────────────────────────────────────────────
compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    echo "docker compose"
  else
    echo "docker-compose"
  fi
}

# ── Build & start ──────────────────────────────────────────────────────────────
build_and_start() {
  local cmd
  cmd="$(compose_cmd)"
  info "Building all Docker images (this may take a few minutes on first run)..."
  $cmd -f "$COMPOSE_FILE" build --no-cache

  info "Starting all services..."
  $cmd -f "$COMPOSE_FILE" up -d

  info "Services started. Waiting for health checks..."
  sleep 5
  $cmd -f "$COMPOSE_FILE" ps
}

# ── Tear down ──────────────────────────────────────────────────────────────────
tear_down() {
  local cmd
  cmd="$(compose_cmd)"
  warn "Stopping and removing all containers..."
  $cmd -f "$COMPOSE_FILE" down --remove-orphans
  info "Done."
}

# ── Logs ──────────────────────────────────────────────────────────────────────
show_logs() {
  local cmd
  cmd="$(compose_cmd)"
  $cmd -f "$COMPOSE_FILE" logs -f
}

# ── Main ──────────────────────────────────────────────────────────────────────
usage() {
  cat <<EOF
Usage: $0 [COMMAND]

Commands:
  up      Build images and start all services   (default)
  down    Stop and remove all containers
  logs    Follow logs for all services
  help    Show this help message
EOF
}

check_prerequisites

case "${1:-up}" in
  up)    build_and_start ;;
  down)  tear_down ;;
  logs)  show_logs ;;
  help)  usage ;;
  *)     usage; error "Unknown command: $1" ;;
esac

