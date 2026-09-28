# Phase 22: Deferred Items (out-of-scope discoveries)

Discoveries made during execution that are outside the current task's scope. Not
fixed here; carried for follow-up planning.

## 1. README consumer repositories omit `google()` — transitive androidx nodes fail

- **Found during:** Plan 22-18 Task 1 (DEP-01 scratch-consumer check, 2026-09-28)
- **What:** The README dependency-setup snippet instructs consumers to use
  `mavenCentral()` + `maven("https://jitpack.io")` only. The published artifact's
  compile-scope Compose dependencies (`org.jetbrains.compose.desktop:desktop-jvm`
  1.12.0 etc.) transitively require `androidx.*` artifacts
  (`androidx.collection:collection:1.5.0`, `androidx.annotation:annotation:1.9.1`,
  `androidx.compose.runtime:runtime:1.12.0`, `androidx.lifecycle:*`,
  `androidx.savedstate:*`, `androidx.navigationevent:*` …) that are NOT on Maven
  Central (verified: HTTP 404 on repo1.maven.org for collection 1.5.0 and
  annotation 1.9.1) — they resolve only from `google()`. A scratch consumer with
  the README's exact repository set renders those nodes `FAILED` in
  `dependencies --configuration runtimeClasspath`; adding `google()` gives 0 FAILED
  and BUILD SUCCESSFUL. The library's own `settings.gradle.kts` uses
  `mavenCentral() + google()`.
- **Scope decision:** not fixed in 22-18 — README belongs to Plan 22-17 (REL-06,
  already complete) and the gap is pre-existing: the published `v3.1.0` POM carries
  the identical compile-scope Compose dependency list (verified by downloading the
  v3.1.0 POM), so nothing about 3.2.0 introduces it. Real consumers with any
  Android/Compose background typically already declare `google()`, which is why it
  went unnoticed.
- **Suggested follow-up:** one-line README fix (add `google()` to the snippet) —
  candidate for the v3.2 gap list or the next docs pass.
- **Evidence:** `build/aero-jitpack-verify-320/consumer-runtimeClasspath-readme-repos.txt`
  (FAILED nodes) and `build/aero-jitpack-verify-320/consumer-runtimeClasspath-with-google.txt`
  (0 FAILED), both git-ignored scratch artifacts; Maven Central 404 checks recorded
  above.
