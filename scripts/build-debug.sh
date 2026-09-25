#!/usr/bin/env bash
# WARRIOR debug build wrapper.
#
# Sandbox/CI machines with <=1GB RAM cannot run AGP's in-daemon D8 external-dex
# merge (mergeExtDexDebug OOMs). This script pre-merges external dex archives
# with a standalone D8 process (full RAM, no Gradle daemon alive), then lets
# Gradle package the APK with that task excluded.
#
# Usage: scripts/build-debug.sh
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
D8_JAR="${ANDROID_HOME}/build-tools/35.0.0/lib/d8.jar"
EXT_OUT=app/build/intermediates/dex/debug/mergeExtDexDebug

if [ ! -f "${EXT_OUT}/classes.dex" ]; then
  echo ">> dumping mergeExtDexDebug inputs via init script..."
  cat > /tmp/warrior-dump.init.gradle <<'EOF'
gradle.projectsEvaluated {
    def t = gradle.rootProject.project(":app").tasks.findByName("mergeExtDexDebug")
    if (t != null) {
        new File("/tmp/mergeExtDexDebug_inputs.txt").text =
            t.inputs.files.files.collect { it.absolutePath }.join("\n")
    }
}
EOF
  ./gradlew -I /tmp/warrior-dump.init.gradle help -q >/dev/null 2>&1 || true

  echo ">> standalone D8 merge of external dex archives..."
  mkdir -p "${EXT_OUT}"
  find $(cat /tmp/mergeExtDexDebug_inputs.txt | tr '\n' ' ') -name "*.dex" > /tmp/ext_dex_files.txt
  java -Xmx900m -XX:MaxMetaspaceSize=128m -cp "${D8_JAR}" com.android.tools.r8.D8 \
    --min-api 24 --output "${EXT_OUT}" $(cat /tmp/ext_dex_files.txt | tr '\n' ' ')
fi

echo ">> gradle assembleDebug (mergeExtDexDebug pre-provided)..."
./gradlew :app:assembleDebug -x :app:mergeExtDexDebug "$@"
