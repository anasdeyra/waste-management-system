#!/usr/bin/env bash
# One command from a fresh clone to a working, pre-seeded demo.
#
# Usage: tools/demo.sh [--reset] [-h|--help]
#   (no option)  start the backend and the frontend, wait until the MN01 demonstration
#                is loaded, print the URL and the accounts, then keep both running
#                (Ctrl-C stops them)
#   --reset      one-shot: wipe the demonstration data and rebuild it (D-055); accounts,
#                the MN01 import and evaluation runs are kept
#
# What it does, in order: checks JDK 21+ and Node 20.19+/22.12+, installs the frontend
# dependencies (`npm ci`) when frontend/node_modules is missing, starts the backend with the
# `dev` profile (creates the accounts, the MN01 import and the demonstration), waits for the
# demonstration report, then starts the frontend.
#
# Seeding is idempotent: running it again keeps the data and progress already in
# backend/data/. To start from scratch, stop everything and delete backend/data/.
#
# Environment: ASE_BACKEND_PORT (default 8080), ASE_FRONTEND_PORT (default 5173),
#              ASE_STARTUP_TIMEOUT (seconds to wait for the backend, default 600; the first
#              run downloads the Maven dependencies).
# Logs: backend/data/demo-backend.log and backend/data/demo-frontend.log.
set -euo pipefail
set -m # job control: every background job gets its own process group, killed as a unit

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_PORT="${ASE_BACKEND_PORT:-8080}"
FRONTEND_PORT="${ASE_FRONTEND_PORT:-5173}"
TIMEOUT="${ASE_STARTUP_TIMEOUT:-600}"
LOG_DIR="$ROOT/backend/data"
BACKEND_LOG="$LOG_DIR/demo-backend.log"
FRONTEND_LOG="$LOG_DIR/demo-frontend.log"
API="http://localhost:$BACKEND_PORT"
APP="http://localhost:$FRONTEND_PORT"

RESET=0
for arg in "$@"; do
  case "$arg" in
    --reset) RESET=1 ;;
    -h | --help) sed -n '2,22p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown argument: $arg (see --help)" >&2; exit 2 ;;
  esac
done

die() { echo "ERROR: $*" >&2; exit 1; }
step() { echo "==> $*"; }

# ---- prerequisites ---------------------------------------------------------------------

java_ok() {
  command -v java >/dev/null 2>&1 &&
    [ "$(java -version 2>&1 | sed -n '1s/.*"\([0-9][0-9]*\).*/\1/p')" -ge 21 ] 2>/dev/null
}

node_ok() {
  command -v node >/dev/null 2>&1 && command -v npm >/dev/null 2>&1 &&
    node -e 'const [a, b] = process.versions.node.split(".").map(Number);
             process.exit(a > 22 || (a === 22 && b >= 12) || (a === 20 && b >= 19) ? 0 : 1)'
}

port_in_use() {
  if command -v ss >/dev/null 2>&1; then
    ss -tln 2>/dev/null | grep -qE "[:.]$1\b"
  elif command -v lsof >/dev/null 2>&1; then
    lsof -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
  else
    return 1
  fi
}

command -v curl >/dev/null 2>&1 || die "curl is required."

if ! java_ok && [ -f "$HOME/.local/opt/ase-env.sh" ]; then
  # shellcheck disable=SC1091
  . "$HOME/.local/opt/ase-env.sh" # toolchain installed by tools/setup-toolchain.sh
fi
java_ok || die "JDK 21 or newer is required. Install it (e.g. Temurin 21) or run 'bash tools/setup-toolchain.sh' on Linux x86_64."
node_ok || die "Node.js 20.19+ or 22.12+ (with npm) is required. Install Node.js 22 LTS, e.g. with fnm or nvm."
for port in "$BACKEND_PORT" "$FRONTEND_PORT"; do
  if port_in_use "$port"; then
    die "Port $port is already in use. Stop the other process, or set ASE_BACKEND_PORT / ASE_FRONTEND_PORT."
  fi
done

# ---- process management ----------------------------------------------------------------

PIDS=()
cleanup() {
  trap - EXIT INT TERM
  local pid
  for pid in ${PIDS[@]+"${PIDS[@]}"}; do
    kill -- "-$pid" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

show_log_tail() { echo "--- last lines of $1 ---" >&2; tail -n 25 "$1" >&2 || true; }

# ---- frontend dependencies -------------------------------------------------------------

if [ ! -d "$ROOT/frontend/node_modules" ]; then
  step "Installing frontend dependencies (npm ci)"
  (cd "$ROOT/frontend" && npm ci --no-audit --no-fund) || die "npm ci failed."
fi

# ---- backend: accounts, MN01 import and demonstration are seeded by the dev profile -----

mkdir -p "$LOG_DIR"
BACKEND_ARGS="--server.port=$BACKEND_PORT"
if [ "$RESET" = 1 ]; then
  BACKEND_ARGS="$BACKEND_ARGS --wastems.demo.reset=true"
  step "Starting the backend with a one-shot demonstration reset"
else
  step "Starting the backend (first start: downloads dependencies, imports MN01, loads the demo)"
fi
(
  cd "$ROOT/backend"
  exec ./mvnw -B -q spring-boot:run \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.arguments="$BACKEND_ARGS"
) >"$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!
PIDS+=("$BACKEND_PID")

# The demonstration report exists only once the start-up load of this run has finished, so a
# 200 on it proves that the seeding completed (the HTTP port opens earlier).
demo_report() {
  local jar="$1" token
  : >"$jar"
  curl -sf -m 5 -c "$jar" -b "$jar" -o /dev/null "$API/api/auth/csrf" || return 1
  token="$(awk '$6=="XSRF-TOKEN"{print $7}' "$jar")"
  curl -sf -m 5 -c "$jar" -b "$jar" -o /dev/null \
    -H "X-XSRF-TOKEN: $token" -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"admin123"}' "$API/api/auth/login" || return 1
  curl -sf -m 10 -b "$jar" "$API/api/admin/demo/report"
}

JAR="$(mktemp)"
REPORT=""
STARTED=$SECONDS
NEXT_NOTE=$((SECONDS + 20))
while :; do
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    show_log_tail "$BACKEND_LOG"
    die "The backend stopped before the demonstration was loaded (full log: $BACKEND_LOG)."
  fi
  if REPORT="$(demo_report "$JAR")"; then break; fi
  if [ $((SECONDS - STARTED)) -ge "$TIMEOUT" ]; then
    show_log_tail "$BACKEND_LOG"
    die "Timed out after ${TIMEOUT}s waiting for the demonstration (full log: $BACKEND_LOG)."
  fi
  if [ "$SECONDS" -ge "$NEXT_NOTE" ]; then
    echo "    still waiting for the backend... ($((SECONDS - STARTED))s)"
    NEXT_NOTE=$((SECONDS + 20))
  fi
  sleep 2
done
rm -f "$JAR"
step "Demonstration loaded ($((SECONDS - STARTED))s)"

TOURS="$(printf '%s' "$REPORT" | grep -o '"tourId"' | wc -l | tr -d ' ')"
case "$REPORT" in
  *'"warnings":[]'*) ;;
  *) echo "    WARNING: the demonstration load reported warnings; see $BACKEND_LOG ('Demo load warning')." ;;
esac

# ---- frontend --------------------------------------------------------------------------

step "Starting the frontend"
(
  cd "$ROOT/frontend"
  API_PROXY_TARGET="$API" exec npm run dev -- --port "$FRONTEND_PORT" --strictPort
) >"$FRONTEND_LOG" 2>&1 &
FRONTEND_PID=$!
PIDS+=("$FRONTEND_PID")

STARTED=$SECONDS
until curl -sf -m 3 -o /dev/null "$APP/"; do
  kill -0 "$FRONTEND_PID" 2>/dev/null || { show_log_tail "$FRONTEND_LOG"; die "The frontend stopped (full log: $FRONTEND_LOG)."; }
  [ $((SECONDS - STARTED)) -lt 120 ] || { show_log_tail "$FRONTEND_LOG"; die "Timed out waiting for the frontend."; }
  sleep 1
done

cat <<EOF

WasteMS demo is ready
  App   $APP
  API   $API
  Logs  $BACKEND_LOG
        $FRONTEND_LOG

  Sign in (development accounts):
    planner / planner123   PLANNER  plan and review tours, dashboards (start here)
    admin   / admin123     ADMIN    data import, users, audit log, demo reset
    agent   / agent123     AGENT    crew view of assigned tours

  Loaded: the real MN01 baskets and depot plus synthetic demo operations ($TOURS tours covering
  every lifecycle state). Walkthrough: README.md section 4.6.
  Back to the pristine demo: stop (Ctrl-C), then tools/demo.sh --reset

Press Ctrl-C to stop both servers.
EOF

while kill -0 "$BACKEND_PID" 2>/dev/null && kill -0 "$FRONTEND_PID" 2>/dev/null; do
  sleep 2
done
kill -0 "$BACKEND_PID" 2>/dev/null || echo "The backend stopped; see $BACKEND_LOG" >&2
kill -0 "$FRONTEND_PID" 2>/dev/null || echo "The frontend stopped; see $FRONTEND_LOG" >&2
exit 1
