# Phase 15: Toolchain Upgrade - Context

**Gathered:** 2026-07-22
**Status:** Ready for planning

<domain>
## Phase Boundary

The library builds, tests, and ships on **Kotlin 2.4.10 + Compose Multiplatform 1.11.1** (or an explicitly user-approved fallback), with **zero visual-code changes** mixed in — so any later regression is unambiguously attributable to either the toolchain or the visual work, never both.

Scope is the mandatory dependency/build-graph migration only: version bumps, the explicit stable Material3 pin, the `AeroPanelGroupRecomposeUiTest` port + re-proof, a full green test suite, a three-theme showcase smoke pass, a `dropShadow`/`innerShadow` scratch-composable proof against the real jar, and a JitPack build on the new toolchain.

**Explicitly NOT in this phase:** any change to rendering/drawing code, `GlassModifiers.kt` bug fixes (that is Phase 16), any of the eight component restyles, any new visual capability. If a visual change is tempting "while we're in here" — it belongs in Phase 16+.

</domain>

<decisions>
## Implementation Decisions

### Target pairing (carried forward — locked, do not re-litigate)
- Target **CMP 1.11.1 + Kotlin 2.4.10**, chosen consciously over the research's own conservative recommendation of CMP 1.9.3 (see `SUMMARY.md` "Superseded Findings" #2 — the user accepted all three consequences: Kotlin 2.2+ floor, explicit stable-M3 pin, and the `AeroPanelGroupRecomposeUiTest` port + re-proof).
- The `./gradlew build` on this pairing is the **first task of the phase** — the pair was never shipped matched by JetBrains (CMP 1.11.1 published ~6 weeks before Kotlin 2.4.10; the Kotlin-2.4-aligned CMP line is 1.12.x, prerelease). Outcome is genuinely unverified until run.
- Three named fallbacks, in preference order, only if the gate fails: **(a)** Kotlin 2.4.10 + CMP `1.12.0-beta02` (matched but prerelease); **(b)** CMP 1.11.1 + newest Kotlin it actually accepts; **(c)** CMP `1.9.3` + Kotlin unchanged. **Any fallback is escalated to the user, never substituted silently** — it walks back an explicit locked decision.

### Escalation threshold (when the build gate fails)
- **Apply known-safe fixes first, then escalate only if the pairing genuinely won't compile after them.** "Gate failed → escalate" means the pair does not compile *after* the expected mechanical migration work is done — not the first red build.
- Known-safe fixes that count as normal migration work (NOT a gate failure): the explicit Material3 pin (TOOL-02 requires it anyway), aligning the `compose-compiler` (`org.jetbrains.kotlin.plugin.compose`) plugin version to the new `kotlin` catalog entry, and confirming/adjusting Gradle if a compile demands it.
- If, after those, the Kotlin 2.4.10 + CMP 1.11.1 pair still does not compile → **stop and escalate to the user with the three named fallbacks**, reporting what was tried. Do not pick a fallback autonomously.

### Scratch composable (TOOL-07)
- **Commit the `dropShadow`/`innerShadow` scratch composable as a seed/reference for Phase 16**, not throwaway. It carries the signature *confirmed (or corrected) against the real 1.11.1 jar*, so Phase 16's Aero-primitives work consumes a proven basis instead of re-verifying from documentation.
- This is the deliberate handoff artifact across the Phase 15→16 boundary. Placement (showcase scratch section vs a dedicated file) is Claude's discretion, but it must compile against the real artifact and the confirmed signature must be readable by the Phase 16 planner.

### Release & JitPack (TOOL-08)
- **Cut a throwaway pre-release git tag now** (e.g. `v3.0.0-alpha01`) to verify the JitPack build on the new toolchain immediately within Phase 15.
- **Do NOT bump the real version** in `build.gradle.kts` — it stays `2.0.4` (root `build.gradle.kts:4`) until `/gsd:complete-milestone`, per the locked bump-on-milestone rule. JitPack derives the artifact coordinate from the git tag (`com.github.Tolaseeq:aero-compose-ui:v3.0.0-alpha01`), so the pre-release tag proves the build without touching the Gradle version line.

### Baseline capture for "no visual change" (TOOL-06)
- **Capture before/after screenshots** of the showcase on all three themes (AeroBlue / AeroDark / Classic): snapshot BEFORE the migration, compare AFTER. A rigorous diff, not an eyes-on-from-memory pass.
- Rationale: Phase 15 touches zero rendering code, but Skia jumps **m126 → m138 → m144** across 1.7.3→1.11.1 (two milestone bumps), so subtle toolchain-induced rendering drift is possible and must be actively looked for. This also directly answers the project's own false-positive-sign-off history (`feedback_repro_must_exercise_path`).

### Claude's Discretion
- Exact placement/structure of the committed scratch composable (as long as it compiles against the real jar and exposes the confirmed signature to Phase 16).
- Whether to port `AeroPanelGroupRecomposeUiTest` to the CMP 1.11 "v2" `runComposeUiTest` API vs keeping the deprecated-but-working v1 — provided TOOL-03/TOOL-04 are satisfied (test is green AND re-proven to FAIL on reverted non-`@Composable` DSL, accounting for the `Unconfined`→`Standard` `TestDispatcher` default change).
- Screenshot tooling/mechanism for the before/after baseline (no Compose Desktop screenshot-regression framework exists — manual capture is expected).
- Single-shot bump (1.7.3 → 1.11.1 directly) vs stepping — research found no repo-relevant breaking changes in the 1.8.x/1.9.x/1.10.x lines; stepping buys no safety here.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents (researcher, planner) MUST read these before planning or implementing.**

### Toolchain migration facts & sequencing (primary)
- `.planning/research/UPGRADE.md` — the load-bearing doc: verified version/date/compatibility matrix (Kotlin/Gradle/JDK floors), the `compose.material3` alias→alpha resolution table, the CMP 1.11.0 test-infra dispatcher-default change, exact `dropShadow`/`innerShadow`/`Shadow` signatures (Part 4, MEDIUM-HIGH confidence — must be re-verified against the real jar), and the full ranked risk register + gated migration sequence (Part 5). Note: its top-line *recommendation* of 1.9.3 is SUPERSEDED by the locked 1.11.1 target — read it for the facts and the 1.11.x-specific consequences, not the recommendation.
- `.planning/research/SUMMARY.md` — "Superseded Findings" #2 (why 1.11.1 over 1.9.3, consequences accepted) and the Phase 15 rationale/gate/fallback block under "Implications for Roadmap".

### Requirements & success criteria
- `.planning/REQUIREMENTS.md` — **TOOL-01..TOOL-08** (the eight requirements this phase closes), lines ~14-25.
- `.planning/ROADMAP.md` §"Phase 15: Toolchain Upgrade" — the five explicit Success Criteria (build gate + escalation, stable-M3 pin, RCMP test port + re-proof, 232-test suite + showcase smoke, scratch-composable + JitPack).

### Locked positions & institutional memory
- `.planning/STATE.md` §"Toolchain Upgrade — MANDATORY (locked 2026-07-21)" and §"Blockers/Concerns" (pairing genuinely unverified; no external consumer app tracks v3.0 this milestone).
- `.planning/research/STACK.md` — origin of the 1.9.0 `dropShadow`/`innerShadow` finding and the zero-direct-`org.jetbrains.skia.*`-references audit of `GlassModifiers.kt` (relevant to the Skia-bump risk assessment).
- `.planning/research/PITFALLS.md` — Pitfall-to-Phase mapping; the test-infrastructure/dispatcher pitfall is the Phase-15-relevant one.

### Files this phase modifies (verified this session)
- `gradle/libs.versions.toml` — `kotlin = "2.1.21"` → `"2.4.10"`; `composeMultiplatform = "1.7.3"` → `"1.11.1"` (`compose-compiler` plugin version-refs `kotlin`, so it auto-tracks).
- `library/build.gradle.kts:22` — replace `api(compose.material3)` with an explicit pinned stable coordinate (TOOL-02).
- `library/src/test/kotlin/com/mordred/aero/components/layout/AeroPanelGroupRecomposeUiTest.kt` — the sole RCMP regression guard to port + re-prove (TOOL-03/04).
- `jitpack.yml` — pins `openjdk17`; research says no change needed (JDK 17 sufficient for Kotlin 2.4.10), verify via the pre-release-tag JitPack build.
- `build.gradle.kts:4` — `version = "2.0.4"`; **left unchanged this phase** (per Release decision above).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **Prior verified audits** (in UPGRADE.md, re-usable as cheap regression re-checks, not fresh investigation): all 14 `Popup(` call sites already pass explicit `PopupProperties` (safe past the 1.10.0 ERROR-deprecation); the only M3 usage is 3 trivial call sites (`ButtonDefaults.buttonColors`, `ButtonDefaults.outlinedButtonColors`, `SliderDefaults.colors`) with no removals found; zero `org.jetbrains.skia.*` / `rememberRipple` / `LocalMinimumInteractiveComponent*` references.
- **`AeroPanelGroupRecomposeUiTest.kt`** — already exists and is the deterministic programmatic-drag guard; the port target, not a from-scratch write.

### Established Patterns
- **Enabling-phase-then-consume precedent** (v2.0 Phase 7): building a proven artifact in one phase for later phases to consume — directly mirrored by the "commit scratch composable as seed for Phase 16" decision.
- **Isolate one class of change per phase** (v2.0.x patch history, Phase 13.1 orientation-param) — the whole justification for Phase 15 existing separately from visual work.
- **`libs.versions.toml` single-source versioning** — `kotlin.jvm` and `compose-compiler` plugins both `version.ref = "kotlin"`, so one catalog line moves both.
- **`api()`-scoped public deps** (`compose.material3`, `kotlinx-datetime`) — any Kotlin/Compose/M3 floor this library adopts becomes a transitive floor on every JitPack consumer; call it out in release notes.

### Integration Points
- **Phase 15 → Phase 16 handoff:** the committed scratch composable + confirmed `dropShadow`/`innerShadow` signature is the concrete artifact Phase 16's `AeroSurfacePrimitives` work builds on.
- **JitPack:** git tag → artifact coordinate; `jitpack.yml` `openjdk17` build side.
- **Verification is manual:** no Compose Desktop screenshot-regression framework exists — before/after baseline is hand-captured; the RCMP test result must be human-inspected (assert exactly 1 header/section post-drag), not just checked "passed".

</code_context>

<specifics>
## Specific Ideas

- Escalation must report *what was tried* (the known-safe fixes) alongside the three fallbacks — so the user's fallback choice is informed, not blind.
- The RCMP guard re-proof is non-negotiable and specific: temporarily revert the non-`@Composable` DSL fix, observe header duplication, restore the fix — a ported-but-inert guard is explicitly unacceptable (TOOL-04, the v2.0.3 lesson encoded as a requirement).
- "Still green" is not sufficient for `AeroPanelGroupRecomposeUiTest` — a human glance at what it asserts is required, because the `Unconfined`→`Standard` dispatcher-default change at CMP 1.11 is exactly the timing-class that produced this project's prior false-positive sign-off.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope. (All visual/rendering work, including `GlassModifiers.kt` fixes, is Phase 16+ by explicit milestone design.)

</deferred>

---

*Phase: 15-toolchain-upgrade*
*Context gathered: 2026-07-22*
