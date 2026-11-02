#!/usr/bin/env bash
#
# Collects the software metrics of Part 6 (docs/06-metrics.md).
#
# Usage: bash tools/metrics.sh [--publish]
#
# Output: metrics/ (git-ignored) with the raw tool reports and their summaries.
# --publish also copies the summaries to docs/results/metrics/ (committed, D-051).
# Never cleans backend/target (the owner's dev backend runs from target/classes).

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/metrics"
PUBLISH_DIR="${ROOT_DIR}/docs/results/metrics"
PUBLISH=0
[ "${1:-}" = "--publish" ] && PUBLISH=1

# shellcheck source=/dev/null
source "${ROOT_DIR}/tools/toolchain.env"
export JAVA_HOME="${HOME}/.local/opt/jdk-21"
export MAVEN_HOME="${HOME}/.local/opt/maven"
export PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"
if ! command -v npx >/dev/null 2>&1 && command -v fnm >/dev/null 2>&1; then eval "$(fnm env)"; fi

mkdir -p "${OUT_DIR}"
cd "${ROOT_DIR}"   # relative paths in every report

{
  echo "commit: $(git rev-parse HEAD 2>/dev/null || echo unknown)"
  echo "tracked_changes: $(git status --porcelain --untracked-files=no 2>/dev/null | wc -l | tr -d ' ')"
  echo "date_utc: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "java: $(java -version 2>&1 | head -1)"
  echo "maven: $(mvn -v 2>/dev/null | head -1)"
  echo "node: $(node -v 2>/dev/null || echo none)"
  echo "oxlint: $(cd frontend && npx oxlint --version 2>/dev/null | head -1)"
} > "${OUT_DIR}/environment.txt"

echo "==> 1/5 Lines of code"
python3 tools/count-loc.py backend/src frontend/src | tee "${OUT_DIR}/loc.txt"
echo "    Own code only (the vendored shadcn/ui components are excluded):"
python3 tools/count-loc.py backend/src frontend/src --exclude components/ui | tee "${OUT_DIR}/loc-own-code.txt"
python3 tools/count-loc.py backend/src/main/java --extensions .java > "${OUT_DIR}/loc-java-main.txt"
python3 tools/count-loc.py backend/src/test/java --extensions .java > "${OUT_DIR}/loc-java-test.txt"
python3 tools/count-loc.py frontend/src --extensions .ts,.tsx --exclude components/ui > "${OUT_DIR}/loc-frontend-own.txt"
python3 tools/count-loc.py frontend/src/components/ui --extensions .ts,.tsx > "${OUT_DIR}/loc-frontend-vendored.txt"
for f in loc-java-main loc-java-test loc-frontend-own loc-frontend-vendored; do
  echo "    ${f}: $(grep '^TOTAL' "${OUT_DIR}/${f}.txt")"
done

echo
echo "==> 2/5 CKJM (WMC, DIT, NOC, CBO, RFC, LCOM)"
(
  cd backend
  mvn -B -q -DskipTests compile
  mvn -B -q dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
)
# Guard: target/classes is never cleaned, so a class whose source was deleted would be measured.
STALE=$(comm -23 \
  <(find backend/target/classes -name '*.class' ! -name '*$*' | sed 's|backend/target/classes/||; s|\.class$||' | sort) \
  <(find backend/src/main/java -name '*.java' | sed 's|backend/src/main/java/||; s|\.java$||' | sort))
if [ -n "${STALE}" ]; then
  echo "error: stale classes in backend/target/classes (source deleted):" >&2
  echo "${STALE}" >&2
  echo "remove exactly these .class files (never run mvn clean) and run again" >&2
  exit 3
fi
CKJM_CLASSPATH="${ROOT_DIR}/backend/target/classes:$(cat backend/target/classpath.txt)"
export CKJM_CLASSPATH
find backend/target/classes -name "*.class" -print0 \
  | xargs -0 "$HOME/.local/bin/ckjm" > "${OUT_DIR}/ckjm.txt" 2>&1 || true
python3 tools/ckjm-summary.py "${OUT_DIR}/ckjm.txt" | tee "${OUT_DIR}/ckjm-summary.txt"

echo
echo "==> 3/5 PMD (cyclomatic complexity, code smells)"
( cd backend && mvn -B -Pmetrics -DskipTests verify ) > "${OUT_DIR}/pmd-build.log" 2>&1 || true
for f in pmd.xml pmd-cc-all/pmd.xml jdepend-report.xml; do
  if [ ! -f "backend/target/${f}" ]; then echo "error: backend/target/${f} missing, see ${OUT_DIR}/pmd-build.log" >&2; exit 4; fi
done
if grep -q "Unknown constant" "${OUT_DIR}/pmd-build.log"; then
  echo "error: JDepend could not parse some class files, see ${OUT_DIR}/pmd-build.log" >&2; exit 5
fi
cp backend/target/pmd.xml "${OUT_DIR}/pmd.xml"
cp backend/target/pmd-cc-all/pmd.xml "${OUT_DIR}/pmd-cc-all.xml"
python3 tools/pmd-summary.py "${OUT_DIR}/pmd.xml" "${OUT_DIR}/pmd-cc-all.xml" | tee "${OUT_DIR}/pmd-summary.txt"

echo
echo "==> 4/5 JDepend (package coupling)"
cp backend/target/jdepend-report.xml "${OUT_DIR}/jdepend-report.xml"
[ -f backend/target/site/jdepend-report.html ] && cp backend/target/site/jdepend-report.html "${OUT_DIR}/jdepend-report.html"
python3 tools/jdepend-summary.py "${OUT_DIR}/jdepend-report.xml" | tee "${OUT_DIR}/jdepend-summary.txt"

echo
echo "==> 5/5 Frontend cyclomatic complexity (oxlint eslint/complexity, vendored components/ui excluded)"
( cd frontend && npx oxlint -c ../tools/oxlint-complexity.json --ignore-pattern 'src/components/ui/**' -f unix src ) \
  > "${OUT_DIR}/ts-complexity.txt" 2>&1 || true
python3 tools/ts-complexity-summary.py "${OUT_DIR}/ts-complexity.txt" | tee "${OUT_DIR}/ts-complexity-summary.txt"

if [ "${PUBLISH}" = 1 ]; then
  mkdir -p "${PUBLISH_DIR}"
  for f in environment.txt loc.txt loc-own-code.txt loc-java-main.txt loc-java-test.txt loc-frontend-own.txt \
           loc-frontend-vendored.txt ckjm-summary.txt pmd-summary.txt jdepend-summary.txt ts-complexity-summary.txt; do
    cp "${OUT_DIR}/${f}" "${PUBLISH_DIR}/${f}"
  done
  echo "Summaries published to ${PUBLISH_DIR}"
fi

echo
echo "Metrics collected in ${OUT_DIR}"
