# Phase 15: Toolchain Upgrade - Research

**Researched:** 2026-07-22
**Domain:** Compose Multiplatform (Desktop/JVM) + Kotlin toolchain migration for a published Maven/JitPack library
**Confidence:** HIGH for version numbers, dates, Maven coordinates, and Kotlin/Gradle floors (all machine-verified this session via GitHub Releases API, Maven Central `maven-metadata.xml`, and official CHANGELOG.md — not memory). MEDIUM-HIGH for the `dropShadow`/`innerShadow`/`Shadow` exact signatures (cross-checked across three independent sources including a doc snapshot dated 2026-07-19, three days before this research — but still not a hands-on compile against the actual 1.11.1 jar; residual uncertainty flagged explicitly and delegated to the phase's own build-time verification step, TOOL-07).

This document is a **continuation, not a restart**, of `.planning/research/UPGRADE.md`. That document's version matrix, Material3 alias table, test-infra findings, and `dropShadow`/`innerShadow` signatures are the primary source and are cited, not re-derived. This session's job was narrower: (1) re-verify UPGRADE.md's MEDIUM-HIGH-confidence shadow signatures against fresher sources, (2) nail the exact stable Material3 coordinate via direct `maven-metadata.xml` inspection, (3) confirm the CMP 1.11 test-infra change against the primary CHANGELOG.md text directly, (4) confirm Kotlin 2.4.10's Gradle/JDK floor against official kotlinlang docs. **UPGRADE.md's own top-line recommendation (target 1.9.3) is superseded** by the locked decision (target 1.11.1) — its facts are used here, its recommendation is not.

## Summary

Phase 15 executes a gated, single-shot toolchain bump: Kotlin 2.1.21 → 2.4.10, Compose Multiplatform 1.7.3 → 1.11.1, with an explicit stable Material3 pin replacing the `compose.material3` alias, a ported and re-proven `AeroPanelGroupRecomposeUiTest`, and a committed `dropShadow`/`innerShadow` scratch composable as a Phase 16 handoff artifact. Every fact in UPGRADE.md's compatibility matrix was re-verified this session against live sources (GitHub Releases API, Maven Central metadata, official CHANGELOG.md, kotlinlang.org) and holds up unchanged. Two gaps UPGRADE.md flagged as needing re-verification are now closed to the highest achievable confidence without a real compile: the exact `dropShadow`/`innerShadow`/`Shadow` signatures (re-confirmed via a fresh, unversioned doc snapshot dated 2026-07-19 — 3 days before this research, i.e., reflecting current 1.11.1-era API, not the 1.9.0-rc01 snapshot UPGRADE.md used), and the precise stable Material3 Maven coordinate to pin (`org.jetbrains.compose.material3:material3:1.9.0` — confirmed as the newest **stable** entry in the artifact's own `maven-metadata.xml`, with everything above it in the 1.10.x/1.11.x/1.12.x lines being alpha-only).

**Primary recommendation:** Run the build gate first (`./gradlew build` on Kotlin 2.4.10 + CMP 1.11.1 with only the three known-safe fixes applied — Material3 pin, compose-compiler version-ref alignment, and Gradle/JDK left unchanged since both are already sufficient); only escalate to the named fallbacks if that gate still fails afterward. Do not guess at the `dropShadow`/`innerShadow` signature in production code — use the scratch composable to compile-verify it first, exactly as TOOL-07 requires.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Target pairing (carried forward — locked, do not re-litigate)**
- Target **CMP 1.11.1 + Kotlin 2.4.10**, chosen consciously over the research's own conservative recommendation of CMP 1.9.3 (see `SUMMARY.md` "Superseded Findings" #2 — the user accepted all three consequences: Kotlin 2.2+ floor, explicit stable-M3 pin, and the `AeroPanelGroupRecomposeUiTest` port + re-proof).
- The `./gradlew build` on this pairing is the **first task of the phase** — the pair was never shipped matched by JetBrains (CMP 1.11.1 published ~6 weeks before Kotlin 2.4.10; the Kotlin-2.4-aligned CMP line is 1.12.x, prerelease). Outcome is genuinely unverified until run.
- Three named fallbacks, in preference order, only if the gate fails: **(a)** Kotlin 2.4.10 + CMP `1.12.0-beta02` (matched but prerelease); **(b)** CMP 1.11.1 + newest Kotlin it actually accepts; **(c)** CMP `1.9.3` + Kotlin unchanged. **Any fallback is escalated to the user, never substituted silently** — it walks back an explicit locked decision.

**Escalation threshold (when the build gate fails)**
- **Apply known-safe fixes first, then escalate only if the pairing genuinely won't compile after them.** "Gate failed → escalate" means the pair does not compile *after* the expected mechanical migration work is done — not the first red build.
- Known-safe fixes that count as normal migration work (NOT a gate failure): the explicit Material3 pin (TOOL-02 requires it anyway), aligning the `compose-compiler` (`org.jetbrains.kotlin.plugin.compose`) plugin version to the new `kotlin` catalog entry, and confirming/adjusting Gradle if a compile demands it.
- If, after those, the Kotlin 2.4.10 + CMP 1.11.1 pair still does not compile → **stop and escalate to the user with the three named fallbacks**, reporting what was tried. Do not pick a fallback autonomously.

**Scratch composable (TOOL-07)**
- **Commit the `dropShadow`/`innerShadow` scratch composable as a seed/reference for Phase 16**, not throwaway. It carries the signature *confirmed (or corrected) against the real 1.11.1 jar*, so Phase 16's Aero-primitives work consumes a proven basis instead of re-verifying from documentation.
- This is the deliberate handoff artifact across the Phase 15→16 boundary. Placement (showcase scratch section vs a dedicated file) is Claude's discretion, but it must compile against the real artifact and the confirmed signature must be readable by the Phase 16 planner.

**Release & JitPack (TOOL-08)**
- **Cut a throwaway pre-release git tag now** (e.g. `v3.0.0-alpha01`) to verify the JitPack build on the new toolchain immediately within Phase 15.
- **Do NOT bump the real version** in `build.gradle.kts` — it stays `2.0.4` (root `build.gradle.kts:4`) until `/gsd:complete-milestone`, per the locked bump-on-milestone rule. JitPack derives the artifact coordinate from the git tag (`com.github.Tolaseeq:aero-compose-ui:v3.0.0-alpha01`), so the pre-release tag proves the build without touching the Gradle version line.

**Baseline capture for "no visual change" (TOOL-06)**
- **Capture before/after screenshots** of the showcase on all three themes (AeroBlue / AeroDark / Classic): snapshot BEFORE the migration, compare AFTER. A rigorous diff, not an eyes-on-from-memory pass.
- Rationale: Phase 15 touches zero rendering code, but Skia jumps **m126 → m138 → m144** across 1.7.3→1.11.1 (two milestone bumps), so subtle toolchain-induced rendering drift is possible and must be actively looked for.

### Claude's Discretion
- Exact placement/structure of the committed scratch composable (as long as it compiles against the real jar and exposes the confirmed signature to Phase 16).
- Whether to port `AeroPanelGroupRecomposeUiTest` to the CMP 1.11 "v2" `runComposeUiTest` API vs keeping the deprecated-but-working v1 — provided TOOL-03/TOOL-04 are satisfied (test is green AND re-proven to FAIL on reverted non-`@Composable` DSL, accounting for the `Unconfined`→`Standard` `TestDispatcher` default change).
- Screenshot tooling/mechanism for the before/after baseline (no Compose Desktop screenshot-regression framework exists — manual capture is expected).
- Single-shot bump (1.7.3 → 1.11.1 directly) vs stepping — research found no repo-relevant breaking changes in the 1.8.x/1.9.x/1.10.x lines; stepping buys no safety here.

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope. All visual/rendering work, including `GlassModifiers.kt` fixes, is Phase 16+ by explicit milestone design. Explicitly NOT in this phase: any change to rendering/drawing code, `GlassModifiers.kt` bug fixes, any of the eight component restyles, any new visual capability.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-------------------|
| TOOL-01 | Project builds (`./gradlew build`) on Kotlin 2.4.10 + CMP 1.11.1, as the first task; escalate on failure after known-safe fixes | Part 1 (version confirmation), Part 2 (Kotlin/Gradle/JDK floors — all sufficient, no gate-blocking floor found), Architecture Patterns sequencing |
| TOOL-02 | Material3 pinned to an explicit **stable** coordinate; `compose.material3` alias not used as version source | Part 3 — exact coordinate `org.jetbrains.compose.material3:material3:1.9.0` confirmed via direct `maven-metadata.xml` inspection this session |
| TOOL-03 | `AeroPanelGroupRecomposeUiTest` ported for CMP 1.11 test-infra changes (v1 deprecation, `Unconfined`→`Standard` dispatcher default) | Part 4 — re-confirmed directly against CHANGELOG.md raw text this session (not paraphrase) |
| TOOL-04 | Ported guard re-proven to FAIL on unbroken code (temporarily revert non-`@Composable` DSL fix, observe duplication, restore) | Common Pitfalls (false-positive test-infra pitfall); Code Examples (re-proof sequence) |
| TOOL-05 | Full library test suite green (232 tests incl. 12 `PanelGroupLogicTest`) | Architecture Patterns (gated sequence step 5); Validation Architecture |
| TOOL-06 | Showcase compiles/launches; three-theme smoke pass shows no change vs. pre-migration baseline | Architecture Patterns (gated sequence step 6); before/after screenshot baseline (Skia m126→m138→m144) |
| TOOL-07 | `dropShadow`/`innerShadow` signatures confirmed by compiling a scratch composable against the real 1.11.1 artifact, not documentation | Part 3-Shadow (this session's re-verified signature); Code Examples (scratch composable) |
| TOOL-08 | JitPack build passes on the new toolchain | Part 2-JDK (jitpack.yml `openjdk17` sufficiency confirmed); Architecture Patterns (pre-release tag step) |
</phase_requirements>

## Standard Stack

### Core

| Coordinate / Catalog entry | Version | Purpose | Why this exact version |
|---|---|---|---|
| `kotlin` (`libs.versions.toml`) | `2.4.10` | Language/compiler | Locked target. Published 2026-07-14, `prerelease: false` (GitHub Releases API, verified this session). Satisfies CMP 1.11.x's "Kotlin language version and API version 2.2" floor with wide margin. |
| `composeMultiplatform` (`libs.versions.toml`) | `1.11.1` | UI/rendering framework | Locked target. Published 2026-06-02, `prerelease: false` (GitHub Releases API, verified this session — same query re-run, unchanged from UPGRADE.md's finding). Latest stable overall as of this research date; `1.12.0-beta02` (newest artifact on Maven Central) is explicitly `prerelease: true` — do not use. |
| `org.jetbrains.compose.material3:material3` | **`1.9.0`** | Material3 component library (explicit pin, replaces `compose.material3` alias) | **Confirmed this session by directly fetching `repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml`**: the full version list runs `...1.9.0-beta06, 1.9.0, 1.10.0-alpha01..05, 1.11.0-alpha01..07, 1.12.0-alpha01..03` — `1.9.0` is the newest entry that does not contain `alpha`/`beta`/`rc`. Every version at or above `1.10.0-alpha01` is alpha-only; **no stable 1.10.x/1.11.x/1.12.x material3 artifact exists on Maven Central as of this research date.** This is a *stronger* form of UPGRADE.md's finding (which stated the alias resolves to `1.11.0-alpha07` at CMP 1.11.x) — it is not just that the alias points to an alpha, it's that **no stable alternative newer than 1.9.0 exists to pin to instead.** Pin exactly this coordinate. |
| `compose-compiler` (`libs.versions.toml`, plugin `org.jetbrains.kotlin.plugin.compose`) | tracks `kotlin` via `version.ref = "kotlin"` | Compose compiler plugin | Already wired to auto-track the `kotlin` catalog entry (`gradle/libs.versions.toml:18`) — bumping `kotlin = "2.4.10"` moves this automatically. No separate action needed beyond confirming the catalog entry after the bump (the "known-safe fix" the CONTEXT.md escalation threshold names is really just verifying this stays true, not a new line to write). |
| Gradle (wrapper) | `8.14.3` (unchanged) | Build tool | **Confirmed sufficient this session**: official kotlinlang.org Gradle compatibility table states Kotlin Gradle Plugin 2.4.x requires **minimum Gradle 7.6.3, maximum fully-supported 9.5.0**. This repo's wrapper (`gradle/wrapper/gradle-wrapper.properties`) is already `8.14.3` — comfortably inside that range. No Gradle bump required. |
| JDK (`jvmToolchain`) | `17` (unchanged, both `library/build.gradle.kts:9` and `showcase/build.gradle.kts:10`) | Compile/runtime JDK | CMP Desktop requires JDK 11+ to run, JDK 17+ for `jpackage` (`showcase` already needs this for `TargetFormat.Exe/Deb`). Kotlin 2.4.x itself imposes no higher JDK floor found in official docs. `jitpack.yml`'s `openjdk17` (`sdk install java 17.0.10-tem`) remains sufficient — verified by the pre-release-tag JitPack build (TOOL-08), not assumed. |

### Supporting (unchanged, verify only if a compile forces it)

| Library | Version | Purpose | When to touch |
|---|---|---|---|
| `kotlinx-datetime` | `0.6.2` (unchanged) | `api()`-exposed in picker signatures | No CMP-forced bump found for the default (non-beta-M3) path at either 1.9.3 or 1.11.1. Leave unchanged; only bump if the full test suite (TOOL-05) surfaces an incompatibility. |
| `kotlinx-coroutines-core`/`-test` | `1.10.2` (unchanged) | Coroutines, test dispatchers | No CMP-driven forcing found. Leave unchanged. |
| `compose.uiTest` (`@OptIn(ExperimentalComposeLibrary::class)`) | ships with CMP 1.11.1 | UI test runner (`runComposeUiTest`, `performMouseInput`, `captureToImage`) | Still experimental at 1.11.1 — no graduation-to-stable found. The v1 functions (`runComposeUiTest`, `runSkikoComposeUiTest`, `runDesktopComposeUiTest`) are deprecated (not removed) as of 1.11.0; see Common Pitfalls and Architecture Patterns for the dispatcher-default consequence. |

### Alternatives Considered (fallbacks — escalate, do not auto-pick)

| Instead of | Could use | Tradeoff | Trigger |
|---|---|---|---|
| CMP 1.11.1 | Kotlin 2.4.10 + CMP `1.12.0-beta02` | Genuinely matched (Kotlin-2.4-aligned CMP line) but explicitly `prerelease: true` per GitHub Releases API — trades pairing-mismatch risk for prerelease-instability risk | Fallback (a): only if 1.11.1 gate fails after known-safe fixes |
| Kotlin 2.4.10 | CMP 1.11.1 + newest Kotlin it actually accepts (likely 2.2.x/2.3.x) | Loses the locked 2.4.10 target but keeps the locked CMP version | Fallback (b) |
| CMP 1.11.1 | CMP `1.9.3` + Kotlin unchanged (2.1.21) | Fully reverts to UPGRADE.md's original conservative recommendation — zero Kotlin bump, stable M3 alias already, no test-infra dispatcher change, but loses `dropShadow`/`innerShadow` scratch-composable relevance is unaffected (lands at 1.9.0, still present at 1.9.3) | Fallback (c), last resort |

**Version verification commands** (run these, don't trust this table alone, per this project's own discipline):
```bash
# Confirm the exact stable Material3 versions currently on Maven Central (no alpha/beta/rc):
curl -s https://repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml

# Confirm CMP 1.11.1 and Kotlin 2.4.10 are both non-prerelease:
curl -s "https://api.github.com/repos/JetBrains/compose-multiplatform/releases/tags/v1.11.1" | grep prerelease
curl -s "https://api.github.com/repos/JetBrains/kotlin/releases/tags/v2.4.10" | grep prerelease
```
Both were run this session (2026-07-22) and returned the values stated above.

## Architecture Patterns

### Gated migration sequence (the load-bearing structure for this phase's plan)

This is not a flat checklist — later steps depend on earlier ones passing, and the escalation branch is a hard stop, not a soft warning.

```
1. BUILD GATE (first task, TOOL-01)
   Bump gradle/libs.versions.toml: kotlin "2.1.21"->"2.4.10", composeMultiplatform "1.7.3"->"1.11.1"
   Apply the three known-safe fixes AT THE SAME TIME (they are normal migration work,
   not separate iteration):
     a. library/build.gradle.kts:22 — replace api(compose.material3) with
        api("org.jetbrains.compose.material3:material3:1.9.0")
     b. Confirm compose-compiler plugin still version.ref = "kotlin" (already true — just verify)
     c. Gradle/JDK: leave both unchanged (already sufficient per Standard Stack) unless the
        compiler itself demands otherwise
   Run: ./gradlew :library:compileKotlin (cheapest, fastest-failing check)
   -> PASS: continue to step 2
   -> FAIL after these fixes: STOP. Escalate to user with the three named fallbacks
      (CONTEXT.md "Escalation threshold"). Report what was tried. Do not proceed further
      or pick a fallback autonomously.

2. RE-CONFIRM PRIOR SAFE-AUDITS (cheap regression re-check, not fresh investigation)
   Grep -rn "Popup(" library/src/main — re-confirm all 14 call sites still pass explicit
   PopupProperties (already verified safe past the 1.10.0 ERROR-deprecation in UPGRADE.md)

3. TEST-INFRA PORT (TOOL-03)
   Port AeroPanelGroupRecomposeUiTest for the CMP 1.11 dispatcher-default change
   (see Common Pitfalls + Code Examples for the exact mechanics). Discretion: keep v1
   runComposeUiTest (deprecated but still compiles) OR migrate to v2 — either is acceptable
   provided TOOL-03/04 hold.

4. RE-PROOF THE GUARD (TOOL-04, non-negotiable, the v2.0.3 lesson as a hard requirement)
   Temporarily revert the non-@Composable DSL fix in AeroPanelGroup's section builder ->
   run the ported test -> confirm it FAILS (header duplication observed, N per section not 1)
   -> restore the fix -> confirm it PASSES again. A ported-but-inert guard is unacceptable.

5. FULL SUITE (TOOL-05)
   ./gradlew :library:test — all 232 tests green, including the 12 PanelGroupLogicTest.
   AeroPanelGroupRecomposeUiTest's result must be manually inspected (assert exactly
   1 header per section post-drag), not just checked "passed" — "still green" alone is
   insufficient given the dispatcher-default change is exactly the failure class this
   project has already been burned by once.

6. SHOWCASE SMOKE + BASELINE DIFF (TOOL-06)
   ./gradlew :showcase:run across all three themes (AeroBlue/AeroDark/Classic).
   Compare against BEFORE-migration screenshots captured prior to step 1 (Skia
   m126->m138->m144 jump across this version range makes subtle rendering drift
   possible even with zero rendering-code changes).

7. SCRATCH COMPOSABLE (TOOL-07)
   Compile a dropShadow/innerShadow scratch composable against the real 1.11.1 jar
   (see Code Examples). Confirm or correct the signature. Commit it (not throwaway) —
   this is the Phase 15->16 handoff artifact.

8. RELEASE PROOF (TOOL-08)
   Tag a throwaway pre-release (e.g. v3.0.0-alpha01). Do NOT bump build.gradle.kts's
   version = "2.0.4" line. Verify the JitPack build succeeds on the new toolchain using
   that tag as the artifact coordinate.
```

Steps 1-2 must complete before 3; 3-4 must complete before 5 is meaningful (an unported or un-re-proven guard passing the suite is a false signal); 6 can run in parallel with 3-5 once step 1 is green; 7 has no hard dependency on 3-6 but is conventionally last since it's the Phase 16 handoff and benefits from a fully-settled toolchain; 8 is last because it's the release-proof step.

### Recommended file-touch map

```
gradle/libs.versions.toml                                            # kotlin, composeMultiplatform version bumps
library/build.gradle.kts                                             # api(compose.material3) -> explicit pin
library/src/test/kotlin/.../AeroPanelGroupRecomposeUiTest.kt          # port for dispatcher-default change
showcase/build.gradle.kts                                             # implementation(compose.material3) — see note below
jitpack.yml                                                           # verify unchanged (openjdk17 sufficient)
build.gradle.kts                                                      # UNCHANGED — version stays "2.0.4"
(new) scratch composable location — Claude's discretion per CONTEXT.md
```

**Note on `showcase/build.gradle.kts:16`:** it also uses `implementation(compose.material3)` (the same alias), currently `implementation`-scoped so it doesn't leak to library consumers the way `library/build.gradle.kts:22`'s `api()` scope does — but it will still resolve to the same alpha artifact at CMP 1.11.x for the showcase module's own compile. TOOL-02 as written names the library's pin explicitly; whether to also pin showcase's copy (for consistency and to avoid an alpha dependency anywhere in the repo, even in a non-published module) is a small planning decision — recommended: pin it identically (`implementation("org.jetbrains.compose.material3:material3:1.9.0")`) for consistency, since it costs nothing and removes an alpha artifact from the build entirely, not just from the published surface.

## Don't Hand-Roll

| Problem | Don't build | Use instead | Why |
|---|---|---|---|
| Shadow/glow primitives for the Phase 16 handoff | A custom Skia `nativeCanvas`/`MaskFilter` blur approximation to "get ahead" of Phase 16 | The real `Modifier.dropShadow`/`Modifier.innerShadow` (package `androidx.compose.ui.graphics.shadow`), confirmed available and cross-platform at 1.11.1 | This phase's entire purpose for TOOL-07 is proving the *real* API works — building a hand-rolled substitute here defeats the handoff and duplicates work Phase 16 would redo anyway |
| Screenshot regression for the before/after baseline (TOOL-06) | A pixel-diff/golden-image test framework (Roborazzi, Paparazzi, or a hand-picked image-diff library) | Manual before/after capture, human comparison | Both flagship screenshot-testing frameworks are Android-only (Robolectric/`layoutlib`-based) with zero Compose Desktop rendering path — confirmed absent in prior STACK.md research and not superseded by anything found this session. Pixel-diffing Skia output is also a documented flakiness risk across machines/GPU drivers. This phase's own CONTEXT.md explicitly names manual capture as the expected mechanism. |
| Material3 version pinning strategy | A version catalog "range" or dynamic `+`/`latest.release` resolution to auto-track future stable M3 releases | An explicit, hardcoded coordinate (`org.jetbrains.compose.material3:material3:1.9.0`) | Dynamic version ranges reintroduce exactly the alias-resolves-to-alpha risk this requirement exists to eliminate — a published library's `api`-scoped dependency must be a fixed, reviewed coordinate, not a moving target |
| Test dispatcher pumping for the ported RCMP test | A custom coroutine-dispatch shim/wrapper to force `Unconfined`-like eager execution under the new `Standard` default | Explicit `waitForIdle()`/dispatcher-idling calls already used in the existing test (the test already calls `waitForIdle()` after every `runOnUiThread { tickState.value++ }` — this pattern is dispatcher-default-agnostic and should continue to work; verify, don't route around it) | The existing test's structure (drive state, `waitForIdle()`, assert) is the standard, supported way to synchronize with a `runComposeUiTest`-driven composition regardless of which `TestDispatcher` is active underneath — inventing a parallel mechanism to force `Unconfined` behavior back would mask rather than confirm correctness across the real dispatcher change |
| JitPack JDK provisioning | A custom JDK install script beyond `jitpack.yml`'s `sdk install java 17.0.10-tem` | The existing `jitpack.yml` unchanged, verified via the actual pre-release-tag build | No official finding requires a JDK bump for Kotlin 2.4.10 on the JitPack build side; changing it preemptively is unverified extra risk for zero confirmed benefit |

**Key insight:** every "don't hand-roll" item above exists because this phase's actual deliverables are *verification artifacts* (a passing build, a re-proven test, a compiled scratch composable, a successful JitPack run) — building custom substitutes for any of them produces something that looks like progress but doesn't answer the phase's real question ("does the real toolchain pairing actually work").

## Common Pitfalls

### Pitfall 1: `AeroPanelGroupRecomposeUiTest` becomes a false-positive guard after the dispatcher-default change

**What goes wrong:** CMP 1.11.0 deprecates `runComposeUiTest`/`runSkikoComposeUiTest`/`runDesktopComposeUiTest` (still compiles, still runs) in favor of "v2" APIs, and **the v2 APIs default to `StandardTestDispatcher` instead of `UnconfinedTestDispatcher` on non-Android targets.** Confirmed directly against the raw CHANGELOG.md text this session: *"runComposeUiTest, runSkikoComposeUiTest, runDesktopComposeUiTest are deprecated in favor v2 versions"* and *"Support v2 Compose UI Tests APIs on non-android targets which uses `StandardTestDispatcher` by default instead of `UnconfinedTestDispatcher`."* `UnconfinedTestDispatcher` runs queued coroutines eagerly; `StandardTestDispatcher` queues them and requires explicit pumping. `AeroPanelGroupRecomposeUiTest` is this project's sole regression guard for the v2.0.3/v2.0.4 header-duplication bug, and it interleaves a programmatic drag with an independent recompose trigger — exactly the class of test whose correctness depends on precise coroutine-dispatch timing.
**Why it happens:** The test's existing code already calls `waitForIdle()` after every state mutation (`runOnUiThread { tickState.value++ }; waitForIdle()`), which is the dispatcher-agnostic synchronization primitive — but this doesn't automatically guarantee the *interleaving* the test wants to exercise stays the same once the underlying dispatcher default changes; a test that happened to pass under eager `Unconfined` semantics (because everything settles before the next drag move) needs to be re-checked that `waitForIdle()` still forces full settlement under `Standard` semantics before the assertions run.
**How to avoid:** Do not treat "the ported test compiles and passes" as sufficient (TOOL-04's entire point). After porting: (1) run the re-proof sequence (revert the DSL fix, confirm the test FAILS with observable header duplication, restore the fix, confirm PASS) — this is the only way to know the guard still actually exercises the bug under the new dispatcher; (2) manually inspect what the test asserts (exactly 1 header per section), not just its pass/fail status.
**Warning signs:** The ported test passes on first try with zero code changes beyond import updates — worth suspicion, not celebration, given this exact failure class. A `waitForIdle()` call that used to be sufficient suddenly needs an additional explicit dispatcher-idling call (`testScheduler.runCurrent()`/`advanceUntilIdle()` if migrating to a v2 API that exposes the underlying `TestDispatcher` directly) to force the interleaving the test depends on.

### Pitfall 2: Material3 alias silently reintroduced by a `showcase`-only or future call site

**What goes wrong:** `library/build.gradle.kts:22` gets the explicit pin, but `showcase/build.gradle.kts:16` still says `implementation(compose.material3)` — if left as the alias, the showcase module compiles against the alpha M3 artifact while the library itself is correctly pinned to stable, creating an inconsistency that could mask an M3-1.5-alpha-specific rendering quirk as if it were a toolchain issue.
**Why it happens:** TOOL-02 as literally worded targets "the library's" pin; the showcase module's own alias usage is easy to overlook since it doesn't affect published consumers.
**How to avoid:** Pin the showcase module's M3 dependency identically, even though it's `implementation`-scoped and technically out of TOOL-02's strict published-API concern — costs nothing, removes an alpha artifact from the entire build.
**Warning signs:** `./gradlew :showcase:dependencies` still shows an `-alpha` suffixed material3 artifact after the library module has been pinned.

### Pitfall 3: Escalating (or not escalating) at the wrong threshold

**What goes wrong:** Two failure modes in either direction: (a) treating the *first* red build as a gate failure and escalating immediately, without applying the three named known-safe fixes first (this wastes the user's attention on something normal migration work would have resolved); (b) continuing to attempt increasingly exotic workarounds past the point the CONTEXT.md decision defines as "genuinely won't compile," silently walking back the locked pairing without ever surfacing the three named fallbacks.
**Why it happens:** "The build failed" is a binary signal that doesn't by itself distinguish "normal migration friction" from "genuine incompatibility" — that distinction is a judgment call the CONTEXT.md decision explicitly pre-answers (known-safe fixes = Material3 pin, compose-compiler alignment, Gradle adjustment) so it doesn't have to be re-litigated live.
**How to avoid:** Apply exactly the three named known-safe fixes as part of the *first* build attempt (not sequentially probing after each individual failure) — bundle them, then run once. If that fails, stop and escalate with the three fallbacks, reporting what was tried, per CONTEXT.md's exact instruction. Do not invent a fourth "known-safe fix" beyond the three named ones without user sign-off — that would be silently expanding the escalation threshold.
**Warning signs:** More than one round of "let me try just one more thing" before reporting to the user; any workaround that touches rendering code, `GlassModifiers.kt`, or component behavior (explicitly out of scope per the Phase Boundary) in an attempt to make the build pass.

### Pitfall 4: Confusing "compiles" with "verified" for the `dropShadow`/`innerShadow` signature

**What goes wrong:** The scratch composable is written using the signature from documentation (this RESEARCH.md's Code Examples section) and it compiles — but a signature *shape* match (right function name, right shape of parameter list) doesn't guarantee every default value, parameter name (for named-argument call sites), or the exact `Shadow` constructor overload resolution is what's assumed. TOOL-07 explicitly requires confirming the signature "by compiling... not by documentation" — meaning any mismatch surfaced by the compiler is exactly the finding to record for Phase 16, not something to work around silently.
**Why it happens:** IDE autocomplete/compiler errors are precise but easy to under-read when you already have a specific signature in mind from research — a slightly-different parameter order or an extra required argument might produce a compile error that gets "fixed" by trial-and-error without documenting what the *actual* signature turned out to be.
**How to avoid:** When compiling the scratch composable, if the signature in this document doesn't match exactly, record the corrected signature verbatim (not just "made it compile") in the scratch composable's own KDoc/comments — this is the artifact Phase 16 reads, per CONTEXT.md's explicit handoff requirement ("the confirmed signature must be readable by the Phase 16 planner").
**Warning signs:** The scratch composable compiles but its comments still describe the pre-verification assumed signature rather than what was actually needed to make it compile.

## Code Examples

### The Material3 pin (`library/build.gradle.kts`)

```kotlin
// Source: this session's direct maven-metadata.xml inspection,
// repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/maven-metadata.xml
// (newest entry without alpha/beta/rc in its version string)
dependencies {
    // was: api(compose.material3)
    api("org.jetbrains.compose.material3:material3:1.9.0")
    // ... rest unchanged
}
```

Optional, recommended for consistency (`showcase/build.gradle.kts`):
```kotlin
dependencies {
    // was: implementation(compose.material3)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
}
```

### `libs.versions.toml` bump

```toml
[versions]
kotlin = "2.4.10"
composeMultiplatform = "1.11.1"
# kotlinxCoroutines, junit, kotlinxDatetime — unchanged unless TOOL-05 forces a bump

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }  # already tracks kotlin — no change needed
```

### `dropShadow`/`innerShadow` scratch composable (TOOL-07)

**Confidence on this signature: MEDIUM-HIGH.** Cross-checked this session against a fresh, unversioned third-party doc snapshot (composables.com, last-updated timestamp 2026-07-19 — three days before this research, i.e. reflecting current 1.11.1-era API), consistent with UPGRADE.md's own 1.9.0-rc01-pinned source. The package is `androidx.compose.ui.graphics.shadow` (confirmed by the existence and title of `developer.android.com/reference/kotlin/androidx/compose/ui/graphics/shadow/package-summary`, though the page's body could not be scraped this session — JS-rendered). **This is still not a hands-on compile against the real 1.11.1 jar** — treat the exact shape below as "verify at build time," per TOOL-07's own requirement, and correct this comment block in the committed scratch file if the real compiler disagrees.

```kotlin
// package androidx.compose.ui.graphics.shadow — verify at build time against the real 1.11.1 jar

fun Modifier.dropShadow(shape: Shape, shadow: Shadow): Modifier
fun Modifier.dropShadow(shape: Shape, block: DropShadowScope.() -> Unit): Modifier

fun Modifier.innerShadow(shape: Shape, shadow: Shadow): Modifier
fun Modifier.innerShadow(shape: Shape, block: InnerShadowScope.() -> Unit): Modifier

class Shadow {
    // Color-based overload
    constructor(
        radius: Dp,
        color: Color = Color.Black,
        spread: Dp = 0.dp,
        offset: DpOffset = DpOffset.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
        blendMode: BlendMode = DefaultBlendMode,
    )

    // Brush-based overload (gradient shadows)
    constructor(
        radius: Dp,
        brush: Brush,
        spread: Dp = 0.dp,
        offset: DpOffset = DpOffset.Zero,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float = 1f,
        blendMode: BlendMode = DefaultBlendMode,
    )
}
```

Modifier-ordering rule (consistent across every source checked, both this session and UPGRADE.md's): `dropShadow(...)` precedes `.background(...)` (shadow drawn behind the fill); `innerShadow(...)` follows `.background(...)` (drawn on top, recessed).

**Minimal scratch composable skeleton** (exact placement is Claude's discretion per CONTEXT.md — a dedicated file such as `showcase/src/main/kotlin/.../ScratchShadowProof.kt` or a section inside the existing showcase scratch area is acceptable; it must compile against the real 1.11.1 jar and be committed, not thrown away):

```kotlin
package com.mordred.showcase.scratch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.DpOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.shadow.dropShadow
import androidx.compose.ui.graphics.shadow.innerShadow
import androidx.compose.ui.unit.dp

/**
 * PHASE 15 -> PHASE 16 HANDOFF ARTIFACT (TOOL-07).
 *
 * Confirmed against Compose Multiplatform 1.11.1 real jar on <date>: <PASTE THE ACTUAL
 * WORKING SIGNATURE HERE IF IT DIFFERS FROM THE BLOCK BELOW — this comment is the
 * source of truth for Phase 16, not the research doc>.
 *
 * Ordering rule proven here: dropShadow precedes .background(); innerShadow follows it.
 */
@Composable
internal fun ScratchAeroShadowProof() {
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .size(120.dp, 40.dp)
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 6.dp,
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = DpOffset(0.dp, 2.dp),
                ),
            )
            .background(color = Color(0xFF3A6EA5), shape = shape)
            .innerShadow(
                shape = shape,
                shadow = Shadow(
                    radius = 2.dp,
                    color = Color.White.copy(alpha = 0.45f),
                    offset = DpOffset(0.dp, 1.dp),
                ),
            ),
    )
}
```

### Ported `AeroPanelGroupRecomposeUiTest` skeleton (TOOL-03/TOOL-04)

The existing test (`library/src/test/kotlin/com/mordred/aero/components/layout/AeroPanelGroupRecomposeUiTest.kt`) already uses the dispatcher-agnostic `waitForIdle()` synchronization pattern throughout — this is the correct pattern to keep regardless of whether the port stays on v1 `runComposeUiTest` (deprecated, still compiles at 1.11.1) or migrates to a v2 API. The structural change needed is minimal; the re-proof is the part that must not be skipped:

```kotlin
// Unchanged structure (already dispatcher-agnostic — do not replace waitForIdle() with a
// custom pumping mechanism, see Don't Hand-Roll):
@OptIn(ExperimentalTestApi::class)
class AeroPanelGroupRecomposeUiTest {
    @Test
    fun dragWithIndependentRecomposeDoesNotDuplicateHeaders() = runComposeUiTest {
        // ... existing drag + independent-recompose interleaving, unchanged ...
        repeat(10) { step ->
            root.performMouseInput { moveTo(Offset(dividerX + step * 5f, y)) }
            runOnUiThread { tickState.value++ }
            waitForIdle()   // <- verify this still fully settles composition under
                            //    StandardTestDispatcher before the next drag move;
                            //    add advanceUntilIdle()/runCurrent() only if a v2 API
                            //    migration exposes the TestDispatcher directly AND
                            //    waitForIdle() alone is proven insufficient by the re-proof step
        }
        root.performMouseInput { release() }
        waitForIdle()
        assertHeaderCounts("after drag + recompose", 1)
    }
}

// TOOL-04 re-proof sequence (run once, manually, as part of this phase — not a permanently
// committed second test variant):
// 1. Temporarily revert the non-@Composable DSL fix in AeroPanelGroup's section builder
//    (the v2.0.4 root-cause fix).
// 2. Run the ported test. EXPECT FAILURE: assertHeaderCounts should report N > 1 headers
//    per section (header duplication observed) — this proves the guard still exercises
//    the bug under the new dispatcher default.
// 3. Restore the fix.
// 4. Run the ported test again. EXPECT PASS: exactly 1 header per section.
// A guard that passes in BOTH states (broken and fixed) is inert and fails TOOL-04.
```

## State of the Art

| Old approach | Current approach | When changed | Impact on this phase |
|---|---|---|---|
| `compose.material3` alias as the version source | Explicit pinned coordinate `org.jetbrains.compose.material3:material3:1.9.0` | Alias deprecated as a version-source pattern starting CMP 1.10.0-beta01 (dependency aliases like `compose.ui` deprecated project-wide); alias itself starts resolving to alpha at CMP 1.10.0 | TOOL-02 — mandatory pin, not optional hardening |
| `runComposeUiTest`/`runSkikoComposeUiTest`/`runDesktopComposeUiTest` (v1) | "v2" Compose UI Test APIs | Deprecated (not removed) at CMP 1.11.0 | TOOL-03 — v1 still compiles and runs at 1.11.1 (Claude's discretion whether to migrate now or later), but the v1 default dispatcher behavior no longer applies once on 1.11.x regardless of which API surface is used |
| `UnconfinedTestDispatcher` default (eager coroutine execution) | `StandardTestDispatcher` default (queued, requires explicit idling) on non-Android targets | CMP 1.11.0, tied to the v2 test API rollout | The core mechanism behind Pitfall 1 — affects `AeroPanelGroupRecomposeUiTest` regardless of v1/v2 API choice, since the default dispatcher change is what actually shifted |
| Skia Milestone 126 (bundled with CMP 1.7.3) | Skia Milestone 144 (bundled with CMP 1.11.0+) | Two intermediate bumps: m126→m138 at CMP 1.10.0, m138→m144 at CMP 1.11.0 | TOOL-06 — motivates the before/after screenshot baseline; this project has zero direct `org.jetbrains.skia.*` references today (confirmed by prior STACK.md audit), so the risk is rendering-drift only, not a compile break |

**Deprecated/outdated, do not resurface:**
- UPGRADE.md's own headline recommendation to target CMP `1.9.3` — explicitly superseded by the locked 1.11.1 decision (SUMMARY.md "Superseded Findings" #2). Its supporting facts (Material3 alias table, test-infra findings, Kotlin/Gradle/JDK floors) remain valid and are reused above; its recommendation is not.
- Dependency aliases (`compose.material3`, `compose.ui`, etc.) as a forward-looking pattern for new code — deprecated since CMP 1.10.0-beta01, even though they still compile through 1.11.1.

## Open Questions

1. **Will Kotlin 2.4.10 + CMP 1.11.1 actually compile together?**
   - What we know: Individually, both floors are satisfied (Kotlin 2.4.10 exceeds CMP 1.11.x's stated 2.2 language-version floor by two full minor versions; Gradle 8.14.3 and JDK 17 are both within range for each independently).
   - What's unclear: The pair itself was never shipped matched by JetBrains — no changelog entry or release note directly states "CMP 1.11.1 is tested against Kotlin 2.4.10." This is a compatibility-by-transitivity inference, not a directly-stated fact.
   - Recommendation: This is exactly why TOOL-01 is the phase's first task and a hard gate, not an assumption — run it, don't reason about it further. If it fails, the three named fallbacks are pre-approved for escalation.

2. **Does the ported `AeroPanelGroupRecomposeUiTest` need explicit `TestDispatcher` control, or does `waitForIdle()` remain sufficient under `StandardTestDispatcher`?**
   - What we know: `waitForIdle()` is `ComposeUiTest`'s own dispatcher-agnostic idling primitive, already used throughout the existing test.
   - What's unclear: Whether `waitForIdle()` alone forces full settlement of the *specific* interleaving this test depends on (drag move → independent recompose → drag move) once the underlying dispatcher no longer runs eagerly — this can only be answered by actually running the re-proof sequence (Pitfall 1 / TOOL-04), not by reading documentation.
   - Recommendation: Treat the TOOL-04 re-proof as the authoritative answer. If `waitForIdle()` alone doesn't produce the expected FAIL-then-PASS behavior, that's the signal to add explicit dispatcher control (only if migrating to a v2 API that exposes the `TestDispatcher`) — document whichever turns out to be true in the test's own comments for future maintainers.

3. **Exact `DropShadowScope`/`InnerShadowScope` lambda-overload member APIs** (beyond the two constructor-style overloads shown in Code Examples).
   - What we know: They exist specifically "so animated shadow properties don't force a modifier-chain recomposition" (consistent phrasing across UPGRADE.md's source and this session's fresh source).
   - What's unclear: Their exact member signatures (what properties are settable inside the lambda) were not independently found this session beyond the general description.
   - Recommendation: Not needed for TOOL-07 — the Shadow-object overloads shown in Code Examples are sufficient to prove the API compiles and works. Defer the scope-lambda variant investigation to Phase 16 if/when an animated shadow property is actually needed (none of Phase 15's scope requires it).

## Validation Architecture

### Test Framework

| Property | Value |
|---|---|
| Framework | JUnit 5 (Jupiter) via `useJUnitPlatform()`, `kotlin.test` assertions, plus Compose's `compose.uiTest` (`runComposeUiTest`, `@OptIn(ExperimentalTestApi::class)`) for the one UI-level test |
| Config file | `library/build.gradle.kts` (`tasks.test { useJUnitPlatform() }`); no separate JUnit/Gradle test config file exists |
| Quick run command | `./gradlew :library:test --tests "com.mordred.aero.components.layout.AeroPanelGroupRecomposeUiTest"` (targets just the ported guard) |
| Full suite command | `./gradlew :library:test` (232 tests, incl. 12 `PanelGroupLogicTest`) |

### Phase Requirements -> Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|---|---|---|---|---|
| TOOL-01 | Project compiles on the new toolchain | build/compile | `./gradlew :library:compileKotlin` | N/A (build task, not a test file) |
| TOOL-02 | Material3 resolves to the pinned stable coordinate, not an alpha | build/dependency-tree | `./gradlew :library:dependencies --configuration compileClasspath \| grep material3` | N/A (verify via dependency report) |
| TOOL-03/TOOL-04 | RCMP guard ported and re-proven to fail on unfixed code | UI/manual-assisted | `./gradlew :library:test --tests "*AeroPanelGroupRecomposeUiTest*"` (run twice — once with DSL fix reverted, once restored, per re-proof sequence) | ✅ `library/src/test/kotlin/com/mordred/aero/components/layout/AeroPanelGroupRecomposeUiTest.kt` |
| TOOL-05 | Full suite green | unit + UI | `./gradlew :library:test` | ✅ existing 232-test suite |
| TOOL-06 | Showcase launches, three-theme visual parity | manual-only (justified: no Compose Desktop screenshot-regression framework exists) | `./gradlew :showcase:run` (manual visual comparison against pre-migration screenshots) | N/A — manual capture is the established, accepted mechanism (STACK.md, CONTEXT.md) |
| TOOL-07 | `dropShadow`/`innerShadow` compile against the real jar | build/compile | `./gradlew :showcase:compileKotlin` (or `:library:compileKotlin` depending on scratch placement) | ❌ Wave 0 — scratch composable does not exist yet, is this phase's own deliverable |
| TOOL-08 | JitPack build passes | external CI (manual trigger via tag push) | `git tag v3.0.0-alpha01 && git push origin v3.0.0-alpha01` then check JitPack build log | N/A — external service, not a local test |

### Sampling Rate
- **Per task commit:** `./gradlew :library:test --tests "*AeroPanelGroupRecomposeUiTest*"` after the port/re-proof work; `./gradlew :library:compileKotlin` after each version-bump/pin change.
- **Per wave merge:** `./gradlew :library:test` (full 232-test suite).
- **Phase gate:** Full suite green + showcase three-theme smoke + JitPack build green, before considering Phase 15 complete.

### Wave 0 Gaps
- [ ] Scratch composable file for `dropShadow`/`innerShadow` proof (TOOL-07) — does not exist yet; this phase creates and commits it (placement is Claude's discretion per CONTEXT.md).
- [ ] Before-migration baseline screenshots (three themes) — must be captured **before** step 1 of the gated sequence runs, or the "before" state is lost.
- None else — the RCMP test file, the full 232-test suite, and the JitPack config all already exist and are port/verify targets, not from-scratch builds.

## Sources

### Primary (HIGH confidence)
- `.planning/research/UPGRADE.md` — primary source for the full compatibility matrix, Material3 alias table, breaking-changes-across-1.7.3→1.11.1 survey, and initial `dropShadow`/`innerShadow` signature (Part 4, cross-checked against a 1.9.0-rc01-pinned mirror). Every version/date/compatibility claim from this doc was independently re-run this session and confirmed unchanged.
- `.planning/research/SUMMARY.md` — "Superseded Findings" #2 (why 1.11.1 over 1.9.3, three accepted consequences), Phase 15 rationale/gate/fallback block.
- GitHub Releases API, `api.github.com/repos/JetBrains/compose-multiplatform/releases` and `.../repos/JetBrains/kotlin/releases/tags/v2.4.10` — re-queried this session (2026-07-22), confirms CMP 1.11.1 (`published_at: 2026-06-02`, `prerelease: false`) and Kotlin 2.4.10 (`published_at: 2026-07-14`, `prerelease: false`); also confirms `1.12.0-beta02`/`1.12.0-beta01`/`1.12.0-alpha0x` are all `prerelease: true`.
- Maven Central `maven-metadata.xml`, `repo1.maven.org/maven2/org/jetbrains/compose/material3/material3/` and `.../compose-gradle-plugin/` — fetched directly via `curl` this session. Confirms `1.9.0` is the newest non-alpha/beta/rc `material3` version; confirms `1.11.1` exists as a published `compose-gradle-plugin` coordinate (between `1.11.0` and `1.12.0-alpha01`).
- Official `JetBrains/compose-multiplatform` `CHANGELOG.md` (raw, master branch) — fetched and analyzed this session for the 1.11.0/1.11.1 sections specifically; directly confirms (verbatim quotes reproduced in this document) the Kotlin 2.2 language-version migration note and the `runComposeUiTest` v1 deprecation + `StandardTestDispatcher`-default-on-non-Android-targets change.
- `kotlinlang.org/docs/gradle-configure-project.html` — fetched this session; confirms Kotlin Gradle Plugin 2.4.x's Gradle compatibility range (min 7.6.3, max fully-supported 9.5.0) and AGP range (not applicable, no Android target in this repo).
- `composables.com/docs/androidx.compose.ui/ui-graphics/classes/Shadow`, `.../ui/modifiers/dropShadow`, `.../ui/modifiers/innerShadow` — fetched fresh this session (unversioned = current/latest docs, page timestamp 2026-07-19, three days before this research); internally consistent with each other and with UPGRADE.md's version-pinned 1.9.0-rc01 source, strengthening confidence the signature has been stable from RC through current 1.11.1-era docs.
- This repository: direct inspection of `gradle/libs.versions.toml`, `library/build.gradle.kts`, `showcase/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`, `jitpack.yml`, `build.gradle.kts` (root), `AeroPanelGroupRecomposeUiTest.kt` — grounds every code example and file-touch claim above in the actual current file contents (this session).

### Secondary (MEDIUM confidence)
- `developer.android.com/reference/kotlin/androidx/compose/ui/graphics/shadow/package-summary` — page title confirms the package exists at this exact path; full body content was not scrapeable (JS-rendered), so this corroborates the package name but not member-level details independently of the composables.com sources above.

### Tertiary (LOW confidence, flagged for validation)
- None new this session beyond what UPGRADE.md already flagged (the August-2025 preview-build `DropShadow(...)` factory-style shape, already identified there as superseded/pre-stabilization).

## Metadata

**Confidence breakdown:**
- Standard stack (versions/coordinates): HIGH — every version number and the exact Material3 coordinate were verified this session via live API/Maven Central queries, not carried over from memory or a single prior document.
- Architecture (gated sequence): HIGH — directly derived from CONTEXT.md's own locked decisions and UPGRADE.md's already-verified risk register; no new architectural invention, just sequencing what's already locked.
- Pitfalls: HIGH for the test-infra dispatcher pitfall (directly sourced from the primary CHANGELOG.md text this session) and the Material3-pin pitfalls (grounded in this repo's actual two call sites); MEDIUM for the shadow-signature pitfall (inherently bounded by "verify at build time," which is exactly what TOOL-07 already requires).
- `dropShadow`/`innerShadow` signature: MEDIUM-HIGH, explicitly not HIGH — no hands-on compile against the real 1.11.1 jar occurred this session (no environment with the actual dependency available); this is the same residual gap UPGRADE.md flagged, narrowed by an additional, more recent, independent source but not eliminated. TOOL-07's own scratch-composable step is the mechanism that closes this gap for real.

**Research date:** 2026-07-22
**Valid until:** ~30 days for the version/coordinate facts (Maven Central and GitHub Releases data can change if new releases ship); the `dropShadow`/`innerShadow` signature confidence is bounded by "verify at build time" regardless of elapsed time — it does not improve with age, only with an actual compile.

---
*Toolchain upgrade implementation research for: aero-compose-ui v3.0 Glass Refinement, Phase 15*
*Researched: 2026-07-22*
