---
phase: 22-native-window-behavior-release-3-2-0
plan: 18
subsystem: release
tags: [rel-08, dep-01, release, jitpack, verify-tag, v3.2.0, jna]

# Dependency graph
requires:
  - phase: 22-native-window-behavior-release-3-2-0 (Plan 26)
    provides: the consolidated 22-HANDOFF.md / 22-UNCONFIRMED.md presented to the maintainer for the release decision
  - phase: 22-native-window-behavior-release-3-2-0 (Plan 17)
    provides: README/KDoc already naming 3.2.0 — the release commit's docs state
  - phase: 21-migration-release-3-1-0 (Plans 13-14)
    provides: the verify-tag-then-real-tag release protocol (v3.1.0 precedent) this plan reuses
provides:
  - "`version = \"3.2.0\"` committed as RELEASE_SHA ff19cb7eb1f64b24fbbc93cbe2752d6cbae564ae (`chore(release): 3.2.0`)"
  - "Disposable verify tag v3.2.0-verify01 green on JitPack on that exact SHA, with the published POM/.module proving JNA 5.19.1 runtime-only and a scratch consumer resolving exactly one JNA version (DEP-01 confirmed on the published artifact)"
  - "The maintainer's release decision, recorded verbatim: `hold` — no v3.2.0 tag exists locally or on origin, remote master unmoved; the release can be cut later from the same verified commit"
affects: [v3.2 milestone closeout, Pinya D-25 consumer upgrade (waits for a resolving v3.2.0), the post-verification release cut]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Verify-tag-then-real-tag release protocol (v3.1.0 precedent, reused): a never-reused disposable -verifyNN tag on the exact release commit proves the JitPack build and the published dependency metadata BEFORE any real tag exists; the real tag and any master push wait for the maintainer's explicit word (D-06)"

key-files:
  created:
    - .planning/phases/22-native-window-behavior-release-3-2-0/deferred-items.md (out-of-scope README google() finding)
    - .planning/phases/22-native-window-behavior-release-3-2-0/22-18-SUMMARY.md (this file)
  modified:
    - build.gradle.kts (version = "3.2.0", line 4 — the only change in ff19cb7)

key-decisions:
  - "Maintainer's release choice recorded as `hold`: the reply named neither release option, so the plan's hold rule applies; intent recorded — manual verification of the unpassed/unconfirmed items first, then full release if they pass, else fix-then-release"
  - "REL-08 stays Pending under hold: the requirement's literal bar (v3.2.0 tag pushed and resolving on JitPack) is not met; ROADMAP success criterion 6 is satisfied through its 'explicitly held by the maintainer' branch instead"
  - "T-22-17b stayed closed: plain v3.2.0 / 3.2.0 was never queried on JitPack and no v3.2.0 tag was ever created, so JitPack's sticky-failure cache holds nothing for the real name"

patterns-established:
  - "A held release is cut later from the already-verified commit, not from a new one: RELEASE_SHA ff19cb7 stays the release candidate until the maintainer's verification outcome"

requirements-completed: [DEP-01]  # REL-08 intentionally NOT completed — held by the maintainer; see Decisions Made

# Metrics
duration: 25min
completed: 2026-09-28
---

# Phase 22 Plan 18: REL-08 v3.2.0 Release Summary

**Version 3.2.0 commit proven on JitPack through a disposable verify tag with JNA-runtime DEP-01 checks on the published POM, then the release explicitly held by the maintainer pending manual verification — no v3.2.0 tag exists anywhere, and the release can later be cut from the same verified commit `ff19cb7`**

## Performance

- **Duration:** ~25 min across two executor sessions (Task 1 + JitPack polling in the first, checkpoint answer + hold branch in this continuation)
- **Started:** 2026-09-28T18:56:30Z
- **Completed:** 2026-09-28T19:18:10Z
- **Tasks:** 3 (2 auto + 1 checkpoint:human-action answered)
- **Files modified:** 2 tracked (1 modified, 1 created) plus this SUMMARY

## Accomplishments
- **Task 1 (REL-08, DEP-01) — the exact release commit is proven green on JitPack:**
  - Suite green before the bump: `:library:test --rerun` → `AERO_TEST_COUNT total=596 skipped=0 expected=596`; `./gradlew build` on 3.2.0 → BUILD SUCCESSFUL
  - RELEASE_SHA = `ff19cb7eb1f64b24fbbc93cbe2752d6cbae564ae` (commit `chore(release): 3.2.0`, version line only; tagged commit's build.gradle.kts carries `version = "3.2.0"` at line 4)
  - Verify tag `v3.2.0-verify01` (first — no `v3.2.0*` tags existed anywhere), tag-only push; remote master before/after: `bbe3658cba7a25643a51f5632d88f71636976346` — unmoved
  - JitPack API `/api/builds/com.github.Tolaseeq/aero-compose-ui/v3.2.0-verify01`: `"status": "ok"`, `"commit": "ff19cb7eb1f64b24fbbc93cbe2752d6cbae564ae"`, `isTag: true` (`message: "Not found"` is a known quirk of this repo — published v3.1.0 shows the same)
  - Build log: Java 21.0.2, Gradle 9.7.1, BUILD SUCCESSFUL in 50s; published aero-compose-ui-v3.2.0-verify01.pom/.module/jar/sources.jar; log saved at `build/aero-jitpack-verify-320/build.log`
  - POM: jna 5.19.1 + jna-platform 5.19.1, both `<scope>runtime</scope>`; .module: JNA only in runtimeElements (DEP-01 shape)
  - DEP-01 scratch consumer (`build/aero-jitpack-verify-320/consumer/`): exactly one `net.java.dev.jna:jna:5.19.1` and one `jna-platform:5.19.1`, no conflict arrows, BUILD SUCCESSFUL
  - Side finding out of scope, logged in `deferred-items.md`: with the README repo set (mavenCentral+jitpack) transitive androidx nodes give FAILED — they 404 on Maven Central, need `google()`; the published v3.1.0 POM has the identical structure, i.e. the README defect predates 3.2.0
- **Task 2 (checkpoint:human-action) — the maintainer's reply, recorded verbatim:**
  - «давай я попробую проверить непрошедшее и неподтверждённое руками, если выяснится, что работает - делаем фулл релиз, иначе фиксим всё, а затем фулл релиз»
  - Names neither `release-tag-only` nor `release-full` → recorded as **`hold`** per the plan's rule. Intent: the maintainer verifies the unpassed/unconfirmed items manually first; a full release follows if they work, otherwise fix-everything first and then the full release
- **Task 3 (hold branch) — nothing outward-facing, verified:**
  - No `v3.2.0` tag locally (`git tag -l 'v3.2.0'` empty) and none on origin (`git ls-remote --tags origin v3.2.0 | wc -l` = 0); it must stay that way until the maintainer's explicit release word
  - Remote master still `bbe3658cba7a25643a51f5632d88f71636976346` — unmoved; no force push, no tag deleted or moved; all earlier `v3.*` tags untouched
  - REL-08 held by maintainer; the release can later be cut from the same verified commit RELEASE_SHA `ff19cb7eb1f64b24fbbc93cbe2752d6cbae564ae` (its JitPack-green status is already proven by the verify tag)

## Task Commits

Each task was committed atomically:

1. **Task 1: Version 3.2.0, verify tag on JitPack, published POM and single-JNA check** - `ff19cb7` (chore) + `adfa7b7` (docs: deferred-items README google() finding)
2. **Task 2: Maintainer reviews the hand-off and chooses the release option** - checkpoint:human-action, answered by the maintainer; no code commit — the verbatim reply and the `hold` choice are recorded here and in STATE.md
3. **Task 3: Real tag per the maintainer's choice (hold branch)** - no tag, no push by design; recorded here and in the plan metadata commit

**Plan metadata:** this docs commit (SUMMARY + STATE.md + ROADMAP.md)

## Files Created/Modified
- `build.gradle.kts` - `version = "3.2.0"` (the only change in the release commit ff19cb7)
- `.planning/phases/22-native-window-behavior-release-3-2-0/deferred-items.md` - out-of-scope discovery from the DEP-01 consumer check (README repositories omit `google()`; pre-existing, v3.1.0 identical)
- `.planning/phases/22-native-window-behavior-release-3-2-0/22-18-SUMMARY.md` - this summary

## Decisions Made
- **`hold` recorded from a conditional reply** (Task 2): the plan's resume-signal expected one of three option words; the reply instead stated a verify-first intention. Per the plan's explicit rule ("a reply that names neither `release-tag-only` nor `release-full` ... is recorded as `hold`"), the choice is `hold` with the intent preserved verbatim — nothing published now
- **REL-08 stays Pending** (not flipped Complete): the requirement's literal bar — tag `v3.2.0` pushed, JitPack `ok`, artifact resolving — is not met under hold. ROADMAP success criterion 6 accepts "explicitly held by the maintainer" as its alternate branch, which is what this plan satisfied
- **DEP-01 recorded complete on the published artifact**: the library-side requirement was already Complete; this plan added the missing published-artifact proof (POM/.module runtime scope + single-JNA consumer resolution)
- **No early JitPack query for the real name** (T-22-17b): plain `v3.2.0`/`3.2.0` was never requested on JitPack before any real tag, keeping the sticky-failure cache empty for the eventual release

## Deviations from Plan

None - plan executed exactly as written. The `hold` outcome is the plan's own explicit Task 3 branch ("`hold` → no tag, no push; record \"REL-08 held by maintainer\"; finish"), not a deviation.

## Assumption Drift (advisory)

- **Found during:** Task 2. **Planned:** the resume-signal assumed the maintainer answers with one of the three option words. **Actual:** a conditional reply — manual verification of the unpassed/unconfirmed items first, full release intended afterwards (else fix-then-release). **Why:** the maintainer wants first-hand evidence on the FAIL/UNCONFIRMED rows from 22-HANDOFF before publishing. The plan's hold rule already covers this shape of answer; advisory only, no gate.

## Issues Encountered
- The working-copy `.planning/STATE.md` had regressed to a Plan-17-era `stopped_at`/position block (HEAD `cfaed28`/`adfa7b7` carries the newer "Gap-closure round COMPLETE" state). Restored the committed content from `adfa7b7` before applying this plan's updates, so no tracking information is lost.
- `.planning/config.json` showed a line-endings-only modification; left uncommitted deliberately (no content change).
- Untracked crash logs (`hs_err_pid*.log`, `replay_pid*.log`) and `.serena/`/`.vscode/`/`.planning/HANDOFF.json` from earlier sessions were left untouched and excluded from all commits.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- **Pending follow-up (the release gate):** the maintainer's manual verification of the unpassed/unconfirmed items (22-HANDOFF.md FAIL/UNCONFIRMED rows, 22-UNCONFIRMED.md). Outcome A — items work: cut the release from the verified commit `ff19cb7` (re-run plan 22-18 Task 3's release branch: `git tag -a v3.2.0 -m "aero-compose-ui 3.2.0" ff19cb7`, push the tag, poll JitPack to `ok` with matching commit, confirm the v3.2.0 POM resolves and the scratch consumer switches to `v3.2.0`; `release-full` additionally fast-forwards origin master after the ancestry check). Outcome B — items fail: a fix round first, then a fresh release commit + verify tag + choice
- REL-08 stays Pending until the real tag resolves; all other phase-22 requirements are booked
- The verify tag `v3.2.0-verify01` is spent (never reused, never deleted); a post-fix release would take `v3.2.0-verify02` on its own commit
- Milestone closeout (`/bm:complete-milestone`) waits for the release outcome

## Self-Check: PASSED

- Files exist on disk: `build.gradle.kts` (carries `version = "3.2.0"` at line 4), `deferred-items.md`, `22-18-SUMMARY.md` (this file)
- Task commits found in git log: `ff19cb7` (`chore(release): 3.2.0`), `adfa7b7` (`docs(22-18): log pre-existing README google() repo gap`)
- Hold assertions re-run at completion: local `git tag -l 'v3.2.0'` → empty; `git ls-remote --tags origin v3.2.0 | wc -l` → 0; `git ls-remote origin refs/heads/master` → `bbe3658cba7a25643a51f5632d88f71636976346` (unmoved); `git tag -l 'v3.2.0-verify*'` → `v3.2.0-verify01` only
- Task 3 verify command exercised through its hold branch: `grep -c "REL-08 held" .planning/phases/22-native-window-behavior-release-3-2-0/22-18-SUMMARY.md` ≥ 1 (the phrase appears in Task 3 above)

---
*Phase: 22-native-window-behavior-release-3-2-0*
*Completed: 2026-09-28*
