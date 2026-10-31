#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${APP_IMAGE:?Provide APP_IMAGE, e.g. ghcr.io/owner/repo:sha-...}"
export APP_IMAGE
[[ -f .env ]] || { echo 'Missing deploy/.env (DB_PASSWORD)' >&2; exit 1; }
docker compose --env-file .env pull
docker compose --env-file .env up -d --remove-orphans
for attempt in $(seq 1 30); do
  if curl --fail --silent http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'; then echo 'Deployment healthy'; exit 0; fi
  sleep 2
done
echo 'Deployment health check failed' >&2
docker compose --env-file .env logs app --tail=80
exit 1
