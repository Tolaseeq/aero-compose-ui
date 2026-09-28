---
status: partial
phase: 22-native-window-behavior-release-3-2-0
source: [22-VERIFICATION.md]
started: 2026-09-28T19:45:00Z
updated: 2026-09-28T19:45:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Maintainer's manual round over the unpassed/unconfirmed items (the recorded basis of the release hold)
expected: Caption double-click maximizes/restores on the JDK build; Alt+Space menu commands act (Move/Size/Min/Max/Close); two half-snapped windows resize together when their shared border is dragged; the pair shows a Snap Groups taskbar thumbnail; a Snap Layouts flyout zone click places the window; caption drag snaps on the JBR build
result: [pending]

### 2. WIN-04: move a window between a 100% and a real 150% monitor (drag, Win+Shift+arrow, maximize on 150%)
expected: No size jumps; caption/buttons at correct scale; hit zones match what is drawn
result: [pending]

### 3. Release cut decision after the manual round
expected: Outcome A (items work): tag v3.2.0 on ff19cb7, push, JitPack ok, artifact resolves (22-18 Task 3 release branch). Outcome B (items fail): fix round, then a fresh release commit + verify02
result: [pending]

## Summary

total: 3
passed: 0
issues: 0
pending: 3
skipped: 0
blocked: 0

## Gaps
