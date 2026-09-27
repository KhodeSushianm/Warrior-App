# WARRIOR release rules (Phase 10 — Hardening + Release RC).
#
# R8 runs in full mode (AGP 8 default). Room, Hilt/Dagger, Compose,
# kotlinx-serialization and Navigation all ship consumer ProGuard rules,
# so the app only adds crash-diagnostics attributes here. Anything further
# must be justified and covered by the release smoke checklist.

# Keep source file names + line numbers so stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
