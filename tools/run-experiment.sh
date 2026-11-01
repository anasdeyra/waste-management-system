#!/usr/bin/env bash
# Runs the seeded MN01 evaluation experiment headless and writes its outputs (D-016, D-022, D-050).
# Usage: bash tools/run-experiment.sh OUT_DIR [--wastems.experiment.profiles=UNIFORM_LOW,HOTSPOT]
#        [--wastems.experiment.seeds=1,2] [--wastems.experiment.sizes=10,25]
#        [--wastems.experiment.repetitions=3] [--wastems.experiment.label=TEXT]
# Without options it runs the fixed 60-scenario matrix once (about 1-2 minutes).
# OUT_DIR must be new or empty; it receives results.csv, scenarios.csv, batch.json, run-info.txt,
# run.log, the experiment H2 database (db/) and the road-graph cache (routing-cache/).
# The jar is rebuilt first (./mvnw -DskipTests package) unless WASTEMS_SKIP_BUILD=1.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${1:?usage: tools/run-experiment.sh OUT_DIR [--wastems.experiment.NAME=VALUE ...]}"
shift
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
if [ -n "$(ls -A "$OUT")" ]; then
  echo "error: $OUT is not empty; choose a new output directory" >&2
  exit 64
fi
if [ -f "$HOME/.local/opt/ase-env.sh" ]; then source "$HOME/.local/opt/ase-env.sh"; fi
JAR="$ROOT/backend/target/wastems-backend-0.1.0-SNAPSHOT.jar"
if [ "${WASTEMS_SKIP_BUILD:-0}" != "1" ] || [ ! -f "$JAR" ]; then
  (cd "$ROOT/backend" && ./mvnw -q -DskipTests package)
fi
{
  echo "commit: $(git -C "$ROOT" rev-parse HEAD 2>/dev/null || echo unknown)"
  echo "tracked_changes: $(git -C "$ROOT" status --porcelain --untracked-files=no 2>/dev/null | wc -l | tr -d ' ')"
  echo "started_utc: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "java: $(java -version 2>&1 | head -1)"
  echo "os: $(uname -srm)"
  echo "cpus: $(nproc 2>/dev/null || echo unknown)"
  echo "args: $*"
} > "$OUT/run-info.txt"
set +e
java -jar "$JAR" --spring.profiles.active=experiment \
  --spring.datasource.url="jdbc:h2:file:$OUT/db/wastems-experiment;DB_CLOSE_ON_EXIT=FALSE" \
  --wastems.routing.cache-dir="$OUT/routing-cache" \
  --wastems.experiment.output-dir="$OUT" "$@" 2>&1 | tee "$OUT/run.log"
STATUS=${PIPESTATUS[0]}
set -e
echo "finished_utc: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> "$OUT/run-info.txt"
echo "exit: $STATUS" >> "$OUT/run-info.txt"
exit "$STATUS"
