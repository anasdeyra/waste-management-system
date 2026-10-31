#!/usr/bin/env bash
#
# Collects the software metrics of Part 6.
#
# The script is the reproducible entry point referenced by `docs/06-metrics.md`:
# every number reported in that document comes from this script.
#
# Usage: bash tools/metrics.sh
#
# Output: `metrics/` (git-ignored) with the raw tool reports.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${ROOT_DIR}/metrics"

# shellcheck source=/dev/null
source "${ROOT_DIR}/tools/toolchain.env"
export JAVA_HOME="${HOME}/.local/opt/jdk-21"
export MAVEN_HOME="${HOME}/.local/opt/maven"
export PATH="${JAVA_HOME}/bin:${MAVEN_HOME}/bin:${PATH}"

mkdir -p "${OUT_DIR}"

echo "==> 1/4 Lines of code"
python3 "${ROOT_DIR}/tools/count-loc.py" \
    "${ROOT_DIR}/backend/src" "${ROOT_DIR}/frontend/src" \
    | tee "${OUT_DIR}/loc.txt"
echo
echo "    Own code only (the vendored shadcn/ui components are excluded):"
python3 "${ROOT_DIR}/tools/count-loc.py" \
    "${ROOT_DIR}/backend/src" "${ROOT_DIR}/frontend/src" \
    --exclude "components/ui" | tee "${OUT_DIR}/loc-own-code.txt"

echo
echo "==> 2/4 CKJM (WMC, DIT, NOC, CBO, RFC, LCOM)"
(
  cd "${ROOT_DIR}/backend"
  mvn -B -q -DskipTests compile
  mvn -B -q dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
)
# CKJM resolves super classes through the JVM class path, so the compiled classes and
# their dependencies have to be part of it.
CKJM_CLASSPATH="${ROOT_DIR}/backend/target/classes:$(cat "${ROOT_DIR}/backend/target/classpath.txt")"
export CKJM_CLASSPATH
find "${ROOT_DIR}/backend/target/classes" -name "*.class" -print0 \
  | xargs -0 "$HOME/.local/bin/ckjm" > "${OUT_DIR}/ckjm.txt" 2>&1 || true
echo "    written to ${OUT_DIR}/ckjm.txt"
python3 "${ROOT_DIR}/tools/ckjm-summary.py" "${OUT_DIR}/ckjm.txt" | tee "${OUT_DIR}/ckjm-summary.txt"

echo
echo "==> 3/4 PMD (cyclomatic complexity, code smells)"
(
  cd "${ROOT_DIR}/backend"
  mvn -B -Pmetrics -DskipTests verify
) > "${OUT_DIR}/pmd-build.log" 2>&1 || true
if [ -f "${ROOT_DIR}/backend/target/pmd.xml" ]; then
  cp "${ROOT_DIR}/backend/target/pmd.xml" "${OUT_DIR}/pmd.xml"
  echo "    written to ${OUT_DIR}/pmd.xml"
else
  echo "    PMD report missing, see ${OUT_DIR}/pmd-build.log"
fi

echo
echo "==> 4/4 JDepend (coupling)"
if [ -f "${ROOT_DIR}/backend/target/site/jdepend-report.html" ]; then
  cp "${ROOT_DIR}/backend/target/site/jdepend-report.html" "${OUT_DIR}/jdepend-report.html"
  cp "${ROOT_DIR}/backend/target/jdepend-report.xml" "${OUT_DIR}/jdepend-report.xml"
  echo "    written to ${OUT_DIR}/jdepend-report.html"
  python3 "${ROOT_DIR}/tools/jdepend-summary.py" "${OUT_DIR}/jdepend-report.xml" \
    | tee "${OUT_DIR}/jdepend-summary.txt"
else
  echo "    JDepend report missing, see ${OUT_DIR}/pmd-build.log"
fi

echo
echo "Metrics collected in ${OUT_DIR}"
