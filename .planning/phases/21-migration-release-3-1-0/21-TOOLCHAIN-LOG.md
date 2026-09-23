# Phase 21: Toolchain Migration Log

Running record of every toolchain/dependency bump in Phase 21, gated by the TOOL-16 test-count
guard before and after each step.

## Starting toolchain (before any bump)

| Component | Version |
|-----------|---------|
| Kotlin | 2.4.10 |
| Compose Multiplatform | 1.11.1 |
| Gradle (wrapper) | 8.14.3 |
| JDK toolchain | 17 |
| kotlinx-coroutines | 1.10.2 |
| kotlinx-datetime | 0.6.2 |
| JUnit | 5.10.0 (jupiter) |
| Material3 | 1.9.0 (pinned) |

## Step 2 — Test count guard (TOOL-16)

**Measured total / skipped:** `./gradlew :library:test --rerun` on the toolchain above printed
`AERO_TEST_COUNT total=541 skipped=0 filtered=false` — the live number, not the 467 carried from
v3.0's closeout, not the 474 `@Test`-annotation count from 21-RESEARCH.md's own count, and not any
other document value.

**Grep comparison and explained delta:** `grep -rc "@Test" library/src/test --include=*.kt` summed
to 545, 4 more than the Gradle-executed total of 541. Fully explained: two files —
`VER01GradientProportionalitySourceTest.kt` and `VER02AeroSurfaceClipOrderSourceTest.kt` — each
contain the literal text `` @Test `` twice inside KDoc prose (describing the RED/GREEN fixture
halves of their own source-scan gate), which `grep -c "@Test"` counts as a match but JUnit does not
discover as a test method:
- `VER01GradientProportionalitySourceTest.kt`: 11 grep matches, 9 real `@Test`-annotated methods
  (2 KDoc mentions at lines 29, 31).
- `VER02AeroSurfaceClipOrderSourceTest.kt`: 10 grep matches, 8 real `@Test`-annotated methods
  (2 KDoc mentions at lines 27, 29).
- 2 + 2 = 4, matching the 545 − 541 delta exactly. No parameterized/disabled/repeated tests exist
  in the suite (`grep -rl "@ParameterizedTest\|@TestFactory\|@RepeatedTest\|@Disabled\|@Ignore"`
  returned zero files), so the delta has no other source.

**Red run — guard proven to fail on a deliberately excluded test class:**
`AeroCalendarGridTest.kt` (7 `@Test` methods) was temporarily excluded via
`exclude("**/AeroCalendarGridTest*")` inside `tasks.test { }`. `./gradlew :library:test --rerun`
printed `AERO_TEST_COUNT total=534 skipped=0 expected=541 expectedSkipped=0 filtered=false`
(541 − 7 = 534, exactly the excluded class's method count) and the build failed with the verbatim
guard line:

```
Test count guard: executed total=534 skipped=0, locked total=541 skipped=0
```

**Green run — guard passes after the exclusion is removed:** the exclusion was removed
(`git diff --quiet HEAD -- library/build.gradle.kts` confirmed the file is byte-identical to the
Task 1 commit before this run). `./gradlew :library:test --rerun` printed the verbatim line:

```
AERO_TEST_COUNT total=541 skipped=0 expected=541 expectedSkipped=0 filtered=false
```

and the build was `BUILD SUCCESSFUL`.

**Filtered-run check (not enforced):** `./gradlew :library:test --rerun --tests "*Base05CaptureProofTest*"`
printed `AERO_TEST_COUNT total=2 skipped=0 expected=541 expectedSkipped=0 filtered=true` and passed
— confirming opt-in filtered/capture runs are unaffected by the guard.

**Locked constants:** `library/build.gradle.kts` — `lockedTestTotal = 541`, `lockedTestSkipped = 0`.

**Gate command for every later step in this phase:**

```
./gradlew :library:test --rerun
```

Every version bump from here on must be followed by this exact command before the next bump; any
non-zero exit code (including a Test count guard failure) stops the bump and is investigated before
proceeding.
