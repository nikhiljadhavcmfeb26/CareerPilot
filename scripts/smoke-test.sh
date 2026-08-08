#!/usr/bin/env bash
# CareerPilot end-to-end smoke test.
#
# Run this AFTER every service and the frontend are up. It exercises the paths
# that the release changes touched: the three logins, the admin module, role
# isolation, and the Gemini configuration.
#
# Requires: curl, jq.
# Usage:  ./scripts/smoke-test.sh [gateway-url]

set -uo pipefail

GATEWAY="${1:-http://localhost:8080}"
API="$GATEWAY/api"

ADMIN_EMAIL="${ADMIN_EMAIL:-admin@careerpilot.com}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-CareerPilot@123}"

STAMP="$(date +%s)"
SEEKER_EMAIL="seeker.$STAMP@example.com"
EMPLOYER_EMAIL="employer.$STAMP@example.com"
PASSWORD='Test@12345'

PASS=0; FAIL=0; SKIP=0

green() { printf '\033[32m%s\033[0m\n' "$1"; }
red()   { printf '\033[31m%s\033[0m\n' "$1"; }
yellow(){ printf '\033[33m%s\033[0m\n' "$1"; }

need() {
  command -v "$1" >/dev/null 2>&1 || { red "Missing dependency: $1"; exit 1; }
}
need curl
need jq

# check <name> <expected-http-status> <actual-http-status> [detail]
check() {
  if [ "$2" = "$3" ]; then
    green "  PASS  $1"
    PASS=$((PASS+1))
  else
    red   "  FAIL  $1 (expected HTTP $2, got $3)${4:+ - $4}"
    FAIL=$((FAIL+1))
  fi
}

skip() { yellow "  SKIP  $1${2:+ - $2}"; SKIP=$((SKIP+1)); }

# request <METHOD> <path> [json-body] [bearer-token]
# Writes the response body to $BODY and echoes the status code.
request() {
  local method="$1" path="$2" body="${3:-}" token="${4:-}"
  local args=(-s -o /tmp/cp_smoke_body -w '%{http_code}' -X "$method" "$API$path"
              -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$body" ]  && args+=(-d "$body")
  local code
  code="$(curl "${args[@]}")"
  BODY="$(cat /tmp/cp_smoke_body)"
  echo "$code"
}

echo
echo "CareerPilot smoke test against $GATEWAY"
echo "======================================================================"

# ---------------------------------------------------------------- infra ----
echo
echo "Infrastructure"
code="$(curl -s -o /dev/null -w '%{http_code}' "$GATEWAY/actuator/health")"
check "API Gateway is reachable" 200 "$code"
code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8761/)"
check "Eureka dashboard is reachable" 200 "$code"
code="$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8888/actuator/health)"
check "Config Server is reachable" 200 "$code"

# ------------------------------------------------------- authentication ----
echo
echo "Authentication"

code="$(request POST /auth/register "{\"email\":\"$SEEKER_EMAIL\",\"password\":\"$PASSWORD\",\"firstName\":\"Smoke\",\"lastName\":\"Seeker\",\"phone\":\"9990000001\",\"role\":\"JobSeeker\"}")"
check "Job seeker registration" 200 "$code" "$BODY"
SEEKER_TOKEN="$(echo "$BODY" | jq -r '.data.accessToken // empty')"

code="$(request POST /auth/register "{\"email\":\"$EMPLOYER_EMAIL\",\"password\":\"$PASSWORD\",\"firstName\":\"Smoke\",\"lastName\":\"Employer\",\"phone\":\"9990000002\",\"role\":\"Employer\"}")"
check "Employer registration" 200 "$code" "$BODY"
EMPLOYER_TOKEN="$(echo "$BODY" | jq -r '.data.accessToken // empty')"

code="$(request POST /auth/register "{\"email\":\"evil.$STAMP@example.com\",\"password\":\"$PASSWORD\",\"firstName\":\"E\",\"lastName\":\"V\",\"phone\":\"9990000003\",\"role\":\"Admin\"}")"
check "Self-registering as Admin is refused" 400 "$code" "$BODY"

code="$(request POST /auth/login "{\"email\":\"$SEEKER_EMAIL\",\"password\":\"$PASSWORD\"}")"
check "Job seeker login" 200 "$code"
SEEKER_TOKEN="$(echo "$BODY" | jq -r '.data.accessToken // empty')"

code="$(request POST /auth/login "{\"email\":\"$EMPLOYER_EMAIL\",\"password\":\"$PASSWORD\"}")"
check "Employer login" 200 "$code"
EMPLOYER_TOKEN="$(echo "$BODY" | jq -r '.data.accessToken // empty')"

code="$(request POST /auth/login "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")"
check "Admin login" 200 "$code" "$BODY"
ADMIN_TOKEN="$(echo "$BODY" | jq -r '.data.accessToken // empty')"
ADMIN_ROLE="$(echo "$BODY" | jq -r '.data.user.role // empty')"
check "Admin token carries the Admin role" "Admin" "$ADMIN_ROLE"

code="$(request POST /auth/login "{\"email\":\"$SEEKER_EMAIL\",\"password\":\"wrong-password\"}")"
check "Wrong password is rejected" 401 "$code"

# ------------------------------------------------------------ job flow ----
echo
echo "Job flow"
code="$(request GET /jobs)"
check "Public job search needs no token" 200 "$code"

code="$(request POST /jobs '{"title":"Smoke Test Engineer","description":"d","requirements":"r","location":"Remote","jobType":0,"experienceLevel":0,"salaryMin":1,"salaryMax":2}' "$EMPLOYER_TOKEN")"
if [ "$code" = "200" ]; then
  green "  PASS  Employer can create a job"; PASS=$((PASS+1))
  JOB_ID="$(echo "$BODY" | jq -r '.data.id')"
elif echo "$BODY" | grep -q "Register your company first"; then
  skip "Employer job creation" "employer has no company yet (expected on a fresh run)"
  JOB_ID=""
else
  red "  FAIL  Employer can create a job (HTTP $code) - $BODY"; FAIL=$((FAIL+1)); JOB_ID=""
fi

code="$(request POST /jobs '{"title":"x","description":"d","requirements":"r","location":"l","jobType":0,"experienceLevel":0}' "$SEEKER_TOKEN")"
check "Job seeker cannot create a job" 403 "$code"

# ------------------------------------------------------------ admin -------
echo
echo "Admin module"
code="$(request GET /admin/stats '' "$ADMIN_TOKEN")"
check "Admin dashboard stats" 200 "$code" "$BODY"

code="$(request GET /admin/users '' "$ADMIN_TOKEN")"
check "Admin can list users" 200 "$code"
TARGET_ID="$(echo "$BODY" | jq -r --arg e "$SEEKER_EMAIL" '.data[] | select(.email==$e) | .id')"

code="$(request GET '/admin/users?role=Employer' '' "$ADMIN_TOKEN")"
check "Admin user list accepts a role filter" 200 "$code"

if [ -n "$TARGET_ID" ]; then
  code="$(request PUT "/admin/users/$TARGET_ID/deactivate" '' "$ADMIN_TOKEN")"
  check "Admin can deactivate a user" 200 "$code"
  code="$(request POST /auth/login "{\"email\":\"$SEEKER_EMAIL\",\"password\":\"$PASSWORD\"}")"
  check "Deactivated user cannot log in" 401 "$code"
  code="$(request PUT "/admin/users/$TARGET_ID/activate" '' "$ADMIN_TOKEN")"
  check "Admin can reactivate a user" 200 "$code"

  code="$(request PUT "/admin/users/$TARGET_ID/block" '{"reason":"smoke test"}' "$ADMIN_TOKEN")"
  check "Admin can block a user" 200 "$code"
  code="$(request POST /auth/login "{\"email\":\"$SEEKER_EMAIL\",\"password\":\"$PASSWORD\"}")"
  check "Blocked user cannot log in" 401 "$code"
  code="$(request PUT "/admin/users/$TARGET_ID/unblock" '' "$ADMIN_TOKEN")"
  check "Admin can unblock a user" 200 "$code"

  code="$(request PUT "/admin/users/$TARGET_ID/ai/disable" '' "$ADMIN_TOKEN")"
  check "Admin can revoke a user's AI access" 200 "$code"
  code="$(request PUT "/admin/users/$TARGET_ID/ai/enable" '' "$ADMIN_TOKEN")"
  check "Admin can restore a user's AI access" 200 "$code"
else
  skip "Admin user lifecycle checks" "could not resolve the test seeker's id"
fi

ADMIN_ID="$(request GET /users/me '' "$ADMIN_TOKEN" >/dev/null; echo "$BODY" | jq -r '.data.id')"
if [ -n "$ADMIN_ID" ] && [ "$ADMIN_ID" != "null" ]; then
  code="$(request PUT "/admin/users/$ADMIN_ID/deactivate" '' "$ADMIN_TOKEN")"
  check "Admin cannot deactivate their own account" 400 "$code"
fi

code="$(request GET /admin/subscriptions '' "$ADMIN_TOKEN")"
check "Admin can list subscriptions" 200 "$code"

code="$(request GET /admin/ai-settings '' "$ADMIN_TOKEN")"
check "Admin can read AI settings" 200 "$code"
SETTINGS="$BODY"

code="$(request GET /applications/admin/all '' "$ADMIN_TOKEN")"
check "Admin can monitor all applications" 200 "$code"

code="$(request GET /jobs/admin/all '' "$ADMIN_TOKEN")"
check "Admin can list all jobs" 200 "$code"

code="$(request GET /companies/admin/all '' "$ADMIN_TOKEN")"
check "Admin can list all employers" 200 "$code"

# ------------------------------------------------- role isolation ---------
echo
echo "Role isolation"
for path in /admin/stats /admin/users /admin/subscriptions /admin/ai-settings; do
  code="$(request GET "$path" '' "$SEEKER_TOKEN")"
  check "Job seeker is refused GET $path" 403 "$code"
  code="$(request GET "$path" '' "$EMPLOYER_TOKEN")"
  check "Employer is refused GET $path" 403 "$code"
done

code="$(request GET /admin/stats)"
check "Anonymous is refused GET /admin/stats" 401 "$code"

code="$(request GET /jobs/admin/all '' "$EMPLOYER_TOKEN")"
check "Employer is refused GET /jobs/admin/all" 403 "$code"

# --------------------------------------------------------------- AI -------
echo
echo "AI features"
if [ -z "${GEMINI_API_KEY:-}" ]; then
  skip "Gemini key check" "GEMINI_API_KEY is not exported in this shell"
else
  code="$(curl -s -o /tmp/cp_gemini -w '%{http_code}' \
    "https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL:-gemini-3.5-flash}:generateContent" \
    -H "x-goog-api-key: $GEMINI_API_KEY" -H 'Content-Type: application/json' \
    -d '{"contents":[{"role":"user","parts":[{"text":"Reply with the single word OK"}]}]}')"
  check "Gemini accepts the configured key (x-goog-api-key header)" 200 "$code" "$(head -c 300 /tmp/cp_gemini)"
fi

# Without Premium this must be refused with 400, not 401/403/500 - that proves
# the gate is reached and the endpoint itself is wired correctly.
code="$(request POST /ai/resume-feedback '' "$SEEKER_TOKEN")"
if [ "$code" = "400" ]; then
  green "  PASS  AI resume feedback is gated (HTTP 400: $(echo "$BODY" | jq -r '.message'))"; PASS=$((PASS+1))
else
  red "  FAIL  AI resume feedback returned HTTP $code - $BODY"; FAIL=$((FAIL+1))
fi

code="$(request POST /ai/candidate-screening '{"jobId":1}' "$SEEKER_TOKEN")"
check "Job seeker cannot run candidate screening" 403 "$code"

# Master switch: turn AI off, confirm it is refused, turn it back on.
if [ -n "${SETTINGS:-}" ]; then
  OFF="$(echo "$SETTINGS" | jq -c '.data | .aiEnabled=false')"
  ON="$(echo "$SETTINGS"  | jq -c '.data')"
  code="$(request PUT /admin/ai-settings "$OFF" "$ADMIN_TOKEN")"
  check "Admin can switch AI off platform-wide" 200 "$code"
  code="$(request POST /ai/resume-feedback '' "$SEEKER_TOKEN")"
  if [ "$code" = "400" ] && echo "$BODY" | grep -qi "disabled by the administrator"; then
    green "  PASS  AI master switch is enforced"; PASS=$((PASS+1))
  else
    red "  FAIL  AI master switch not enforced (HTTP $code) - $BODY"; FAIL=$((FAIL+1))
  fi
  code="$(request PUT /admin/ai-settings "$ON" "$ADMIN_TOKEN")"
  check "AI settings restored" 200 "$code"
fi

# ------------------------------------------------------------ summary -----
echo
echo "======================================================================"
echo "PASS: $PASS   FAIL: $FAIL   SKIP: $SKIP"
[ "$FAIL" -eq 0 ] && green "Smoke test passed." || red "Smoke test FAILED."
exit $(( FAIL > 0 ? 1 : 0 ))
