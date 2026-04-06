#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SMOKE_SCRIPT="${PROJECT_DIR}/scripts/docker-smoke.sh"
COMPOSE_CMD="${COMPOSE_CMD:-docker-compose}"
DB_CONTAINER="${DB_CONTAINER:-triage-mariadb}"

if ! command -v "${COMPOSE_CMD}" >/dev/null 2>&1; then
  printf 'Missing compose command: %s\n' "${COMPOSE_CMD}" >&2
  exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
  printf 'Missing required command: docker\n' >&2
  exit 1
fi

output_file="$(mktemp)"
trap 'rm -f "${output_file}"' EXIT

"${SMOKE_SCRIPT}" | tee "${output_file}"

username="$(grep '^USERNAME=' "${output_file}" | cut -d'=' -f2-)"
description="$(grep '^DESCRIPTION=' "${output_file}" | cut -d'=' -f2-)"

if [[ -z "${username}" || -z "${description}" ]]; then
  printf 'Could not extract smoke artifacts from docker-smoke.sh output\n' >&2
  exit 1
fi

echo "[persistence] Restarting app and database containers"
"${COMPOSE_CMD}" restart mariadb app >/dev/null

echo "[persistence] Waiting for app health"
for _ in {1..30}; do
  if curl -s "http://localhost:8080/actuator/health" | grep -q '"status":"UP"'; then
    break
  fi
  sleep 2
done

echo "[persistence] Verifying request is still present in MariaDB volume"
docker exec "${DB_CONTAINER}" mariadb -utriage_user -ptriage_secret -D triage_db \
  -e "SELECT COUNT(*) AS matches FROM academic_requests ar JOIN users u ON u.id = ar.applicant_id WHERE u.username = '${username}' AND ar.description = '${description}';" \
  | tee "${output_file}.sql"

if ! grep -q '^1$' "${output_file}.sql"; then
  printf 'Persisted request was not found after restart\n' >&2
  exit 1
fi

echo "Docker persistence check OK"
