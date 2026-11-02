#!/usr/bin/env bash
# Runs the backend (Spring Boot, :8080) and the frontend (Vite, :5173) side by side
# in one tmux session.
#
# Usage: tools/dev.sh [start|stop|restart|status|attach] [-d]
#   start     create the session and attach to it (default; -d / --detach to stay out)
#   stop      kill the session (stops both servers)
#   restart   stop, then start
#   status    show the session and whether both ports answer
#   attach    attach to the running session
#
# Environment: ASE_TMUX_SESSION (session name, default "ase"),
#              ASE_BACKEND_ARGS (extra args for `mvnw spring-boot:run`, e.g.
#              "-Dspring-boot.run.profiles=prod" or
#              "-Dspring-boot.run.arguments=--wastems.demo.reset=true").
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SESSION="${ASE_TMUX_SESSION:-ase}"
BACKEND_PORT=8080
FRONTEND_PORT=5173
BACKEND_ARGS="${ASE_BACKEND_ARGS:-}"

command -v tmux >/dev/null || { echo "tmux is not installed" >&2; exit 1; }

session_exists() { tmux has-session -t "$SESSION" 2>/dev/null; }
port_in_use() { ss -tln 2>/dev/null | grep -qE ":$1\b"; }
http_code() { curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$1" 2>/dev/null || true; }

attach() {
  if [ -n "${TMUX:-}" ]; then
    tmux switch-client -t "$SESSION"
  elif [ -t 1 ]; then
    tmux attach -t "$SESSION"
  else
    echo "Session '$SESSION' is running; attach with: tmux attach -t $SESSION"
  fi
}

start() {
  local detach="$1"
  if session_exists; then
    echo "Session '$SESSION' is already running."
    [ "$detach" = 1 ] || attach
    return 0
  fi
  for port in "$BACKEND_PORT" "$FRONTEND_PORT"; do
    if port_in_use "$port"; then
      echo "Port $port is already in use by another process; stop it first." >&2
      exit 1
    fi
  done

  # Each pane runs a login shell so the toolchain env loads; `exec bash` keeps the pane
  # open after Ctrl-C so the command can be re-run.
  local backend_cmd frontend_cmd
  backend_cmd="source ~/.local/opt/ase-env.sh; ./mvnw spring-boot:run $BACKEND_ARGS; exec bash"
  frontend_cmd='eval "$(fnm env)" 2>/dev/null; fnm use >/dev/null 2>&1; npm run dev; exec bash'

  tmux new-session -d -s "$SESSION" -n dev -c "$ROOT/backend" "bash -lc '$backend_cmd'"
  tmux split-window -h -t "$SESSION:dev" -c "$ROOT/frontend" "bash -lc '$frontend_cmd'"
  tmux select-layout -t "$SESSION:dev" even-horizontal
  tmux select-pane -t "$SESSION:dev.0" -T backend
  tmux select-pane -t "$SESSION:dev.1" -T frontend
  tmux set-option -t "$SESSION" pane-border-status top

  echo "Started tmux session '$SESSION': backend :$BACKEND_PORT (left), frontend :$FRONTEND_PORT (right)."
  echo "The backend needs ~30-60 s to be ready; check with: tools/dev.sh status"
  [ "$detach" = 1 ] || attach
}

stop() {
  if session_exists; then
    tmux kill-session -t "$SESSION"
    echo "Stopped session '$SESSION'."
  else
    echo "Session '$SESSION' is not running."
  fi
}

status() {
  if session_exists; then
    echo "tmux session '$SESSION': running"
  else
    echo "tmux session '$SESSION': not running"
  fi
  echo "backend  :$BACKEND_PORT  HTTP $(http_code "http://localhost:$BACKEND_PORT/api/auth/me") (401 = up, not signed in)"
  echo "frontend :$FRONTEND_PORT  HTTP $(http_code "http://localhost:$FRONTEND_PORT/")"
}

cmd="start"
detach=0
for arg in "$@"; do
  case "$arg" in
    start | stop | restart | status | attach) cmd="$arg" ;;
    -d | --detach) detach=1 ;;
    -h | --help) sed -n '2,15p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) echo "Unknown argument: $arg (see --help)" >&2; exit 2 ;;
  esac
done

case "$cmd" in
  start) start "$detach" ;;
  stop) stop ;;
  restart) stop; start "$detach" ;;
  status) status ;;
  attach)
    session_exists || { echo "Session '$SESSION' is not running; start it with tools/dev.sh" >&2; exit 1; }
    attach
    ;;
esac
