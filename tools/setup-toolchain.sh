#!/usr/bin/env bash
#
# Installs the toolchain required by the prototype into the user's home directory.
#
# The development machine has no system package manager and no root access, so the JDK,
# Maven and the metric tools are installed as user-local tarballs:
#
#   ~/.local/opt/jdk-21      Eclipse Temurin JDK 21 (LTS)
#   ~/.local/opt/maven       Apache Maven 3.9.x
#   ~/.local/opt/ckjm        CKJM (Chidamber & Kemerer Java Metrics) CLI
#
# Node.js is expected to be provided by fnm / nvm (see the project README).
#
# Usage: bash tools/setup-toolchain.sh

set -euo pipefail

TOOLS_DIR="${HOME}/.local/opt"
BIN_DIR="${HOME}/.local/bin"
SRC_DIR="${HOME}/.local/src"

JAVA_VERSION="21.0.13+9"
JAVA_URL="https://github.com/adoptium/temurin21-binaries/releases/download/jdk-${JAVA_VERSION}/OpenJDK21U-jdk_x64_linux_hotspot_${JAVA_VERSION}.tar.gz"
MAVEN_VERSION="3.9.16"
MAVEN_URL="https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
CKJM_URL="https://github.com/mjureczko/CKJM-extended/releases/download/v2.10/ckjm_ext.jar"

mkdir -p "${TOOLS_DIR}" "${BIN_DIR}" "${SRC_DIR}"

install_jdk() {
  if [ -x "${TOOLS_DIR}/jdk-21/bin/java" ]; then
    echo "==> JDK 21 already installed"
    return
  fi
  echo "==> Downloading Temurin JDK ${JAVA_VERSION}"
  curl -sSL --retry 3 -o "${SRC_DIR}/jdk21.tar.gz" "${JAVA_URL}"
  tar xzf "${SRC_DIR}/jdk21.tar.gz" -C "${TOOLS_DIR}"
  ln -sfn "${TOOLS_DIR}/jdk-${JAVA_VERSION}" "${TOOLS_DIR}/jdk-21"
}

install_maven() {
  if [ -x "${TOOLS_DIR}/maven/bin/mvn" ]; then
    echo "==> Maven already installed"
    return
  fi
  echo "==> Downloading Apache Maven ${MAVEN_VERSION}"
  curl -sSL --retry 3 -o "${SRC_DIR}/maven.tar.gz" "${MAVEN_URL}"
  tar xzf "${SRC_DIR}/maven.tar.gz" -C "${TOOLS_DIR}"
  ln -sfn "${TOOLS_DIR}/apache-maven-${MAVEN_VERSION}" "${TOOLS_DIR}/maven"
}

install_ckjm() {
  if [ -f "${TOOLS_DIR}/ckjm/ckjm.jar" ]; then
    echo "==> CKJM already installed"
    return
  fi
  echo "==> Downloading CKJM"
  mkdir -p "${TOOLS_DIR}/ckjm"
  curl -sSL --retry 3 -o "${TOOLS_DIR}/ckjm/ckjm.jar" "${CKJM_URL}"
  cat > "${BIN_DIR}/ckjm" <<'WRAPPER'
#!/bin/sh
exec "$HOME/.local/opt/jdk-21/bin/java" -jar "$HOME/.local/opt/ckjm/ckjm.jar" "$@"
WRAPPER
  chmod +x "${BIN_DIR}/ckjm"
}

write_env_file() {
  cat > "${TOOLS_DIR}/ase-env.sh" <<'ENV'
#!/bin/sh
# Environment for the locally installed Java / Maven toolchain.
export JAVA_HOME="$HOME/.local/opt/jdk-21"
export MAVEN_HOME="$HOME/.local/opt/maven"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:--Xmx1g}"
ENV
  echo "==> Wrote ${TOOLS_DIR}/ase-env.sh"
}

install_jdk
install_maven
install_ckjm
write_env_file

echo
echo "Toolchain ready. Activate it with:"
echo '    . "$HOME/.local/opt/ase-env.sh"'
echo
"${TOOLS_DIR}/jdk-21/bin/java" -version
"${TOOLS_DIR}/maven/bin/mvn" -version
