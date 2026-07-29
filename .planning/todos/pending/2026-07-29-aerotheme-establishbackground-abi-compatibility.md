---
created: 2026-07-29T00:00:00Z
type: finding
title: AeroTheme's establishBackground parameter is source- but not verified binary-compatible for the JitPack-published artifact
area: build
severity: warning
files:
  - library/src/main/kotlin/com/mordred/aero/theme/AeroTheme.kt:82-88
---

## Problem

Filed while closing VER-05 (`20-06-SUMMARY.md`), from the review addendum finding **WR-03** in
`20-REVIEW.md` (addendum covering commits `1d139a7`/`ff577fc`).

`AeroTheme`'s new `establishBackground: Boolean = true` parameter, inserted between `typography`
and the trailing `content` lambda, was added to fix VER-05's naive-consumer light-background
defect. `library/build.gradle.kts` applies `maven-publish` and the module is distributed via
JitPack by git tag — this is a genuinely externally-consumed published artifact, not an
internal-only module.

Inserting the new parameter is **source-compatible by construction**: Kotlin resolves the
trailing-lambda `content` positionally against the *last* parameter regardless of how many
defaulted parameters precede it, so every existing `AeroTheme { ... }` /
`AeroTheme(colorScheme = ...) { ... }` call site — recompiled against the new source — keeps
compiling with zero edits (verified: this repository's only call-site shapes, via
`grep -rn "AeroTheme(" --include="*.kt"`, are the one production call site in
`showcase/.../Main.kt` plus ~12 files using the bare `AeroTheme { ... }` form, all still
compile clean).

What was **not** established, and the fix commit did not claim to check, is **binary**
compatibility. Kotlin compiles default-parameter functions to a synthetic bridge method carrying
a bitmask over the parameter list; adding a parameter changes that bitmask's shape and the
primary method's descriptor. A consumer holding an **already-compiled** artifact built against a
previous published version of this library (rather than one recompiled from source against the
new version) could fail to link against a new publish of this JAR with a `NoSuchMethodError`-class
failure, if it called `AeroTheme` positionally/by name in a way that resolved to the old
descriptor.

No `@JvmOverloads` is used anywhere in `AeroTheme.kt` or, per a full-library grep, anywhere else
in `library/src/main`. No binary-compatibility validator (e.g.
`kotlinx-binary-compatibility-validator`) is configured in either `build.gradle.kts` — this is a
systemic gap in the library's public-API-evolution discipline that this commit inherits rather
than introduces, but it is the first commit in the v3.0 milestone to add a *parameter* (rather
than a data-class field, as `AeroColorScheme.ornamentOverride` did — a structurally different and
lower-risk kind of addition) to an existing public `@Composable` signature, so it is the first
place this gap becomes concretely exercisable.

## Solution

Either of two dispositions closes this — leaving it silently undecided is the actual gap, not
picking one of them:

1. **Add `@JvmOverloads` to `AeroTheme`.** The compiler then emits the pre-existing 3-parameter
   overload as a real, separately-callable JVM method alongside the new 4-parameter one, restoring
   binary compatibility for old callers with zero source change required elsewhere.
2. **Explicitly accept and document** that this library's published-artifact compatibility
   guarantee is source-level only (recompile-to-upgrade) — consistent with its current practice
   everywhere else in the library (no `@JvmOverloads`, no ABI validator anywhere) — so this is not
   treated as a regression unique to this diff, just an existing, now-visible policy.

If (2) is chosen, consider also adopting a binary-compatibility validator plugin so future
parameter additions to other public composables surface the same tradeoff mechanically instead of
requiring a manual review catch each time.

Related, but separately filed: **IN-03** (the `establishBackground = true` default silently
changes rendered output for a hypothetical consumer wanting a transparent `AeroTheme {}` slot; no
`CHANGELOG.md` exists to record this as a behavior change) and **IN-04** (a comment-precision nit
in the new test's KDoc) are recorded in `20-REVIEW.md`'s addendum only — neither needs a separate
todo.
