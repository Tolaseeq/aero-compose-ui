---
phase: 06-showcase-iconssection
verified: 2026-04-29
verdict: pending
checkpoint_type: three-theme-visual
v1_1_milestone_signoff: pending
---

# Phase 6 — Verification

## Pre-flight (automated)

- [x] IconsSection.kt grep suite: PASS (138 icon entries confirmed via `grep -c 'IconEntry("'`; 139 count from plan grep includes the `data class IconEntry(` definition line — actual icon entries = 138. All other patterns hit: `GridCells.Adaptive(80.dp)`, `.height(400.dp)`, `Modifier.size(24.dp)`, `tint = colors.onSurface`, `AeroSearchField(`, `Search icons`, `ignoreCase = true`, `No icons match`, `.glassSurface(cornerRadius = 6.dp)`, `rememberCoroutineScope`, `LocalClipboardManager`)
- [x] ShowcaseApp.kt wiring grep: PASS (`import com.mordred.showcase.sections.IconsSection` at line 27; `IconsSection(toastState = toastState)` at line 82; awk ordering check prints `OK`)
- [x] ButtonsSection.kt SHW-06 grep: PASS (0 `Text("▲/▼/×/✕")` hits in any `.kt` source; `Icon(AeroIcons.CaretUp/CaretDown/X)` = 3 hits; `Text("B/I/U/S")` = 4 hits — locked exclusion confirmed)
- [x] `./gradlew :showcase:compileKotlin`: PASS (exits 0 with `JAVA_HOME` pointed at Gradle-cached JDK 17 at `~/.gradle/jdks/eclipse_adoptium-17-amd64-windows.2`)
- [x] `./gradlew :library:test`: PASS (exits 0; 8 tasks up-to-date)

## Three-theme visual checkpoint

(Filled by Task 2.)
