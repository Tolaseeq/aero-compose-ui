# Phase 11 Deferred Items

## Pre-existing Issues (out of scope for 11-02)

### LayoutSection.kt compile errors (untracked file)

**Discovered during:** Plan 11-02, Task 2 compile verification
**File:** `showcase/src/main/kotlin/com/mordred/showcase/sections/LayoutSection.kt`
**Status:** Untracked (never committed). Has compile errors:
- `Cannot access 'val RowColumnParentData?.weight: Float': it is internal in file.`
- `Unresolved reference 'button'.`
- `Unresolved reference 'AeroButton'.`
- `@Composable invocations can only happen from the context of a @Composable function`

**Note:** These errors existed before plan 11-02 execution. DataSection.kt has no compile errors.
Plan 11-03 (LayoutSection) should fix these as part of its scope.

**DataSection.kt status:** No compile errors. The `:showcase:compileKotlin` failure is entirely from LayoutSection.kt.
