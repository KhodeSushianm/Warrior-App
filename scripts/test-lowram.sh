#!/usr/bin/env bash
# Runs :data:local unit tests (Robolectric) in a standalone JVM.
# Needed on machines with <=1GB RAM where Gradle daemon + test worker cannot
# coexist. On normal machines prefer: ./gradlew :data:local:testDebugUnitTest
set -euo pipefail
cd "$(dirname "$0")/.."

export JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}
export ANDROID_HOME=${ANDROID_HOME:-/opt/android-sdk}

cat > /tmp/warrior-testcp.init.gradle <<'EOF'
gradle.projectsEvaluated {
    def p = gradle.rootProject.project(":data:local")
    p.tasks.register("dumpTestCp") {
        dependsOn p.tasks.named("transformDebugUnitTestClassesWithAsm")
        doLast {
            def t = p.tasks.findByName("testDebugUnitTest")
            new File("/tmp/warrior_test_cp.txt").text =
                t.classpath.files.collect { it.absolutePath }.join(File.pathSeparator)
        }
    }
}
EOF

echo ">> resolving test classpath..."
./gradlew -I /tmp/warrior-testcp.init.gradle :data:local:dumpTestCp \
    -Dorg.gradle.jvmargs="-Xmx640m -XX:MaxMetaspaceSize=320m -XX:ReservedCodeCacheSize=64m -Xss512k" -q >/dev/null 2>&1 || true

echo ">> running WarriorDatabaseTest in standalone JVM (850m heap)..."
# AGP test_config.properties uses module-relative paths, so cwd must be the module dir.
cd data/local
java -Xmx850m -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8 \
    -cp "$(cat /tmp/warrior_test_cp.txt)" \
    org.junit.runner.JUnitCore com.warrior.data.local.WarriorDatabaseTest
