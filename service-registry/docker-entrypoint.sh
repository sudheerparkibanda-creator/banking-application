#!/bin/sh
set -eu

CONFIG_SERVER_URL="${CONFIG_SERVER_URL:-http://config-server:8888}"
CONFIG_HEALTH_URL="${CONFIG_HEALTH_URL:-${CONFIG_SERVER_URL%/}/actuator/health}"
MAX_ATTEMPTS="${CONFIG_SERVER_MAX_ATTEMPTS:-60}"
SLEEP_SECONDS="${CONFIG_SERVER_RETRY_DELAY_SECONDS:-5}"

printf 'Waiting for Config Server at %s\n' "$CONFIG_HEALTH_URL"

attempt=1
while [ "$attempt" -le "$MAX_ATTEMPTS" ]; do
  if curl -fsS "$CONFIG_HEALTH_URL" >/dev/null 2>&1; then
    printf 'Config Server is available. Starting service-registry.\n'
    exec java -jar app.jar
  fi

  printf 'Attempt %s/%s failed. Retrying in %s seconds...\n' "$attempt" "$MAX_ATTEMPTS" "$SLEEP_SECONDS"
  attempt=$((attempt + 1))
  sleep "$SLEEP_SECONDS"
done

printf 'Config Server did not become available after %s attempts.\n' "$MAX_ATTEMPTS" >&2
exit 1

