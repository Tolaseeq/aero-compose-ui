# Phase 7 Spike: touchSlop on Compose Multiplatform 1.7.3

**Date:** 2026-04-30
**CMP version:** 1.7.3
**Kotlin:** 2.1.21
**Issue reference:** JetBrains/compose-jb #343

## Question
Does `detectDragGestures` still suffer from the 18dp touchSlop delay on Compose Desktop in CMP 1.7.3, or has the slop been reduced enough that mouse drags register on the first or second event?

## Decision (locked regardless of outcome)
Phase 7 ships `awaitPointerEventScope` + manual loop for all in-content Canvas drag (PITFALL-03 mitigation). This is the locked v2.0 pattern — see STATE.md "v2.0 Locked Decisions". This spike is documentation only.

## Outcome
Spike skipped — `awaitPointerEventScope` manual loop is the locked v2.0 pattern regardless. Empirical re-test deferred to a future v2.x maintenance window when the upstream issue #343 has a canonical fix announcement; until then, the manual loop is the single source of truth for v2.0 drag behavior.

## Implication
`Modifier.aeroDragSplitter`, `AeroHsvColorSquare`, and `AeroHueSlider` (Plan-02) all use `awaitPointerEventScope { while(true) { awaitFirstDown(); ... } }`. `detectDragGestures` is BANNED for in-content Canvas drag.
