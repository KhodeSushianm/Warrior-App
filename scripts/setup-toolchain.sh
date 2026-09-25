#!/usr/bin/env bash
# Idempotent dev-toolchain bootstrap for WARRIOR (sandbox/CI-less environments).
# Installs JDK 17, Gradle 8.11.1 and Android SDK (platform 35 + build-tools 35).
set -euo pipefail

export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
GRADLE_VERSION=8.11.1

if ! command -v java >/dev/null 2>&1; then
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-17-jdk-headless unzip curl
fi

if [ ! -x "/opt/gradle/gradle-${GRADLE_VERSION}/bin/gradle" ]; then
  mkdir -p /opt/dl
  curl -sSL -o /opt/dl/gradle.zip "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  unzip -q /opt/dl/gradle.zip -d /opt/gradle-tmp
  mkdir -p /opt/gradle
  mv "/opt/gradle-tmp/gradle-${GRADLE_VERSION}" "/opt/gradle/gradle-${GRADLE_VERSION}"
  rm -rf /opt/dl /opt/gradle-tmp
fi

if [ ! -d "${ANDROID_HOME}/platforms/android-35" ]; then
  mkdir -p "${ANDROID_HOME}/cmdline-tools"
  if [ ! -x "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" ]; then
    curl -sSL -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
    unzip -q /tmp/cmdtools.zip -d "${ANDROID_HOME}/cmdline-tools"
    mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
  fi
  yes 2>/dev/null | "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="${ANDROID_HOME}" --licenses >/dev/null 2>&1 || true
  "${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager" --sdk_root="${ANDROID_HOME}" \
    "platforms;android-35" "build-tools;35.0.0" >/dev/null 2>&1
fi

echo "toolchain ready: java=$(java -version 2>&1 | head -1), gradle=${GRADLE_VERSION}, sdk=${ANDROID_HOME}"
