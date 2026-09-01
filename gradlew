#!/bin/sh
#
# Simple Gradle wrapper bootstrap for Luna V1.
# Downloads Gradle only on the CI runner if needed.
#
set -eu

GRADLE_VERSION="8.13"
DIST_NAME="gradle-${GRADLE_VERSION}-bin.zip"
DIST_URL="https://services.gradle.org/distributions/${DIST_NAME}"
CACHE_DIR="${HOME}/.gradle/wrapper/dists/gradle-${GRADLE_VERSION}"
INSTALL_DIR="${CACHE_DIR}/gradle-${GRADLE_VERSION}"

if [ ! -x "${INSTALL_DIR}/bin/gradle" ]; then
  mkdir -p "${CACHE_DIR}"
  TMP_ZIP="${CACHE_DIR}/${DIST_NAME}"
  if [ ! -f "${TMP_ZIP}" ]; then
    curl -fsSL "${DIST_URL}" -o "${TMP_ZIP}"
  fi
  rm -rf "${INSTALL_DIR}"
  unzip -q "${TMP_ZIP}" -d "${CACHE_DIR}"
fi

exec "${INSTALL_DIR}/bin/gradle" "$@"
