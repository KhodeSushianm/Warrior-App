#!/usr/bin/env bash
# WARRIOR release build wrapper (Phase 10 — Hardening + Release RC).
#
# Why this script exists (1GiB sandbox constraint, documented in the Phase 10
# report): running `minifyReleaseWithR8` inside the Gradle daemon is impossible
# here — R8 needs ~600-700MB heap over the full dependency graph, and the
# daemon is kernel-OOM-killed at that footprint (verified via dmesg; R8
# heap-OOMs at 512m, daemon dies at 640m). So R8 runs as a STANDALONE process,
# mirroring the standalone-D8 pattern of build-debug.sh:
#
#   1. Gradle builds every R8 input (classes, resource-linked aapt rules,
#      library jars) with R8/lintVital/package excluded.
#   2. The release runtime classpath (subproject classes.jar + external jars)
#      is dumped via an init-script ArtifactView ("android-classes-jar").
#   3. App classes (ASM-transformed) are jar'd; AAR consumer ProGuard rules
#      are collected from the Gradle transforms cache (R8 cannot see them
#      inside extracted classes.jars — missing them silently strips
#      reflection-instantiated classes like Room's *_Impl!).
#   4. Standalone R8 (AGP's embedded R8 from builder-8.7.3.jar, -Xmx700m)
#      shrinks + obfuscates everything into AGP's intermediates layout.
#   5. Gradle packages + signs the APK with R8 excluded.
#
# Resource shrinking is disabled in release (needs AGP-internal
# -printresources wiring); code shrinking + obfuscation are ON.
#
# Usage: scripts/build-release.sh   ->  app/build/outputs/apk/release/app-release.apk
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
GRADLE_MEM="-Dorg.gradle.jvmargs=-Xmx384m\ -XX:MaxMetaspaceSize=256m\ -XX:ReservedCodeCacheSize=64m\ -Xss512k"
AGP_JAR=$(find "${HOME}/.gradle/caches/modules-2/files-2.1/com.android.tools.build/builder" -name "builder-*.jar" | head -1)
R8_JVM="-Xmx700m -XX:MaxMetaspaceSize=96m -XX:MaxDirectMemorySize=32m -XX:ReservedCodeCacheSize=32m -XX:TieredStopAtLevel=1 -Xss512k"
DEX_OUT=app/build/intermediates/dex/release/minifyReleaseWithR8
MAP_OUT=app/build/outputs/mapping/release
APP_CLASSES=app/build/intermediates/classes/release/transformReleaseClassesWithAsm
LINT_EXCLUDES="-x :app:lintVitalRelease -x :app:lintVitalAnalyzeRelease -x :app:generateReleaseLintVitalReportModel"

echo ">> 1/5 building R8 inputs (module-by-module; low-RAM chain)..."
./gradlew :app:assembleRelease -x :app:minifyReleaseWithR8 -x :app:packageRelease $LINT_EXCLUDES $GRADLE_MEM

if [ ! -f "${DEX_OUT}/classes.dex" ]; then
  echo ">> 2/5 dumping release runtime classpath via ArtifactView..."
  cat > /tmp/warrior-cpdump.init.gradle <<'EOF'
import org.gradle.api.attributes.Attribute
gradle.projectsEvaluated {
    def app = gradle.rootProject.project(":app")
    def conf = app.configurations.getByName("releaseRuntimeClasspath")
    def view = conf.incoming.artifactView { v ->
        v.lenient(true)
        v.attributes { it.attribute(Attribute.of("artifactType", String), "android-classes-jar") }
    }
    new File("/tmp/r8_runtime_cp.txt").text = view.files.files.collect { it.absolutePath }.join("\n")
}
EOF
  ./gradlew -I /tmp/warrior-cpdump.init.gradle help $GRADLE_MEM -q >/dev/null 2>&1 || true

  echo ">> 3/5 collecting app classes jar + AAR consumer ProGuard rules..."
  (cd "${APP_CLASSES}/dirs" && jar cf /tmp/warrior-app-release-classes.jar .)
  find "${HOME}/.gradle/caches" -path "*transforms*" -name "proguard.txt" 2>/dev/null | sort -u > /tmp/r8_consumer_confs.txt
  CONFS=""
  while read -r c; do CONFS="${CONFS} --pg-conf ${c}"; done < /tmp/r8_consumer_confs.txt

  echo ">> 4/5 standalone R8 (no Gradle daemon alive)..."
  rm -rf "${DEX_OUT}" "${MAP_OUT}" && mkdir -p "${DEX_OUT}" "${MAP_OUT}"
  java ${R8_JVM} -cp "${AGP_JAR}" com.android.tools.r8.R8 \
    --release --min-api 24 \
    --lib "${ANDROID_HOME}/platforms/android-35/android.jar" \
    --pg-conf app/build/intermediates/default_proguard_files/global/proguard-android-optimize.txt-8.7.3 \
    --pg-conf app/build/intermediates/generated_proguard_file/release/mergeReleaseGeneratedProguardFiles/proguard.txt \
    --pg-conf app/build/intermediates/aapt_proguard_file/release/processReleaseResources/aapt_rules.txt \
    --pg-conf app/proguard-rules.pro \
    ${CONFS} \
    --pg-map-output "${MAP_OUT}/mapping.txt" \
    --output "${DEX_OUT}" \
    /tmp/warrior-app-release-classes.jar \
    $(find "${APP_CLASSES}/jars" -name "*.jar" 2>/dev/null) \
    $(cat /tmp/r8_runtime_cp.txt | tr '\n' ' ')

  # Sanity: reflection-instantiated classes must survive with their names.
  grep -q "^com.warrior.data.local.database.WarriorDatabase_Impl -> com.warrior.data.local.database.WarriorDatabase_Impl:" "${MAP_OUT}/mapping.txt" \
    || { echo "FATAL: Room *_Impl was stripped/renamed — consumer rules missing?"; exit 1; }
fi

echo ">> 5/5 packaging + signing..."
./gradlew :app:assembleRelease -x :app:minifyReleaseWithR8 $LINT_EXCLUDES $GRADLE_MEM
ls -la app/build/outputs/apk/release/app-release.apk
