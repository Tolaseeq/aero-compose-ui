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

## Step 3a — Gradle 9.7.1 (TOOL-09)

**Removed lines (`settings.gradle.kts`):** both occurrences of
`maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")` — one inside
`pluginManagement.repositories`, one inside `dependencyResolutionManagement.repositories`.

**Precondition proof (on the still-untouched Gradle 8.14.3 wrapper, before bumping the wrapper):**
`./gradlew build --refresh-dependencies` printed `BUILD SUCCESSFUL in 36s` with the stale repo
already removed from `settings.gradle.kts` — confirming nothing was actually being resolved from
`maven.pkg.jetbrains.space`.

**Checksum:** fetched directly from `https://services.gradle.org/distributions/gradle-9.7.1-bin.zip.sha256`:
```
acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a
```

**Wrapper bump:** `./gradlew wrapper --gradle-version 9.7.1 --distribution-type bin --gradle-distribution-sha256-sum acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a`
run twice (first pass on 8.14.3 rewrote `gradle-wrapper.properties`; second pass executed on 9.7.1
itself and rewrote `gradlew`/`gradlew.bat`/`gradle-wrapper.jar`). The second pass's first attempt hit
a transient `SocketTimeoutException` (10000ms `networkTimeout`) downloading the distribution zip; a
plain retry of the identical command succeeded.

**`./gradlew --version` output:**
```
Gradle 9.7.1
Build time:    2026-08-19 14:16:09 UTC
Revision:      92f0512e7f06d84621afba191f75e265363890cf
Kotlin:        2.4.0
Groovy:        4.0.32
Ant:           Apache Ant(TM) version 1.10.17 compiled on April 6 2026
Launcher JVM:  21.0.9 (Eclipse Adoptium 21.0.9+10-LTS)
Daemon JVM:    C:\Users\1\.jdks\ms-21.0.9 (from org.gradle.java.home)
OS:            Windows 11 10.0 amd64
```

**`grep -c "maven.pkg.jetbrains.space" settings.gradle.kts`:** `0`

**Gate:** `./gradlew build --refresh-dependencies` → `BUILD SUCCESSFUL in 1m 5s`; then
`./gradlew :library:test --rerun` printed
`AERO_TEST_COUNT total=541 skipped=0 expected=541 expectedSkipped=0 filtered=false` and
`BUILD SUCCESSFUL`.

**Warnings printed by the build (verbatim, `--warning-mode all`):** one Gradle-level deprecation, unrelated
to the repo removal — the project still targets `jvmToolchain(17)` at this step (JDK 21 lands in Step
3b), and Gradle 9.7.1 warns about auto-provisioned toolchains without a configured toolchain
repository:
```
Using toolchain 'Eclipse Temurin JDK 17 (17.0.17+10)' installed via auto-provisioning without toolchain repositories. This behavior has been deprecated. This will fail with an error in Gradle 10. Builds may fail when this toolchain is not available in other environments. Add toolchain repositories to this build. For more information, please refer to https://docs.gradle.org/9.7.1/userguide/toolchains.html#sub:download_repositories in the Gradle documentation.
```
No action taken on this warning at this step: it names JDK 17, which Step 3b removes entirely by
moving both modules to `jvmToolchain(21)` (JBR 21 is already installed, not auto-provisioned). All
other warnings printed during compilation (`runComposeUiTest` v1 deprecation across ~40 existing test
files, a handful of unnecessary `!!` assertions, one `@ConsistentCopyVisibility` note) are pre-existing
in the codebase, unrelated to this bump, and were not touched (scope boundary — see deviation rules).
The build also printed a generic `Deprecated Gradle features were used in this build, making it
incompatible with Gradle 10` banner; re-run with `--warning-mode all` resolved it to the single
toolchain-provisioning line above (no other Gradle-level deprecations found).

**Environment note (not a code deviation):** the first `./gradlew build --refresh-dependencies` attempt
on this wrapper failed with `Unable to delete file '...\library\build\libs\library-3.0.0.jar'`
(`ERROR_SHARING_VIOLATION`) — an external VS Code Kotlin Language Server process (`fwcd.kotlin`,
`org.javacs.kt.MainKt`) had the jar open as part of its classpath indexing. Per the global stop rule,
the executor did not attempt to kill or work around that process; the orchestrator/maintainer ended it,
after which the same command succeeded unmodified. No build or code file needed any change for this.

**Commit:** `build(21-06): Gradle 9.7.1 wrapper, drop stale JetBrains Space repo (TOOL-09)`.

## Step 3b — JDK 21 (TOOL-10)

**Changed:** `jvmToolchain(17)` → `jvmToolchain(21)` in `library/build.gradle.kts` and
`showcase/build.gradle.kts` — no other lines touched in either file (`explicitApi()` in `:library`
kept as-is).

**`./gradlew -q javaToolchains` (before choosing anything):** lists five distinct JDK 21 installs
(Eclipse Temurin 21.0.9 at `C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot`, JetBrains JDK
21.0.9 at `C:\Users\1\.jdks\jbr-21.0.9`, Microsoft JDK 21.0.9 at `C:\Users\1\.jdks\ms-21.0.9`) plus
JDK 8/17/25 — confirming JDK 21 is already Gradle-visible without foojay, per MCP-HOWTO.md.

**`jitpack.yml`:** `jdk: [openjdk21]`; SDKMAN identifier fetched live from
`https://api.sdkman.io/2/candidates/java/linuxx64/versions/list?installed=`, not guessed — highest
`21.0.*-tem` entry in the returned Temurin row was `21.0.12+1.1-tem`:
```
jdk:
  - openjdk21
before_install:
  - sdk install java 21.0.12+1.1-tem
  - sdk use java 21.0.12+1.1-tem
```

**Gate:** `./gradlew build --console=plain` → `BUILD SUCCESSFUL in 31s`; `./gradlew :library:test --rerun`
printed `AERO_TEST_COUNT total=541 skipped=0 expected=541 expectedSkipped=0 filtered=false` and
`BUILD SUCCESSFUL in 11s`.

**Bytecode check:** `./gradlew :library:jar :library:generateMetadataFileForMavenPublication` →
`BUILD SUCCESSFUL`. First class in the jar:
```
> jar.exe tf library/build/libs/library-3.0.0.jar | grep "\.class$" | head -1
com/mordred/aero/components/buttons/AeroButtonKt.class
```
```
> javap.exe -v -cp library/build/libs/library-3.0.0.jar com.mordred.aero.components.buttons.AeroButtonKt
  minor version: 0
  major version: 65
```
(major version 65 = Java 21, both jar and javap run from `C:/Users/1/.jdks/jbr-21.0.9/bin`.)

**Metadata check:** `grep -c '"org.gradle.jvm.version": 21' library/build/publications/maven/module.json`
→ `2` (present on both the `apiElements`/`runtimeElements` variants).

**Warnings:** a full clean rebuild (`rm -rf library/build showcase/build build`, then
`./gradlew build --refresh-dependencies --console=plain --warning-mode all`) printed **zero**
deprecation/warning lines — the Step 3a `jvmToolchain(17)` auto-provisioning warning is gone now that
both modules resolve `jvmToolchain(21)` against an already-installed JDK (not auto-provisioned).

**Commit:** `build(21-06): JDK 21 toolchain for library, showcase and JitPack (TOOL-10)`.

## Step 3c — Kotlin 2.4.20 + Compose Multiplatform 1.12.0 (TOOL-11, TOOL-12, TOOL-17)

**Catalog change (`gradle/libs.versions.toml`):** `kotlin = "2.4.10"` → `"2.4.20"`,
`composeMultiplatform = "1.11.1"` → `"1.12.0"` (compose-compiler plugin follows `kotlin` via
`version.ref`). `kotlinxCoroutines`, `kotlinxDatetime`, `junit` untouched — out of scope for this
commit (Plans 07+ per the roadmap's 7-step order).

**Gate:** `./gradlew build --console=plain` → `BUILD SUCCESSFUL in 1m 22s` on the first attempt, no
mechanical fixes needed for any CMP 1.12.0/Kotlin 2.4.20 API change. `./gradlew :library:test --rerun`
printed `AERO_TEST_COUNT total=541 skipped=0 expected=541 expectedSkipped=0 filtered=false` and
`BUILD SUCCESSFUL in 19s` — no test failures, no timeouts, no hangs; Pitfall 3
(`uiTest`'s `StandardTestDispatcher` default change) did not materialize against this suite.

**Warnings (verbatim, full clean rebuild with `--warning-mode all`):** no Gradle-level deprecation
banner and no Gradle-9.7.1-vs-Kotlin's-9.7.0-ceiling warning appeared on this build. The compiler
(`w:`) warnings are the same pre-existing set already recorded in Step 3a (v1 `runComposeUiTest`
deprecation across the existing test files, a handful of unnecessary `!!` assertions, the
`ExperimentalComposeLibrary`/`uiTest` version-catalog note, one `@ConsistentCopyVisibility` note) —
none of them newly introduced by this bump, all pre-existing and out of this commit's scope. One
warning is new and CMP-1.12-specific, in showcase code untouched by this plan:
```
w: file:///C:/1A_WORK/ui_lib/showcase/src/main/kotlin/com/mordred/showcase/sections/IconsSection.kt:325:21 'val LocalClipboardManager: ProvidableCompositionLocal<ClipboardManager>' is deprecated. Use LocalClipboard instead which supports suspend functions.
```
Recorded, not fixed — out of this task's file scope (`IconsSection.kt` is not in this task's files
list) and non-blocking (deprecation warning, not a compile error).

**Material3 gate:** `tools/verify/check-material3.sh` created (Git Bash, `set -euo pipefail`) — runs
`dependencyInsight --dependency material3` on `:library`/`:showcase` × `compileClasspath`/
`runtimeClasspath` (4 runs) plus `:showcase:dependencies`, appends everything to the given log file,
fails on any case-insensitive `alpha` match or a missing
`org.jetbrains.compose.material3:material3:1.9.0` line. Run:
```
bash tools/verify/check-material3.sh build/aero-m3-plan06.log
MATERIAL3 OK
```
All four `dependencyInsight` runs resolved exactly `org.jetbrains.compose.material3:material3:1.9.0`
(and its `-desktop` artifact of the same version); `:showcase:dependencies`' full graph contained no
`alpha` string anywhere. No `resolutionStrategy.force` needed.

**TOOL-17 smoke:**
```
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools/capture/Invoke-ShowcaseSweep.ps1 -Themes AeroBlue -Sections Buttons -Captures 1 -OutDir C:\1A_WORK\ui_lib\.captures\smoke\plan06-cmp1.12
AERO_SWEEP_DONE frames=1 out=C:\1A_WORK\ui_lib\.captures\smoke\plan06-cmp1.12
```
`manifest.json` records `"composeMultiplatform": "1.12.0"`, `"gradle": "9.7.1"`,
`"jvm": "21.0.9/Microsoft"`, commit `11d1135`. The launch log's ready line:
```
AERO_READY scheme=AeroBlue section=Buttons page=0 pages=1 viewportPx=768 contentPx=768 scrollPx=0 background=FF0D1B2A jvm=21.0.9/Microsoft
```
`AeroBlue/Buttons-p0-c1.png` was read directly and visually confirms the Buttons section rendered
correctly (AeroButton/AeroOutlinedButton/AeroIconButton/AeroToolbar, glass surfaces, AeroBlue theme
intact) — no launch failure, no blank/black frame. This folder did not exist before this run (created
fresh under `.captures/smoke/`, nothing pre-existing overwritten, nothing under `.captures/`'s
pre-upgrade baseline touched).

**Commit:** `build(21-06): Kotlin 2.4.20 + Compose Multiplatform 1.12.0 (TOOL-11, TOOL-12)`.
