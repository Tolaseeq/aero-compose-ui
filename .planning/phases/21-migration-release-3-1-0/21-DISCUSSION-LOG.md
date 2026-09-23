# Phase 21: Migration + Release 3.1.0 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-09-23
**Phase:** 21-migration-release-3-1-0
**Areas discussed:** Frame storage, Drift policy, Opened states, Verify tag

Questions were asked in Russian; options are translated here.

---

## Frame storage

**Q1 — Where do before/after showcase and UI-test frames live?**

| Option | Description | Selected |
|--------|-------------|----------|
| Baseline in git (recommended) | Whole "before" baseline + only differing "after" frames in git, ~+3–4 MB; baseline reusable by the next milestone | |
| Everything in git | Both passes in full, +6–10 MB (repo 2–3× larger), pulled by every clone and JitPack forever | |
| Nothing in git | Local folder only; repo does not grow, but losing the folder loses the baseline | ✓ |

**Q2 — Exact location (text reports still go to git)?**

| Option | Description | Selected |
|--------|-------------|----------|
| Inside the project (recommended) | `C:\1A_WORK\ui_lib\.captures\`, gitignored; survives `gradlew clean`; absent in worktrees | ✓ |
| Next to the project | `C:\1A_WORK\ui_lib-captures\`, shared by all worktrees; paths point outside the project | |

**User's choice:** Nothing in git; `.captures/` inside the project, gitignored.

---

## Drift policy

**Q1 — Visible non-noise drift after the upgrade: "explain or fix" (VER-07) vs. "more than a version bump → stop" (stop rule)?**

| Option | Description | Selected |
|--------|-------------|----------|
| One stop with the list (recommended) | Finish the sweep, collect all drift, stop once with before/after pairs, cause and proposed fix per item | ✓ |
| Fix simple ones myself | Fix single-component "restore previous look" changes without asking; stop only on big ones | |
| Do not fix in this phase | Record with cause, release 3.1.0 as is, fix as a separate task | |

**Q2 — Hand-off format?**

| Option | Description | Selected |
|--------|-------------|----------|
| Local HTML page (recommended) | In `.captures`: before/after side by side per section × theme, differences highlighted, both lists; opened in the browser for the maintainer | ✓ |
| Markdown + folder | Report in `.planning` with links to frames in `.captures` | |
| Chat summary | Short summary + folder path | |

**Q3 — Differences visible only in a pixel comparison, not by eye side by side?**

| Option | Description | Selected |
|--------|-------------|----------|
| Explain only (recommended) | Record with cause, do not stop; stop list = only what is visible side by side at 100% | |
| Show everything | Any difference outside the noise regions goes on the stop list | ✓ |

**User's choice:** One stop with the full list, local HTML page, show everything.
**Notes:** Recommendation on Q3 was overridden — the maintainer prefers a longer unfiltered list.

---

## Opened states

**Q1 — How to verify opened states of the 14 popup-bearing components (no "before" baseline exists otherwise)?**

| Option | Description | Selected |
|--------|-------------|----------|
| UI tests with a baseline (recommended) | Test opens the popup and captures it in 3 themes before the upgrade, compared after; ~40 more frames/tests; per-component fallback to after-only inspection if the popup layer cannot be captured | ✓ |
| After-only inspection | Open each via MCP after the upgrade, inspect by eye, mark "inspected without baseline" | |
| Straight to "unconfirmed" | Do not check; list honestly | |

**User's choice:** UI tests with a baseline, including the pre-approved per-component fallback.

---

## Verify tag

**Q1 — May the agent push the throwaway JitPack verify tag without confirmation?**

| Option | Description | Selected |
|--------|-------------|----------|
| Push myself (recommended) | `v3.1.0-verify01`, then `-verify02`… without asking; not a release | ✓ |
| Ask every time | Stop before each tag push | |

**Q2 — What to do with verify tags after 3.1.0 ships?**

| Option | Description | Selected |
|--------|-------------|----------|
| Keep, as in v3.0 (recommended) | Tags stay on GitHub | ✓ |
| Delete | Remove v3.1 verify tags from GitHub and locally | |

**User's choice:** Push verify tags without asking; keep them.

---

## Claude's Discretion

- `.captures/` layout and naming; opt-in mechanism that keeps normal test runs from writing there.
- BASE-01 section-selection mechanism and handling of sections taller than the window.
- BASE-03 noise handling (measure vs. freeze animations).
- TOOL-16 count-guard mechanism; diff/highlight technique and grouping on the hand-off page.

## Deferred Ideas

None raised. Five keyword-matched todos (all v3.0 debt) reviewed and not folded — out of scope for v3.1.
