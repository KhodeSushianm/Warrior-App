#!/usr/bin/env bash
# WARRIOR release build wrapper (Phase 10 + Phase 15 revision).
#
# Why this script exists (1GiB sandbox constraint, documented in the Phase 10
# and 15 reports): three build steps cannot run inside the Gradle daemon here —
#   * `minifyReleaseWithR8` over the full graph (needs ~700MB heap; daemon gets
#     kernel-OOM-killed),
#   * the app module's cold `compileReleaseKotlin` (same wall),
#   * `mergeExtDexRelease`-class merges (debug script handles the debug twin).
# So this script runs them as STANDALONE JVMs (no Gradle alive at that moment):
#
#   1. Gradle builds every R8/Kotlin input EXCEPT the app's own Kotlin compile
#      (excluded via -x) and except minify/package/lintVital.
#   2. The release runtime classpath is dumped via an ArtifactView init script.
#   3. `kotlinc` (kotlin-compiler-embeddable + compose plugin from the Gradle
#      cache) compiles the app module standalone with an 820MB heap.
#   4. AAR consumer ProGuard rules are collected (AAR roots + transforms cache)
#      and R8 (embedded in AGP's builder jar) shrinks/obfuscates everything
#      into AGP's intermediates layout, with a correctness gate on the mapping.
#   5. Gradle packages + signs the APK (minify & app-kotlin excluded; their
#      outputs are pre-provided).
#
# Resource shrinking stays disabled (needs AGP-internal R8 wiring).
#
# Usage: scripts/build-release.sh   ->  app/build/outputs/apk/release/app-release.apk
set -euo pipefail
cd "$(dirname "$0")/.."

# Kill leftover Gradle/Kotlin JVMs so standalone steps get the whole 1GiB.
# (Pattern is prefixed with the JVM path so this script's own shell never matches.)
kill_jvms() {
  for p in /proc/[0-9]*; do
    c=$(tr '\0' ' ' < "$p/cmdline" 2>/dev/null)
    case "$c" in
      /usr/lib/jvm*|/opt/*java*) kill -9 "$(basename "$p")" 2>/dev/null || true ;;
    esac
  done
  sleep 2
  sync
}

export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}
GRADLE_CACHE="${HOME}/.gradle/caches"
[ -d /root/.gradle/caches ] && GRADLE_CACHE="/root/.gradle/caches"
M2="${GRADLE_CACHE}/modules-2/files-2.1"
KC_JAR=$(find "${M2}/org.jetbrains.kotlin/kotlin-compiler-embeddable" -name "kotlin-compiler-embeddable-*.jar" 2>/dev/null | sort -V | tail -1)
KC_STD=$(find "${M2}/org.jetbrains.kotlin/kotlin-stdlib" -name "kotlin-stdlib-2.*.jar" 2>/dev/null | grep -v sources | sort -V | tail -1)
KC_COR=$(find "${M2}/org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm" -name "kotlinx-coroutines-core-jvm-*.jar" 2>/dev/null | grep -v sources | sort -V | tail -1)
KC_TR=$(find "${M2}" -path "*trove*" -name "*.jar" 2>/dev/null | grep -v sources | head -1)
KC_AN=$(find "${M2}/org.jetbrains/annotations" -name "annotations-*.jar" 2>/dev/null | grep -v sources | head -1)
CC_JAR=$(find "${M2}/org.jetbrains.kotlin/kotlin-compose-compiler-plugin-embeddable" -name "*embeddable-*.jar" 2>/dev/null | sort -V | tail -1)
AGP_JAR=$(find "${M2}/com.android.tools.build/builder" -name "builder-*.jar" 2>/dev/null | head -1)
R8_JVM="-Xmx512m -XX:MaxMetaspaceSize=160m -XX:MaxDirectMemorySize=32m -XX:ReservedCodeCacheSize=32m -XX:+UseSerialGC -XX:ActiveProcessorCount=1 -Xss512k"
KOTLIN_JVM="-Xmx820m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -XX:+UseSerialGC"
DEX_OUT=app/build/intermediates/dex/release/minifyReleaseWithR8
MAP_OUT=app/build/outputs/mapping/release
APP_CLASSES=app/build/intermediates/classes/release/transformReleaseClassesWithAsm
KOTLIN_OUT=app/build/tmp/kotlin-classes/release
RJAR=app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/release/processReleaseResources/R.jar
LINT_EXCLUDES="-x :app:lintVitalRelease -x :app:lintVitalAnalyzeRelease -x :app:generateReleaseLintVitalReportModel"
APP_KOTLIN_EXCLUDE="-I /tmp/warrior-skip.init.gradle"
cat > /tmp/warrior-skip.init.gradle <<'INITEOF'
gradle.taskGraph.whenReady { g ->
    g.allTasks.each { t ->
        if (t.path == ":app:compileReleaseKotlin" || t.path == ":app:minifyReleaseWithR8") {
            t.onlyIf { false }
        }
    }
}
INITEOF
# Keep the Gradle daemon tiny during standalone-heavy phases.
GFLAGS=(
  "-Dorg.gradle.jvmargs=-Xmx256m -XX:MaxMetaspaceSize=224m -XX:ReservedCodeCacheSize=40m -XX:+UseSerialGC -XX:-TieredCompilation -XX:ActiveProcessorCount=1 -XX:MaxDirectMemorySize=32m -Xss384k"
)

echo ">> 1/5 KSP + resources + proguard configs (no app Kotlin compile)..."
# NOTE: mergeReleaseGeneratedProguardFiles depends on compileReleaseKotlin, so
# it runs in step 3b (its output from a previous run is reused by R8 if present).
./gradlew :app:kspReleaseKotlin :app:processReleaseResources \
    :app:extractProguardFiles :app:generateReleaseBuildConfig $LINT_EXCLUDES "${GFLAGS[@]}"

if [ ! -f "${KOTLIN_OUT}/com/warrior/app/MainActivity.class" ]; then
  echo ">> 2/5 dumping release compile classpath..."
  cat > /tmp/warrior-appcp.init.gradle <<'EOF'
import org.gradle.api.attributes.Attribute
gradle.projectsEvaluated {
    def app = gradle.rootProject.project(":app")
    def conf = app.configurations.getByName("releaseCompileClasspath")
    def view = conf.incoming.artifactView { v ->
        v.lenient(true)
        v.attributes { it.attribute(Attribute.of("artifactType", String), "android-classes-jar") }
    }
    new File("/tmp/app_release_cp.txt").text = view.files.files.collect { it.absolutePath }.join(File.pathSeparator)
}
EOF
  ./gradlew -I /tmp/warrior-appcp.init.gradle help -q >/dev/null 2>&1 || true

  echo ">> 3/5 standalone kotlinc for :app (820m, no Gradle alive)..."
  kill_jvms
  mkdir -p "${KOTLIN_OUT}"
  "${JAVA_HOME}/bin/java" ${KOTLIN_JVM} -cp "${KC_JAR}:${KC_STD}:${KC_COR}:${KC_TR}:${KC_AN}" \
    org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
    -no-stdlib -jvm-target 17 -Xjvm-default=all-compatibility \
    -cp "${ANDROID_HOME}/platforms/android-35/android.jar:${KC_STD}:${RJAR}:$(cat /tmp/app_release_cp.txt)" \
    -Xplugin="${CC_JAR}" \
    -Xjava-source-roots=app/src/main/java,app/build/generated/source/buildConfig/release,app/build/generated/ksp/release/java \
    -d "${KOTLIN_OUT}" \
    $(find app/src/main/java app/build/generated/ksp/release/kotlin -name "*.kt" 2>/dev/null)
fi

echo ">> 3b/5 javac + ASM/Hilt transform + project dexing (standalone-compiled classes)..."
./gradlew :app:transformReleaseClassesWithAsm :app:mergeReleaseGeneratedProguardFiles :app:dexBuilderRelease \
    $APP_KOTLIN_EXCLUDE $LINT_EXCLUDES -x :app:packageRelease "${GFLAGS[@]}"

EXT_OUT=app/build/intermediates/external_libs_dex/release/mergeExtDexRelease
if [ "${WARRIOR_MINIFY:-0}" = "1" ]; then
if [ ! -f "${DEX_OUT}/classes.dex" ]; then
  echo ">> 4/5 standalone R8 (collecting consumer rules first)..."
  cat > /tmp/warrior-cpdump.init.gradle <<'EOF2'
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
EOF2
  ./gradlew -I /tmp/warrior-cpdump.init.gradle help -q "${GFLAGS[@]}" >/dev/null 2>&1 || true
  (cd "${APP_CLASSES}/dirs" && jar cf /tmp/warrior-app-release-classes.jar .)
  rm -rf /tmp/r8_aar_confs && mkdir -p /tmp/r8_aar_confs
  find "${GRADLE_CACHE}/modules-2" -name "*.aar" 2>/dev/null | while read -r aar; do
    unzip -p "${aar}" proguard.txt > "/tmp/r8_aar_confs/$(basename "${aar}" .aar).pro" 2>/dev/null || true
  done
  find /tmp/r8_aar_confs -name "*.pro" -size +0 2>/dev/null | sort -u > /tmp/r8_consumer_confs.txt
  find "${GRADLE_CACHE}" -path "*transforms*" -name "proguard.txt" 2>/dev/null | sort -u >> /tmp/r8_consumer_confs.txt
  CONF_COUNT=$(wc -l < /tmp/r8_consumer_confs.txt)
  if [ "${CONF_COUNT}" -lt 5 ]; then
    echo "FATAL: only ${CONF_COUNT} consumer rule files found — refusing to run R8."
    exit 1
  fi
  echo "   collected ${CONF_COUNT} consumer rule files"
  CONFS=""
  while read -r c; do CONFS="${CONFS} --pg-conf ${c}"; done < /tmp/r8_consumer_confs.txt
  rm -rf "${DEX_OUT}" "${MAP_OUT}" && mkdir -p "${DEX_OUT}" "${MAP_OUT}"
  kill_jvms
  "${JAVA_HOME}/bin/java" ${R8_JVM} -cp "${AGP_JAR}" com.android.tools.r8.R8 \
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
  grep -q "^com.warrior.data.local.database.WarriorDatabase_Impl -> com.warrior.data.local.database.WarriorDatabase_Impl:" "${MAP_OUT}/mapping.txt" \
    || { echo "FATAL: Room *_Impl stripped/renamed — consumer rules missing?"; exit 1; }
fi
PACKAGE_EXCLUDES=""
else
if [ ! -f "${EXT_OUT}/classes.dex" ]; then
  echo ">> 4/5 standalone D8 merge of external dex archives (un-minified release)..."
  cat > /tmp/warrior-dump-rel.init.gradle <<'EOF2'
gradle.projectsEvaluated {
    def t = gradle.rootProject.project(":app").tasks.findByName("mergeExtDexRelease")
    if (t != null) {
        new File("/tmp/mergeExtDexRelease_inputs.txt").text =
            t.inputs.files.files.collect { it.absolutePath }.join("\n")
    }
}
EOF2
  ./gradlew -I /tmp/warrior-dump-rel.init.gradle help -q "${GFLAGS[@]}" >/dev/null 2>&1 || true
  mkdir -p "${EXT_OUT}"
  # The inputs list contains marker dirs (duplicate-classes check) that may not
  # exist yet; keep only real paths before feeding find.
  : > /tmp/mergeExtDexRelease_inputs_existing.txt
  while IFS= read -r p; do [ -e "$p" ] && echo "$p" >> /tmp/mergeExtDexRelease_inputs_existing.txt; done < /tmp/mergeExtDexRelease_inputs.txt
  kill_jvms
  find $(cat /tmp/mergeExtDexRelease_inputs_existing.txt | tr '\n' ' ') -name "*.dex" > /tmp/ext_dex_release_files.txt
  "${JAVA_HOME}/bin/java" -Xmx880m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:ActiveProcessorCount=1 \
    -cp "${ANDROID_HOME}/build-tools/35.0.0/lib/d8.jar" com.android.tools.r8.D8 \
    --release --min-api 24 --output "${EXT_OUT}" $(cat /tmp/ext_dex_release_files.txt | tr '\n' ' ')
fi
# Final dex merge (project + external) also exceeds the daemon's memory here:
MERGE_OUT=app/build/intermediates/dex/release/mergeDexRelease
if [ ! -f "${MERGE_OUT}/classes.dex" ]; then
  echo ">> 4b/5 standalone D8 final merge (exact mergeDexRelease inputs)..."
  cat > /tmp/warrior-mergedex.init.gradle <<'EOF2'
gradle.projectsEvaluated {
    def t = gradle.rootProject.project(":app").tasks.findByName("mergeDexRelease")
    if (t != null) {
        new File("/tmp/mergedex_inputs.txt").text = t.inputs.files.files.collect { it.absolutePath }.join("\n")
    }
}
EOF2
  ./gradlew -I /tmp/warrior-mergedex.init.gradle help -q "${GFLAGS[@]}" >/dev/null 2>&1 || true
  : > /tmp/mergedex_existing.txt
  while IFS= read -r p; do [ -e "$p" ] && echo "$p" >> /tmp/mergedex_existing.txt; done < /tmp/mergedex_inputs.txt
  kill_jvms
  mkdir -p "${MERGE_OUT}"
  "${JAVA_HOME}/bin/java" -Xmx880m -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:ActiveProcessorCount=1 \
    -cp "${ANDROID_HOME}/build-tools/35.0.0/lib/d8.jar" com.android.tools.r8.D8 \
    --release --min-api 24 --output "${MERGE_OUT}" \
    $(find $(cat /tmp/mergedex_existing.txt | tr '\n' ' ') -name "*.dex" 2>/dev/null)
fi
PACKAGE_EXCLUDES="-x :app:mergeExtDexRelease -x :app:mergeDexRelease"
fi

echo ">> 5/5 packaging + signing..."
./gradlew :app:assembleRelease $APP_KOTLIN_EXCLUDE $LINT_EXCLUDES $PACKAGE_EXCLUDES "${GFLAGS[@]}"
ls -la app/build/outputs/apk/release/app-release.apk
