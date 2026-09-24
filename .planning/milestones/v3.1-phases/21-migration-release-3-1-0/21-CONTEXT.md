# Phase 21: Migration + Release 3.1.0 - Context

**Gathered:** 2026-09-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Move the whole project onto the latest stable toolchain and dependencies (Gradle 9.7.1, JDK 21,
Kotlin 2.4.20, Compose Multiplatform 1.12.0, kotlinx-coroutines 1.11.0, plain kotlinx-datetime 0.8.0,
JUnit 6.1.3; Material3 stays pinned at stable 1.9.0), prove against a pre-upgrade baseline that nothing
broke, and publish `3.1.0` on JitPack. Compose Hot Reload 1.2.0 + its MCP server is installed in
`:showcase` only, as the agent's own GUI-inspection tool — a means, not a deliverable.

Scope, order and success criteria are fixed by `.planning/ROADMAP.md` § Phase 21 and the 24
requirements in `.planning/REQUIREMENTS.md` (BASE-01..05, TOOL-09..17, HRM-01..03, VER-07..10,
REL-03..05). This discussion only settled HOW the verification artifacts are stored, how drift is
handled, how opened popup states are covered, and how the verify tag is handled.

**Not in this phase:** new components, public API changes, visual changes other than fixing drift
caused by the upgrade itself (and only per D-04), v3.0 debt, an external scratch consumer as a release
gate, 125% / 200% DPI passes (they go on the "unconfirmed" list, VER-09).

</domain>

<decisions>
## Implementation Decisions

### Carried forward (locked before this discussion — do not re-litigate)
- Execution order inside the phase is the 7-step order in ROADMAP.md § Phase 21; every version bump is
  its own commit gated by a full test run before the next.
- Global stop rule: if any upgrade or the JBR setup needs more than a version bump plus mechanical
  renames forced by the new API, or the MCP moves the real cursor / steals input focus — stop and ask.
  No workarounds.
- Window capture is `PrintWindow(hwnd, hdc, 2)` only. The MCP server's `take_screenshot` is banned
  (it is a `java.awt.Robot` screen scrape and has already captured the maintainer's private browser).
- Hover / press / keyboard focus / drag are verified by Compose UI tests only; no system input
  (Windows messages, `java.awt.Robot`) is ever delivered to a live window.
- Pixel comparison is a tool for the agent's own analysis, never an automatic pass/fail gate.
- `.planning/research/MCP-HOWTO.md` (maintainer-verified) wins over the desk research wherever they
  disagree; JBR 21 is already installed, no foojay resolver.
- The test-count guard (TOOL-16) must be proven red on a deliberately excluded test class first.

### Frame storage
- **D-01:** **No image goes into git.** Every image artifact of this phase — BASE-04 showcase baseline,
  BASE-05 UI-test reference images, the opened-state images of D-07, post-upgrade captures, diff
  images, the hand-off page of D-06 — lives in **`.captures/` at the repo root**
  (`C:\1A_WORK\ui_lib\.captures\`), which is added to `.gitignore`. It is outside `build/`, so
  `gradlew clean` does not wipe it (satisfies BASE-04 / BASE-05 "outside `build/`"). Consequence the
  maintainer accepted: the folder does not exist in git worktrees, and if it is deleted the baseline
  is gone for good — the next milestone can reuse it only if the folder survives.
- **D-02:** **What IS committed:** the capture helper (BASE-02), the launch-parameter code (BASE-01),
  all UI-test code (BASE-05 + D-07), and the **text** of every report — noise-region list (BASE-03),
  explained-differences list, unconfirmed list (VER-09), the toolchain each capture run was taken on —
  in the phase directory. Text reports reference frames by their `.captures/`-relative path.
- **D-03:** BASE-05 / D-07 UI tests are permanent members of the suite (they count toward the TOOL-16
  number), so an ordinary `gradlew test` run must not write into `.captures/` or dirty the working
  tree. Writing reference / after images into `.captures/` happens only on an explicit opt-in (e.g. a
  Gradle or system property). The mechanism is Claude's discretion.

### Drift found after the upgrade
- **D-04:** **One stop, with the complete list.** The agent finishes the entire post-upgrade sweep
  first — every showcase section × 3 themes (VER-07), every BASE-05 state (VER-08), every opened-state
  image (D-07) — and only then stops **once**, presenting every difference outside the BASE-03 noise
  regions: before/after pair, named cause, and a proposed action per item (fix in code / accept as an
  upstream rendering change). The agent does **not** change drawing/rendering code on its own to
  compensate for drift — a drift fix is "more than a version bump" under the stop rule. Fixes land
  only after the maintainer rules on the list; affected frames are then re-captured and re-compared.
- **D-05:** **Threshold = everything outside the named noise regions.** Differences visible only in a
  pixel comparison (e.g. text antialiasing a few shades off across the page after a Skia bump) are NOT
  filtered out — they go on the stop list too. Grouping several frames under one shared cause is
  allowed to keep the list readable, but no difference may be dropped or silently reclassified as
  noise. The noise list is established on the OLD toolchain (BASE-03) before any bump; any region
  added to it after the upgrade must be shown to vary run-to-run on repeated capture of the same state
  on the new toolchain, and is itself called out on the stop list.
- **D-06:** **Hand-off format = a local HTML page** in `.captures/` (not committed): before/after side
  by side per section × theme and per UI-test state, differences highlighted, and both lists
  (explained differences, unconfirmed) at the bottom. It must work offline from the local folder
  (images by relative path). The agent opens it for the maintainer with
  `start "" "C:\1A_WORK\ui_lib\.captures\...\index.html"`. The same page format is used for the D-04
  drift stop. The list text is also committed (D-02). VER-10 still holds: the page reaches the
  maintainer only after the agent's own sweep is complete.

### Opened popup states
- **D-07:** **Opened states are covered by UI tests with a before-baseline**, the same mechanism as
  BASE-05 (`runComposeUiTest` + click / key input → `captureToImage`), in all three themes, captured
  on the old toolchain (Kotlin 2.4.10 / CMP 1.11.1) before any bump and compared after it. Coverage:
  the components whose sources contain `Popup(` — `AeroDropdown` (via `AeroDropdownPopup`),
  `AeroComboBox`, `AeroContextMenu`, `AeroMenuBar`, `AeroTooltip`, `AeroPopover`, `AeroDrawer`,
  `AeroColorPickerButton`, `AeroDatePicker`, `AeroDateRangePicker`, `AeroDateTimePicker`,
  `AeroDateTimeRangePicker`, `AeroTimePicker`. `AeroDialog` / `AeroAlertDialog` (built on `Dialog`)
  follow the same rule where the test can capture them. These tests are added before the TOOL-16
  count is locked, so they are part of the locked number.
- **D-08:** **Pre-approved per-component fallback:** if a UI test cannot capture a particular
  component's popup layer (e.g. it renders as a separate root or window that `captureToImage` does not
  reach), then for THAT component only the agent switches to after-only inspection — open it via MCP
  `click`, capture with `PrintWindow` — and marks it "inspected without baseline" in the report. This
  is not a stop. The BASE-05 stop still applies if `captureToImage` does not work in desktop
  `runComposeUiTest` at all. `AeroFilePicker` opens the native OS dialog, which is not this library's
  rendering; it goes on the unconfirmed list.
- **D-09:** **Open-state frames must be deterministic across days:** pickers must be given a fixed
  `value`. With `value == null` they open on the current month via a private `todayLocalDate()`
  (`Clock.System.now()` — `AeroDatePicker.kt:167`, `AeroDateRangePicker.kt:263`,
  `AeroDateTimePicker.kt:196`, `AeroDateTimeRangePicker.kt:333`), so before and after frames taken on
  different days would differ. `AeroCalendarGrid` has no today-highlight (checked), so a fixed value is
  sufficient.

### JitPack verify tag
- **D-10:** **The agent pushes throwaway verify tags without asking:** `v3.1.0-verify01`, then
  `-verify02`, … after each fix (a failed JitPack build sticks to its tag forever, so a name is never
  reused). This standing authorization covers only pushing verify tags (which carries the commits they
  point to); moving `master` on the remote, setting `3.1.0` in `build.gradle.kts`, and pushing
  `v3.1.0` stay behind the maintainer's confirmation at execution time (ROADMAP success criterion 5).
- **D-11:** **Verify tags stay on GitHub** after `3.1.0` ships — same as v3.0 (`v3.0.0-alpha01`,
  `-verify01`, `-verify02` are still there). Nothing is deleted.

### Claude's Discretion
- Internal layout and file naming of `.captures/` — must record the toolchain (old/new), theme, and
  section or component/state of each frame.
- BASE-01 mechanism: e.g. `-Paero.section=<Name>` forwarded like the existing `-Paero.scheme`
  (`showcase/build.gradle.kts` `tasks.withType<JavaExec>` block + `initialScheme()` in `Main.kt`), and
  a sibling forwarding block for Hot Reload's non-`JavaExec` run task. Whether it renders only the
  chosen section or scrolls to it is open, as long as the section is fully visible and the before and
  after frames are produced identically. Sections taller than the window may use a taller window or
  several frames — identically before and after.
- BASE-03 noise handling (measure and list vs. a capture mode that freezes infinite animations). Known
  sources: `AeroProgressBar`'s two `rememberInfiniteTransition`s (`runningSheen`, `indeterminate`), the
  text caret, and anything time-dependent.
- TOOL-16 count-guard mechanism.
- Diff/highlight technique on the D-06 page and how differences are grouped by cause (within D-05).
- Showcase `testTag`s added for HRM-02.

### Folded Todos
None — the 5 matching todos are v3.0 debt, explicitly out of scope for v3.1 (see Deferred).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Scope, order and acceptance
- `.planning/ROADMAP.md` § "Phase 21: Migration + Release 3.1.0" — 7-step execution order, global stop
  rule, 5 success criteria, human-action point (Claude Code restart after `.mcp.json`).
- `.planning/REQUIREMENTS.md` — BASE-01..05, TOOL-09..17, HRM-01..03, VER-07..10, REL-03..05, Out of
  Scope table, Future Requirements (VER-F01..F03).
- `.planning/PROJECT.md` § "Current Milestone: v3.1" — goal, key decisions (minor `3.1.0` despite a
  consumer-breaking floor, Java 21 everywhere, stop rule).
- `.planning/STATE.md` § "Locked decisions carried forward" (esp. the `[v3.1]` entries) and
  § "Blockers/Concerns".

### Hot Reload + MCP (wins on conflict)
- `.planning/research/MCP-HOWTO.md` — maintainer-verified recipe: CMP ≥ 1.12.0, plugin 1.2.0 pinned,
  `.mcp.json` via `cmd /c .\gradlew.bat`, `hotRun --mainClass=com.mordred.showcase.MainKt`,
  `PrintWindow`, `ShowWindow(h, 4)`, `SetWindowPos(h, HWND_BOTTOM, …, 0x13)`, `gradlew reload` over the
  `reload` tool, node ids change after recomposition.

### Upgrade research
- `.planning/research/SUMMARY.md` — five flagged conflicts (kotlinx-datetime break vs. no-op, JBR 21 vs.
  25, Hot Reload task names for plain `kotlin.jvm`, Gradle 9.7.1 vs. Kotlin 2.4.20's 9.7.0 ceiling,
  `take_screenshot` mechanism) with the cheapest empirical check for each; MCP tool-surface limits.
- `.planning/research/STACK.md`, `.planning/research/PITFALLS.md`, `.planning/research/ARCHITECTURE.md`,
  `.planning/research/FEATURES.md` — detail behind SUMMARY.md (Material3 alpha leak via Hot Reload
  devtools, CMP 1.12 `uiTest` dispatcher default, stdio noise on MCP, JitPack sticky failed builds).

### Precedent (v3.0)
- `.planning/milestones/v3.0-phases/15-toolchain-upgrade/15-CONTEXT.md` — previous toolchain migration:
  known-safe fixes vs. escalation, throwaway pre-release tag instead of a version bump.
- `.planning/milestones/v3.0-phases/15-toolchain-upgrade/baseline/README.md` and
  `.planning/milestones/v3.0-phases/15-toolchain-upgrade/after/DIFF-REPORT.md` — previous before/after
  capture and diff-report format (old `CopyFromScreen` method — superseded by `PrintWindow`).
- `.planning/milestones/v3.0-phases/20-verification/20-CONTEXT.md` D-14 — JitPack throwaway-tag
  mechanism.

### Files this phase touches
- `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, `settings.gradle.kts`
  (stale `maven.pkg.jetbrains.space` repo, TOOL-09), `build.gradle.kts` (version, REL-05),
  `library/build.gradle.kts` (`jvmToolchain(17)`, Material3 1.9.0 pin, JUnit launcher),
  `showcase/build.gradle.kts` (`jvmToolchain(17)`, `-Paero.scheme` forwarding, Hot Reload plugin),
  `jitpack.yml` (`openjdk17` → 21), `README.md:6` (stale "Kotlin 2.1.21 · Compose 1.7.3 · JVM 17" line),
  `.gitignore` (add `.captures/`), new `.mcp.json`.
- `showcase/src/main/kotlin/com/mordred/showcase/Main.kt` (`initialScheme()`),
  `showcase/src/main/kotlin/com/mordred/showcase/ShowcaseApp.kt` (single `verticalScroll` column of 16
  sections).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `captureToImage()` already works in desktop `runComposeUiTest` on the current toolchain — used in
  `library/src/test/kotlin/com/mordred/aero/verification/VER07ButtonStateMatrixTest.kt`,
  `library/src/test/kotlin/com/mordred/aero/components/range/AeroRangeSliderHoverCancellationTest.kt`
  (hover via `performMouseInput`) and
  `library/src/test/kotlin/com/mordred/aero/theme/AeroThemeBackgroundEstablishmentTest.kt`
  (`onRoot().captureToImage()`). BASE-05's first-step proof is therefore very likely to pass.
- `internal fun pixelMapsDiffer(a: PixelMap, b: PixelMap)` in `VER07ButtonStateMatrixTest.kt:265`, with
  its own fixture proofs — reusable for comparing UI-test frames.
- `AeroPanelGroupRecomposeUiTest.kt` — deterministic programmatic drag pattern (for `AeroSplitPane`,
  `AeroPanelGroup`, `AeroDataTable` column resize in BASE-05).
- `VER08SegmentLabelFlipTest.kt` — `mainClock.autoAdvance = false` + `advanceTimeByFrame()` pattern for
  stepping animations deterministically (relevant to noise and to the CMP 1.12 dispatcher change).
- `-Paero.scheme` → `System.getProperty("aero.scheme")` → `initialScheme()` — the exact pattern to
  extend for BASE-01's section parameter.

### Established Patterns
- Single version catalog: `kotlin.jvm` and `compose-compiler` plugins both `version.ref = "kotlin"`, so
  one line moves both.
- Material3 is pinned by explicit coordinate `org.jetbrains.compose.material3:material3:1.9.0` in both
  modules; the `compose.material3` alias resolves to alpha and must not be used.
- `api()`-scoped `kotlinx-datetime` and Compose — the new floor becomes a transitive floor for every
  JitPack consumer (hence REL-04).

### Integration Points
- kotlinx-datetime 0.8.0 renames (`dayOfMonth` / `monthNumber`, `Clock`) touch more than the four
  library pickers the research counted: also `AeroCalendarGrid.kt:71,150`,
  `showcase/.../sections/DataSection.kt:82`, `showcase/.../sections/PickersSection.kt:64,112,146,164`,
  and `library/src/test/.../AeroCalendarGridTest.kt:38`. The compiler decides (TOOL-14).
- Popup call sites: 14 files under `library/src/main` contain `Popup(` (D-07 list); dialogs:
  `AeroDialog.kt`, `AeroAlertDialog.kt`, `AeroFilePicker.kt`.
- Showcase sections (ShowcaseApp.kt order): ThemeSwitcher, Verification, Foundation, Primitives, Icons,
  Buttons, Input, Selection, Dropdown, Range, List, Containers, Overlays, Navigation, Data, Pickers,
  Layout.

</code_context>

<specifics>
## Specific Ideas

- The maintainer wants to be interrupted once, not per finding: the D-04 stop comes after the full
  sweep, in the D-06 page format.
- "Show everything" was chosen over "explain pixel-only changes silently" — the maintainer prefers a
  longer list to a filtered one. Grouping by cause is the way to keep it readable, never omission.
- Verify-tag naming follows v3.0: `vX.Y.Z-verifyNN`.

</specifics>

<deferred>
## Deferred Ideas

None raised in discussion — it stayed within phase scope.

### Reviewed Todos (not folded)
Matched by keyword, all v3.0 debt — out of scope for v3.1 per REQUIREMENTS.md Out of Scope
("Долги v3.0 — отдельная веха"):
- `2026-07-28-track-aerobutton-label-contrast-below-wcag-4-5-1-floor` — AeroButton label contrast below WCAG 4.5:1.
- `2026-07-29-aeroblue-aerodark-opaque-fill-label-below-wcag-floor-white-decision` — opaque-fill label contrast (white, maintainer-approved).
- `2026-07-29-aeroradiobutton-hover-shadow-square-not-round` — AeroRadioButton hover shadow is square.
- `2026-07-29-aerotheme-establishbackground-abi-compatibility` — `establishBackground` binary compatibility for the JitPack artifact.
- `2026-07-29-contrast-regression-test-omits-outlined-and-segment-disabled-fills` — contrast test coverage gap.

</deferred>

---

*Phase: 21-migration-release-3-1-0*
*Context gathered: 2026-09-23*
