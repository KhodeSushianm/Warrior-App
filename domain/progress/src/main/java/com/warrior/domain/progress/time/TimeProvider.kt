package com.warrior.domain.progress.time

/**
 * Injectable time source for the Progress Engine (EXECUTION-PLAN Phase 7:
 * "tests with a fixed clock"). Pure JVM on purpose — `java.time.Clock` needs
 * API 26 and the app ships minSdk 24 without core-library desugaring
 * (owner decision recorded in Phase 6). The app module provides the system
 * implementation; tests provide fixed values.
 */
fun interface TimeProvider {
    fun nowMillis(): Long
}
