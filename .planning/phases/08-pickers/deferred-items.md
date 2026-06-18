# Phase 08 Deferred Items

Out-of-scope issues discovered during execution. Logged, NOT fixed by the discovering plan.

## From 08-02 (AeroDatePicker)

- **AeroColorPicker.kt compile error (owner: 08-06).** Uncommitted working-tree
  modifications to `library/src/main/kotlin/com/mordred/aero/components/pickers/AeroColorPicker.kt`
  reference `keyboardOptions` / `keyboardActions` parameters on `AeroTextField` (lines 205-208),
  but `AeroTextField` does not expose those parameters. This breaks `:library:compileKotlin`
  for the whole module. Discovered while running the 08-02 test. Out of 08-02 scope (08-02
  owns AeroDatePicker only). Belongs to plan 08-06; either AeroTextField must gain
  keyboard-options support or AeroColorPicker must drop those args.
