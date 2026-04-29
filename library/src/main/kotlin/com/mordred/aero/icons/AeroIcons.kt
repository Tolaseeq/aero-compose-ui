package com.mordred.aero.icons

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Typed [ImageVector] constants — Phosphor Icons Regular weight.
 *
 * All constants are initialized lazily on first access. Each icon is cached after first use
 * and reused for the lifetime of the process.
 *
 * ## Naming convention
 *
 * Phosphor kebab-case names map 1-to-1 to PascalCase Kotlin identifiers. Look up icons at
 * [phosphoricons.com](https://phosphoricons.com) and use the source name directly — no
 * renaming layer. Phosphor's vocabulary differs from Material/Feather conventions:
 *
 * | Phosphor name      | AeroIcons identifier | NOT (other libraries)   |
 * |--------------------|----------------------|-------------------------|
 * | `caret-down`       | `CaretDown`          | ~~ChevronDown~~         |
 * | `magnifying-glass` | `MagnifyingGlass`    | ~~Search~~              |
 * | `house`            | `House`              | ~~Home~~                |
 * | `funnel`           | `Funnel`             | ~~Filter~~              |
 * | `gear`             | `Gear`               | ~~Settings~~            |
 * | `x`                | `X`                  | ~~Close~~               |
 * | `eye-slash`        | `EyeSlash`           | ~~EyeOff~~              |
 * | `envelope`         | `Envelope`           | ~~Mail~~                |
 *
 * ## Source and license
 *
 * Source: [Phosphor Icons](https://github.com/phosphor-icons/core) Regular weight, MIT license
 * (see `tools/phosphor-svgs/LICENSE`). Each glyph is a 256×256 viewBox with stroke-width 16,
 * round line caps, and round line joins.
 *
 * ## Recommended size
 *
 * Render at **16dp–32dp**. The default `Icon()` size of 24dp is the canonical target. Below
 * 14dp the 16-unit stroke (`16/256 ≈ 6.25%` of the viewBox) renders thinner than 1dp at 96 DPI
 * and antialiases to a faint line — for very small contexts (e.g. AeroNumberSpinner up/down
 * buttons) consider a heavier mitigation (Canvas draw or larger button).
 *
 * ## Tint is mandatory
 *
 * `androidx.compose.material3.Icon()` defaults its tint to `LocalContentColor`, which
 * [com.mordred.aero.theme.AeroTheme] does **not** set. On AeroDark and Classic themes the
 * inherited `LocalContentColor` is unpredictable and may render icons invisible. Every
 * `Icon()` call site inside `:library` and `:showcase` MUST pass an explicit tint:
 *
 * ```kotlin
 * Icon(
 *     imageVector = AeroIcons.X,
 *     contentDescription = "Close",
 *     tint = AeroTheme.colors.onSurface
 * )
 * ```
 *
 * Look-up: <https://phosphoricons.com>.
 */
public object AeroIcons {
    // Properties are extension properties; see icons/internal/*.kt
}
