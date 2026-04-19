#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
PASSWORD="${SMOKE_PASSWORD:-SmokePass123}"
TS="$(date +%s)"
USERNAME="${SMOKE_USERNAME:-docker-smoke-${TS}}"
EMAIL="${SMOKE_EMAIL:-${USERNAME}@uniquindio.edu.co}"
DESCRIPTION="${SMOKE_DESCRIPTION:-Necesito validar el flujo dockerizado completo ${TS}}"

tmp_dir="$(mktemp -d)"
cleanup() {
  rm -rf "${tmp_dir}"
}
trap cleanup EXIT

require() {
  if ! command -v "$1" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$1" >&2
    exit 1
  fi
}

require curl
require python3

echo "[1/6] Checking health endpoint"
health_code="$(curl -s -o "${tmp_dir}/health.json" -w "%{http_code}" "${BASE_URL}/actuator/health")"
test "${health_code}" = "200"

echo "[2/6] Checking Swagger endpoint"
swagger_code="$(curl -s -o /dev/null -w "%{http_code}" -I "${BASE_URL}/swagger-ui.html")"
case "${swagger_code}" in
  200|302) ;;
  *)
    printf 'Unexpected swagger status: %s\n' "${swagger_code}" >&2
    exit 1
    ;;
esac

echo "[3/6] Registering smoke user ${USERNAME}"
register_code="$(curl -s -o "${tmp_dir}/register.json" -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/auth/register" \
  -H "Content-Type: application/json" \
  --data "{\"username\":\"${USERNAME}\",\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"firstName\":\"Docker\",\"lastName\":\"Smoke\",\"identification\":\"ID-${TS}\"}")"
test "${register_code}" = "201"

echo "[4/6] Logging in smoke user (canonical identifier)"
login_code="$(curl -s -o "${tmp_dir}/login.json" -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  --data "{\"identifier\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"
test "${login_code}" = "200"
TOKEN="$(python3 - <<'PY' "${tmp_dir}/login.json"
import json, sys
print(json.load(open(sys.argv[1]))['token'])
PY
)"

echo "[4b/6] Alias compatibility check (deprecated username field — backward compat)"
alias_login_code="$(curl -s -o "${tmp_dir}/login-alias.json" -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  --data "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\"}")"
test "${alias_login_code}" = "200"

echo "[5/6] Discovering first available catalog IDs"
catalog_payload="$(
  request_types_code=$(curl -s -o "${tmp_dir}/request-types.json" -w "%{http_code}" \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}/api/v1/catalogs/request-types")
  origin_channels_code=$(curl -s -o "${tmp_dir}/origin-channels.json" -w "%{http_code}" \
    -H "Authorization: Bearer ${TOKEN}" \
    "${BASE_URL}/api/v1/catalogs/origin-channels")
  test "${request_types_code}" = "200"
  test "${origin_channels_code}" = "200"
  python3 - <<'PY' "${tmp_dir}/request-types.json" "${tmp_dir}/origin-channels.json"
import json, sys
request_types = json.load(open(sys.argv[1]))
origin_channels = json.load(open(sys.argv[2]))
if not request_types:
    raise SystemExit('No request types available for smoke test')
if not origin_channels:
    raise SystemExit('No origin channels available for smoke test')
print(request_types[0]['id'])
print(origin_channels[0]['id'])
PY
)"
REQUEST_TYPE_ID="$(printf '%s\n' "${catalog_payload}" | sed -n '1p')"
ORIGIN_CHANNEL_ID="$(printf '%s\n' "${catalog_payload}" | sed -n '2p')"

echo "[6/6] Creating and listing a request"
create_code="$(curl -s -o "${tmp_dir}/create.json" -w "%{http_code}" \
  -X POST "${BASE_URL}/api/v1/requests" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  --data "{\"requestTypeId\":${REQUEST_TYPE_ID},\"originChannelId\":${ORIGIN_CHANNEL_ID},\"description\":\"${DESCRIPTION}\",\"deadline\":\"2026-05-15\"}")"
test "${create_code}" = "201"

list_code="$(curl -s -o "${tmp_dir}/requests.json" -w "%{http_code}" \
  -H "Authorization: Bearer ${TOKEN}" \
  "${BASE_URL}/api/v1/requests")"
test "${list_code}" = "200"
python3 - <<'PY' "${tmp_dir}/requests.json" "${DESCRIPTION}"
import json, sys
payload = json.load(open(sys.argv[1]))
description = sys.argv[2]
content = payload.get('content', []) if isinstance(payload, dict) else payload
if not any(item.get('description') == description for item in content):
    raise SystemExit('Created request not found in listing')
PY

echo "Docker smoke check OK"
echo "USERNAME=${USERNAME}"
echo "EMAIL=${EMAIL}"
echo "DESCRIPTION=${DESCRIPTION}"
