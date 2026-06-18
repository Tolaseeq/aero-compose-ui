# Feature Research — v1.1 AeroIcons (Phosphor Edition)

**Domain:** Compose Desktop UI component library — icon set milestone (v1.1)
**Researched:** 2026-04-29
**Confidence:** HIGH (Phosphor icon names confirmed via dev778g-me/PhosphorIcon-compose Kotlin port, Iconify ph/ collection, iconbolt.com phosphor-regular listings, and official Phosphor React package; source SVG format confirmed from phosphor-icons/core repository)

> **REVISION NOTE:** This document replaces the 2026-04-28 version that used Feather icon names.
> The source has been changed to **Phosphor Icons Regular weight** throughout. All Feather names
> (chevron-*, eye-off, search, alert-triangle, alert-circle, help-circle, maximize-2, minimize-2)
> are replaced with their Phosphor equivalents (caret-*, eye-slash, magnifying-glass, warning,
> x-circle, question, frame-corners / square, minus). Migration touchpoints are unchanged.

---

## Context

This document covers only the **v1.1 AeroIcons milestone** — the addition of a typed ImageVector
icon set to the existing aero-compose-ui library. All v1.0 component features are already shipped.
The feature question here is: which icons to include, how to name them, and what to exclude.

Source confirmed: **Phosphor Icons Regular weight**
- License: MIT
- Viewbox: 256×256
- Stroke-width: 16 (equivalent ~1.5 px at 24 dp render)
- Caps/joins: round — matching Win7-toolbar-glyph softness
- Repository: https://github.com/phosphor-icons/core — SVGs at `raw/regular/*.svg`
- File naming: `{icon-name}-regular.svg` (e.g. `caret-down-regular.svg`)
- Total Regular weight icons: ~1,300 (Phosphor v2.1+)

---

## Part 1: Required Icons — Current Usages in aero-compose-ui

Every text-glyph and Material Icons usage in `:library` must map to a concrete Phosphor icon name.

### 1.1 Text Glyphs (must replace)

| Component | File | Glyph Used | Role | Phosphor Name (kebab) | Kotlin Identifier |
|-----------|------|-----------|------|----------------------|-------------------|
| `AeroDropdown` | `dropdown/AeroDropdown.kt:108` | `"▼"` text | Collapsed indicator | `caret-down` | `AeroIcons.CaretDown` |
| `AeroNumberSpinner` | `input/AeroNumberSpinner.kt:129` | `"▲"` Text | Increment button | `caret-up` | `AeroIcons.CaretUp` |
| `AeroNumberSpinner` | `input/AeroNumberSpinner.kt:141` | `"▼"` Text | Decrement button | `caret-down` | `AeroIcons.CaretDown` |
| `AeroCheckbox` | `selection/AeroCheckbox.kt:97` | `"✓"` Text | Checked state mark | `check` | `AeroIcons.Check` |
| `AeroCheckbox` | `selection/AeroCheckbox.kt:98` | `"–"` Text | Indeterminate state | `minus` | `AeroIcons.Minus` |
| `AeroTitleBar` | `navigation/AeroTitleBar.kt:107` | `"─"` glyph | Minimize window | `minus` | `AeroIcons.Minus` |
| `AeroTitleBar` | `navigation/AeroTitleBar.kt:113` | `"□"` glyph | Maximize window | `square` | `AeroIcons.Square` |
| `AeroTitleBar` | `navigation/AeroTitleBar.kt:113` | `"❒"` glyph | Restore from maximized | `frame-corners` | `AeroIcons.FrameCorners` |
| `AeroTitleBar` | `navigation/AeroTitleBar.kt:125` | `"✕"` glyph | Close window | `x` | `AeroIcons.X` |
| `AeroToastHost` | `overlay/AeroToastHost.kt:92` | `"✕"` Text | Dismiss toast | `x` | `AeroIcons.X` |
| `AeroNotificationBanner` | `overlay/AeroNotificationBanner.kt:64` | `"✕"` Text | Dismiss banner | `x` | `AeroIcons.X` |
| `AeroContextMenu` | `overlay/AeroContextMenu.kt:183` | `"▶"` Text | Submenu indicator | `caret-right` | `AeroIcons.CaretRight` |
| `AeroSearchField` | `input/AeroSearchField.kt:81–110` | Canvas magnifier | Search leading icon | `magnifying-glass` | `AeroIcons.MagnifyingGlass` |
| `AeroSearchField` | `input/AeroSearchField.kt:121` | `"x"` Text | Clear button | `x` | `AeroIcons.X` |
| `AeroPasswordField` | `input/AeroPasswordField.kt:121–122` | Canvas eye open | Show password toggle | `eye` | `AeroIcons.Eye` |
| `AeroPasswordField` | `input/AeroPasswordField.kt:121–122` | Canvas eye+slash | Hide password toggle | `eye-slash` | `AeroIcons.EyeSlash` |
| `AeroFilePicker` | `input/AeroFilePicker.kt:71` | `"Обзор"` text | Browse file system | `folder` | `AeroIcons.Folder` |

**Notes:**
- `AeroBreadcrumb` uses `separator: String = "›"` — this is intentionally a `String` parameter, not
  an icon. The default `"›"` text separator stays as-is in v1.1.
- `AeroContextMenu` Action items have an `item.icon: ImageVector?` slot already — consumers pass
  their own icon there. The only text-glyph to replace is the `"▶"` submenu indicator on line 183.
- `AeroFilePicker` "Обзор" is currently text-only. Migration to `Icon(AeroIcons.Folder)` is
  optional (a component-level decision). The icon is available; inclusion is at implementer's
  discretion.
- `AeroTitleBar` maximize/restore: Phosphor `square` (a simple open square) maps to the plain
  maximize glyph "□"; `frame-corners` (four corner-brackets) maps to the restore-from-maximized
  glyph "❒". Both are confirmed present in Phosphor Regular.

### 1.2 Material Icons (must replace to remove `compose.materialIconsExtended` dependency)

| File | Line | Material Icon | Role | Phosphor Name (kebab) | Kotlin Identifier |
|------|------|--------------|------|----------------------|-------------------|
| `AeroAlertKind.kt` | 23 | `Icons.Outlined.Info` | Info alert icon | `info` | `AeroIcons.Info` |
| `AeroAlertKind.kt` | 24 | `Icons.Outlined.Warning` | Warning alert icon | `warning` | `AeroIcons.Warning` |
| `AeroAlertKind.kt` | 25 | `Icons.Outlined.Error` | Error alert icon | `x-circle` | `AeroIcons.XCircle` |
| `AeroAlertKind.kt` | 26 | `Icons.Outlined.HelpOutline` | Question/help icon | `question` | `AeroIcons.Question` |
| `AeroBannerKind.kt` | 19 | `Icons.Outlined.Info` | Info banner icon | `info` | `AeroIcons.Info` |
| `AeroBannerKind.kt` | 20 | `Icons.Outlined.Warning` | Warning banner icon | `warning` | `AeroIcons.Warning` |
| `AeroBannerKind.kt` | 21 | `Icons.Outlined.Error` | Error banner icon | `x-circle` | `AeroIcons.XCircle` |
| `AeroBannerKind.kt` | 22 | `Icons.Outlined.CheckCircle` | Success banner icon | `check-circle` | `AeroIcons.CheckCircle` |

**Phosphor naming notes vs. Feather/Material:**
- Material `Icons.Outlined.Warning` → Phosphor `warning` (triangle with `!`) — confirmed name
- Material `Icons.Outlined.Error` → Phosphor `x-circle` (circle with X) — stronger than
  `warning-circle` for error severity. `warning-octagon` is also available for critical/stop-sign
  style; `x-circle` is the recommended default.
- Material `Icons.Outlined.HelpOutline` → Phosphor `question` (standalone `?` mark) —
  confirmed in Phosphor React port. Alternatively `question-mark` but `question` is the base name.
- Material `Icons.Outlined.CheckCircle` → Phosphor `check-circle` — exact match.

**Total unique icons required for migration: 15 distinct icons.**

`x, caret-down, caret-up, caret-right, check, minus, square, frame-corners, magnifying-glass,
eye, eye-slash, folder, info, warning, x-circle, question, check-circle`

---

## Part 2: Standard Icons (~100–115 commonly needed)

Not required by existing components but needed by consuming apps. Selection cross-references the
Phosphor Regular corpus (~1,300 icons) against desktop UI patterns.

**Phosphor naming idiosyncrasies vs. industry conventions** (critical for developers):

| Industry Convention | Phosphor Actual Name | Notes |
|--------------------|---------------------|-------|
| `home` | `house` | Phosphor uses `house`, not `home` |
| `settings` / `gear` | `gear` | Phosphor has `gear` (6-tooth) and `gear-six` (6-tooth variant) |
| `filter` | `funnel` | Phosphor uses `funnel`, not `filter` |
| `send` | `paper-plane` | Phosphor uses `paper-plane` |
| `mail` | `envelope` | Phosphor uses `envelope`, not `mail` |
| `message` | `chat-circle` | Phosphor uses `chat-circle` for round chat bubbles |
| `external-link` | `arrow-square-out` | Phosphor's external-link icon name |
| `sort` | `arrows-down-up` | Phosphor sort arrows |
| `more-horizontal` | `dots-three` | Phosphor's three dots horizontal |
| `more-vertical` | `dots-three-vertical` | Phosphor's three dots vertical |
| `delete` / `trash` | `trash` | Phosphor also has `trash-simple` (simpler form) |
| `edit` | `pencil-simple` | Phosphor prefers `pencil-simple` for edit actions |
| `save` | `floppy-disk` | Phosphor's save icon name |
| `zoom-in` | `magnifying-glass-plus` | Phosphor's zoom-in |
| `zoom-out` | `magnifying-glass-minus` | Phosphor's zoom-out |
| `eye-off` | `eye-slash` | Phosphor uses `eye-slash`, NOT `eye-off` |
| `chevron-*` | `caret-*` | Phosphor uses `caret-down/up/left/right` not `chevron-*` |
| `volume-x` (mute) | `speaker-x` | Phosphor speaker mute |
| `volume` / `volume-2` | `speaker-high` / `speaker-low` | Phosphor speaker volume levels |
| `mic` | `microphone` | Phosphor uses full word `microphone` |
| `mic-off` | `microphone-slash` | Phosphor mic mute |
| `wifi` (full) | `wifi-high` | Phosphor uses signal-level naming |
| `wifi-off` | `wifi-slash` | Phosphor wifi disabled |
| `unlock` | `lock-open` | Phosphor: `lock-open` (confirmed via iconbolt) |
| `slash` / `no` | `prohibit` | Phosphor's prohibition/not-allowed icon |
| `terminal` | `terminal-window` | Phosphor's terminal icon |
| `stop-circle` | `stop` | Phosphor uses `stop` for a filled square stop |

### 2.1 Editor Controls

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `pencil-simple` | `PencilSimple` | Edit / pencil (primary edit action) |
| `floppy-disk` | `FloppyDisk` | Save file |
| `copy` | `Copy` | Copy to clipboard |
| `scissors` | `Scissors` | Cut |
| `clipboard` | `Clipboard` | Paste / clipboard |
| `arrow-counter-clockwise` | `ArrowCounterClockwise` | Undo |
| `arrow-clockwise` | `ArrowClockwise` | Redo / refresh |
| `trash` | `Trash` | Delete to trash (standard) |
| `trash-simple` | `TrashSimple` | Delete (simpler minimal form) |
| `broom` | `Broom` | Clear / clean action |

### 2.2 File Operations

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `file` | `File` | Generic file |
| `file-text` | `FileText` | Text / document file |
| `files` | `Files` | Multiple files / copy-file concept |
| `folder` | `Folder` | Closed folder (also Required §1.1) |
| `folder-open` | `FolderOpen` | Open folder |
| `folder-plus` | `FolderPlus` | New folder |
| `download` | `Download` | Download to device |
| `upload` | `Upload` | Upload from device |
| `paperclip` | `Paperclip` | Attach file |
| `archive` | `Archive` | Archive / compress |
| `printer` | `Printer` | Print |

### 2.3 Navigation & Application Shell

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `house` | `House` | Home / dashboard (NOTE: `house` not `home`) |
| `list` | `List` | Menu / list view / hamburger |
| `dots-three` | `DotsThree` | More actions (horizontal) |
| `dots-three-vertical` | `DotsThreeVertical` | More actions (vertical) |
| `arrow-left` | `ArrowLeft` | Back / previous |
| `arrow-right` | `ArrowRight` | Forward / next |
| `arrow-up` | `ArrowUp` | Up |
| `arrow-down` | `ArrowDown` | Down |
| `arrow-square-out` | `ArrowSquareOut` | External link / open in new window |
| `arrow-bend-up-left` | `ArrowBendUpLeft` | Back / undo direction |
| `sign-in` | `SignIn` | Login / sign in |
| `sign-out` | `SignOut` | Logout / sign out |

### 2.4 Status & Feedback

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `bell` | `Bell` | Notifications |
| `bell-slash` | `BellSlash` | Mute notifications |
| `calendar` | `Calendar` | Calendar / date picker trigger |
| `calendar-blank` | `CalendarBlank` | Empty calendar |
| `clock` | `Clock` | Time / clock |
| `lock` | `Lock` | Locked / secure |
| `lock-open` | `LockOpen` | Unlocked |
| `shield` | `Shield` | Security / protection |
| `shield-warning` | `ShieldWarning` | Security warning |
| `warning-circle` | `WarningCircle` | Mild warning (circle) |
| `warning-diamond` | `WarningDiamond` | Warning (diamond shape) |
| `warning-octagon` | `WarningOctagon` | Critical / stop-sign warning |
| `spinner` | `Spinner` | Loading indicator |
| `flag` | `Flag` | Flag / mark item |

### 2.5 Communication

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `envelope` | `Envelope` | Email (NOTE: `envelope` not `mail`) |
| `chat-circle` | `ChatCircle` | Chat message (round bubble) |
| `chat-circle-text` | `ChatCircleText` | Chat with lines (text content) |
| `phone` | `Phone` | Phone call |
| `paper-plane` | `PaperPlane` | Send message (NOTE: `paper-plane` not `send`) |

### 2.6 Media & Audio/Video

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `play` | `Play` | Play media |
| `pause` | `Pause` | Pause media |
| `stop` | `Stop` | Stop media |
| `skip-back` | `SkipBack` | Previous track |
| `skip-forward` | `SkipForward` | Next track |
| `fast-forward` | `FastForward` | Fast-forward |
| `rewind` | `Rewind` | Rewind |
| `speaker-high` | `SpeakerHigh` | Volume high (NOTE: `speaker-high` not `volume`) |
| `speaker-low` | `SpeakerLow` | Volume low |
| `speaker-x` | `SpeakerX` | Mute (NOTE: `speaker-x` not `volume-x`) |
| `music-note` | `MusicNote` | Music / audio track |
| `music-notes` | `MusicNotes` | Multiple notes / playlist |
| `video-camera` | `VideoCamera` | Video |
| `image` | `Image` | Image / photo |
| `camera` | `Camera` | Camera |
| `microphone` | `Microphone` | Microphone (NOTE: `microphone` not `mic`) |
| `microphone-slash` | `MicrophoneSlash` | Muted mic (NOTE: `microphone-slash` not `mic-off`) |

### 2.7 System & Devices

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `gear` | `Gear` | Settings / gear (NOTE: `gear` not `settings`) |
| `sliders` | `Sliders` | Adjust / parameters (horizontal sliders) |
| `sliders-horizontal` | `SlidersHorizontal` | Horizontal sliders variant |
| `funnel` | `Funnel` | Filter (NOTE: `funnel` not `filter`) |
| `arrows-down-up` | `ArrowsDownUp` | Sort ascending/descending |
| `magnifying-glass-plus` | `MagnifyingGlassPlus` | Zoom in |
| `magnifying-glass-minus` | `MagnifyingGlassMinus` | Zoom out |
| `frame-corners` | `FrameCorners` | Full-screen / maximize panel (also Required §1.1) |
| `monitor` | `Monitor` | Display / screen |
| `desktop-tower` | `DesktopTower` | Desktop computer |
| `hard-drive` | `HardDrive` | Hard drive / storage |
| `database` | `Database` | Database |
| `cloud` | `Cloud` | Cloud |
| `cloud-arrow-down` | `CloudArrowDown` | Download from cloud |
| `cloud-arrow-up` | `CloudArrowUp` | Upload to cloud |
| `wifi-high` | `WifiHigh` | Wi-Fi connected full signal (NOTE: `wifi-high`) |
| `wifi-slash` | `WifiSlash` | Wi-Fi disconnected (NOTE: `wifi-slash`) |
| `bluetooth` | `Bluetooth` | Bluetooth |
| `battery-full` | `BatteryFull` | Battery full |
| `battery-low` | `BatteryLow` | Battery low |
| `battery-empty` | `BatteryEmpty` | Battery empty |
| `power` | `Power` | Power on/off |
| `lightning` | `Lightning` | Power / lightning bolt / charge |
| `lightbulb` | `Lightbulb` | Idea / lightbulb |

### 2.8 Developer / Tools

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `wrench` | `Wrench` | Tool / wrench |
| `gear-six` | `GearSix` | Settings alternate (6-point gear) |
| `code` | `Code` | Code / source |
| `terminal-window` | `TerminalWindow` | Terminal / CLI |
| `bug` | `Bug` | Bug / debug |
| `cpu` | `Cpu` | CPU / processor |
| `link` | `Link` | Hyperlink |
| `link-simple` | `LinkSimple` | Simplified link icon |
| `key` | `Key` | API key / password key |
| `hash` | `Hash` | Hash / ID |
| `globe` | `Globe` | Globe / web |

### 2.9 User & Identity

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `user` | `User` | Single user |
| `user-circle` | `UserCircle` | User with circle frame |
| `users` | `Users` | Multiple users / group |
| `user-plus` | `UserPlus` | Add user |
| `user-minus` | `UserMinus` | Remove user |
| `user-check` | `UserCheck` | Verified / approved user |

### 2.10 Actions & UI Controls

| Phosphor Name (kebab) | Kotlin Identifier | Purpose |
|----------------------|-------------------|---------|
| `heart` | `Heart` | Favorite / like |
| `star` | `Star` | Star / rate |
| `bookmark-simple` | `BookmarkSimple` | Bookmark / save for later |
| `share-network` | `ShareNetwork` | Share |
| `eye` | `Eye` | Visibility (also Required §1.1) |
| `eye-slash` | `EyeSlash` | Hidden / masked (also Required §1.1) |
| `prohibit` | `Prohibit` | Forbidden / not allowed (NOTE: `prohibit` not `slash`) |
| `plus` | `Plus` | Add / create |
| `plus-circle` | `PlusCircle` | Add (circled) |
| `minus-circle` | `MinusCircle` | Remove (circled) |
| `arrow-up-right` | `ArrowUpRight` | Open / navigate up-right |
| `sort-ascending` | `SortAscending` | Sort A-Z |
| `sort-descending` | `SortDescending` | Sort Z-A |
| `map-pin` | `MapPin` | Location / pin |

---

## Part 3: Complete v1.1 Icon List (Master Reference — 138 icons)

Authoritative list of all AeroIcons for v1.1. Phosphor kebab-case → Kotlin PascalCase.

**Mapping rule:** kebab-case → PascalCase; hyphens removed, each segment capitalized.
- `caret-down` → `CaretDown`
- `magnifying-glass` → `MagnifyingGlass`
- `eye-slash` → `EyeSlash`
- `arrow-counter-clockwise` → `ArrowCounterClockwise`
- `x` → `X` (single-letter stays as-is)

| # | Phosphor (kebab-case) | Kotlin `AeroIcons.*` | Category | Required |
|---|----------------------|---------------------|----------|----------|
| 1 | `archive` | `Archive` | Files | |
| 2 | `arrow-bend-up-left` | `ArrowBendUpLeft` | Navigation | |
| 3 | `arrow-clockwise` | `ArrowClockwise` | Editor | |
| 4 | `arrow-counter-clockwise` | `ArrowCounterClockwise` | Editor | |
| 5 | `arrow-down` | `ArrowDown` | Navigation | |
| 6 | `arrow-left` | `ArrowLeft` | Navigation | |
| 7 | `arrow-right` | `ArrowRight` | Navigation | |
| 8 | `arrow-square-out` | `ArrowSquareOut` | Navigation | |
| 9 | `arrow-up` | `ArrowUp` | Navigation | |
| 10 | `arrow-up-right` | `ArrowUpRight` | Actions | |
| 11 | `arrows-down-up` | `ArrowsDownUp` | System | |
| 12 | `battery-empty` | `BatteryEmpty` | System | |
| 13 | `battery-full` | `BatteryFull` | System | |
| 14 | `battery-low` | `BatteryLow` | System | |
| 15 | `bell` | `Bell` | Status | |
| 16 | `bell-slash` | `BellSlash` | Status | |
| 17 | `bluetooth` | `Bluetooth` | System | |
| 18 | `bookmark-simple` | `BookmarkSimple` | Actions | |
| 19 | `broom` | `Broom` | Editor | |
| 20 | `bug` | `Bug` | Dev | |
| 21 | `calendar` | `Calendar` | Status | |
| 22 | `calendar-blank` | `CalendarBlank` | Status | |
| 23 | `camera` | `Camera` | Media | |
| 24 | `caret-down` | `CaretDown` | Navigation | YES |
| 25 | `caret-left` | `CaretLeft` | Navigation | |
| 26 | `caret-right` | `CaretRight` | Navigation | YES |
| 27 | `caret-up` | `CaretUp` | Navigation | YES |
| 28 | `chat-circle` | `ChatCircle` | Communication | |
| 29 | `chat-circle-text` | `ChatCircleText` | Communication | |
| 30 | `check` | `Check` | Actions | YES |
| 31 | `check-circle` | `CheckCircle` | Status | YES |
| 32 | `clipboard` | `Clipboard` | Editor | |
| 33 | `clock` | `Clock` | Status | |
| 34 | `cloud` | `Cloud` | System | |
| 35 | `cloud-arrow-down` | `CloudArrowDown` | System | |
| 36 | `cloud-arrow-up` | `CloudArrowUp` | System | |
| 37 | `code` | `Code` | Dev | |
| 38 | `copy` | `Copy` | Editor | |
| 39 | `cpu` | `Cpu` | Dev | |
| 40 | `database` | `Database` | System | |
| 41 | `desktop-tower` | `DesktopTower` | System | |
| 42 | `dots-three` | `DotsThree` | Navigation | |
| 43 | `dots-three-vertical` | `DotsThreeVertical` | Navigation | |
| 44 | `download` | `Download` | Files | |
| 45 | `envelope` | `Envelope` | Communication | |
| 46 | `eye` | `Eye` | Actions | YES |
| 47 | `eye-slash` | `EyeSlash` | Actions | YES |
| 48 | `fast-forward` | `FastForward` | Media | |
| 49 | `file` | `File` | Files | |
| 50 | `file-text` | `FileText` | Files | |
| 51 | `files` | `Files` | Files | |
| 52 | `flag` | `Flag` | Status | |
| 53 | `floppy-disk` | `FloppyDisk` | Editor | |
| 54 | `folder` | `Folder` | Files | YES |
| 55 | `folder-open` | `FolderOpen` | Files | |
| 56 | `folder-plus` | `FolderPlus` | Files | |
| 57 | `frame-corners` | `FrameCorners` | Navigation | YES (restore) |
| 58 | `funnel` | `Funnel` | System | |
| 59 | `gear` | `Gear` | System | |
| 60 | `gear-six` | `GearSix` | System | |
| 61 | `globe` | `Globe` | Dev | |
| 62 | `hard-drive` | `HardDrive` | System | |
| 63 | `hash` | `Hash` | Dev | |
| 64 | `heart` | `Heart` | Actions | |
| 65 | `house` | `House` | Navigation | |
| 66 | `image` | `Image` | Media | |
| 67 | `info` | `Info` | Status | YES |
| 68 | `key` | `Key` | Dev | |
| 69 | `lightning` | `Lightning` | System | |
| 70 | `lightbulb` | `Lightbulb` | System | |
| 71 | `link` | `Link` | Dev | |
| 72 | `link-simple` | `LinkSimple` | Dev | |
| 73 | `list` | `List` | Navigation | |
| 74 | `lock` | `Lock` | Status | |
| 75 | `lock-open` | `LockOpen` | Status | |
| 76 | `magnifying-glass` | `MagnifyingGlass` | Actions | YES |
| 77 | `magnifying-glass-minus` | `MagnifyingGlassMinus` | System | |
| 78 | `magnifying-glass-plus` | `MagnifyingGlassPlus` | System | |
| 79 | `map-pin` | `MapPin` | Actions | |
| 80 | `microphone` | `Microphone` | Media | |
| 81 | `microphone-slash` | `MicrophoneSlash` | Media | |
| 82 | `minus` | `Minus` | Actions | YES |
| 83 | `minus-circle` | `MinusCircle` | Actions | |
| 84 | `monitor` | `Monitor` | System | |
| 85 | `music-note` | `MusicNote` | Media | |
| 86 | `music-notes` | `MusicNotes` | Media | |
| 87 | `paperclip` | `Paperclip` | Files | |
| 88 | `paper-plane` | `PaperPlane` | Communication | |
| 89 | `pause` | `Pause` | Media | |
| 90 | `pencil-simple` | `PencilSimple` | Editor | |
| 91 | `phone` | `Phone` | Communication | |
| 92 | `play` | `Play` | Media | |
| 93 | `plus` | `Plus` | Actions | |
| 94 | `plus-circle` | `PlusCircle` | Actions | |
| 95 | `power` | `Power` | System | |
| 96 | `printer` | `Printer` | Files | |
| 97 | `prohibit` | `Prohibit` | Actions | |
| 98 | `question` | `Question` | Status | YES |
| 99 | `rewind` | `Rewind` | Media | |
| 100 | `scissors` | `Scissors` | Editor | |
| 101 | `share-network` | `ShareNetwork` | Actions | |
| 102 | `shield` | `Shield` | Status | |
| 103 | `shield-warning` | `ShieldWarning` | Status | |
| 104 | `sign-in` | `SignIn` | Navigation | |
| 105 | `sign-out` | `SignOut` | Navigation | |
| 106 | `skip-back` | `SkipBack` | Media | |
| 107 | `skip-forward` | `SkipForward` | Media | |
| 108 | `sliders` | `Sliders` | System | |
| 109 | `sliders-horizontal` | `SlidersHorizontal` | System | |
| 110 | `sort-ascending` | `SortAscending` | Actions | |
| 111 | `sort-descending` | `SortDescending` | Actions | |
| 112 | `speaker-high` | `SpeakerHigh` | Media | |
| 113 | `speaker-low` | `SpeakerLow` | Media | |
| 114 | `speaker-x` | `SpeakerX` | Media | |
| 115 | `spinner` | `Spinner` | Status | |
| 116 | `square` | `Square` | Navigation | YES (maximize) |
| 117 | `star` | `Star` | Actions | |
| 118 | `stop` | `Stop` | Media | |
| 119 | `terminal-window` | `TerminalWindow` | Dev | |
| 120 | `trash` | `Trash` | Editor | |
| 121 | `trash-simple` | `TrashSimple` | Editor | |
| 122 | `upload` | `Upload` | Files | |
| 123 | `user` | `User` | Identity | |
| 124 | `user-check` | `UserCheck` | Identity | |
| 125 | `user-circle` | `UserCircle` | Identity | |
| 126 | `user-minus` | `UserMinus` | Identity | |
| 127 | `user-plus` | `UserPlus` | Identity | |
| 128 | `users` | `Users` | Identity | |
| 129 | `video-camera` | `VideoCamera` | Media | |
| 130 | `warning` | `Warning` | Status | YES |
| 131 | `warning-circle` | `WarningCircle` | Status | |
| 132 | `warning-diamond` | `WarningDiamond` | Status | |
| 133 | `warning-octagon` | `WarningOctagon` | Status | |
| 134 | `wifi-high` | `WifiHigh` | System | |
| 135 | `wifi-slash` | `WifiSlash` | System | |
| 136 | `wrench` | `Wrench` | Dev | |
| 137 | `x` | `X` | Actions | YES |
| 138 | `x-circle` | `XCircle` | Actions/Status | YES |

**Total: 138 icons** (17 required for migration + 121 standard). Within the 120–150 target.

**Required icons (15 unique, covering all text-glyph and Material Icons replacements):**
`X, CaretDown, CaretUp, CaretRight, Check, Minus, Square, FrameCorners, MagnifyingGlass,
Eye, EyeSlash, Folder, Info, Warning, XCircle, Question, CheckCircle`

---

## Part 4: Naming Convention Decision

### Decision: Keep Phosphor Names (kebab-case → PascalCase), Flat Namespace

**Format:** `AeroIcons.IconName`

Examples:
```kotlin
AeroIcons.CaretDown        // ← caret-down-regular.svg   (NOT ChevronDown)
AeroIcons.MagnifyingGlass  // ← magnifying-glass-regular.svg  (NOT Search)
AeroIcons.EyeSlash         // ← eye-slash-regular.svg  (NOT EyeOff)
AeroIcons.House            // ← house-regular.svg  (NOT Home)
AeroIcons.Funnel           // ← funnel-regular.svg  (NOT Filter)
AeroIcons.Gear             // ← gear-regular.svg  (NOT Settings)
AeroIcons.PaperPlane       // ← paper-plane-regular.svg  (NOT Send)
AeroIcons.Envelope         // ← envelope-regular.svg  (NOT Mail)
AeroIcons.X                // ← x-regular.svg  (NOT Close)
```

### Rationale

**Keep Phosphor names (no renaming to industry conventions):**
Developers look up icons on phosphoricons.com using Phosphor names. Renaming creates an invisible
mapping layer: when a developer searches `phosphoricons.com` for "home" they find `house`; if we
call it `Home` they must know the renaming happened. Keeping Phosphor names means the identifier
in code is a direct 1:1 match with the source asset name — zero lookup friction.

**Why flat (not grouped by category):**
At ~138 icons, grouping adds friction without benefit. Grouped namespaces
(`AeroIcons.Navigation.CaretDown`) require knowing the category before finding the icon. Material
Icons uses flat-by-style (`Icons.Outlined.CaretDown`). The PhosphorIcon-compose Kotlin library
uses `PhIcons.Regular.CaretDownRegular`. A flat `AeroIcons.CaretDown` is simpler and consistent
with what Material Icons consumers already know. Grouping pays off at 500+ icons.

**Why PascalCase:**
Kotlin idiomatic for `val` in `object`. Phosphor's kebab-case (`caret-down`) cannot be a Kotlin
identifier. PascalCase matches Material Icons convention (`Icons.Outlined.CheckCircle`).

**Why `AeroIcons.` prefix:**
Library namespace is `Aero*`. Using `AeroIcons` is consistent with `AeroButton`, `AeroTextField`.
Avoids name collision with `Icons` (Material) in consuming projects.

**Number-suffix convention:**
When Phosphor uses numeric suffixes (`gear-six` → `GearSix`), the number is written as a word in
PascalCase since it follows a hyphen. Single-letter names like `x` become `X`.

### KDoc naming-convention note (to include in `AeroIcons.kt`)

```kotlin
/**
 * Typed [ImageVector] constants — Phosphor Icons Regular weight, ported to Compose.
 *
 * **Naming:** Phosphor kebab-case names are mapped 1-to-1 to PascalCase Kotlin identifiers.
 * Look up icons at phosphoricons.com using the source name; the Kotlin name follows directly.
 *
 * Examples:
 * - phosphoricons.com "caret-down" → `AeroIcons.CaretDown`
 * - phosphoricons.com "magnifying-glass" → `AeroIcons.MagnifyingGlass`
 * - phosphoricons.com "house" → `AeroIcons.House`  (Phosphor uses `house`, not `home`)
 * - phosphoricons.com "funnel" → `AeroIcons.Funnel`  (Phosphor uses `funnel`, not `filter`)
 * - phosphoricons.com "gear" → `AeroIcons.Gear`  (Phosphor uses `gear`, not `settings`)
 *
 * Source: Phosphor Icons Regular, MIT license. ViewBox 256×256, stroke-width 16.
 */
public object AeroIcons {
    public val CaretDown: ImageVector get() = loadCaretDown()
    // …
}
```

### Kotlin Implementation Pattern

```kotlin
// AeroIcons.kt — top-level object in :library
public object AeroIcons {
    public val X: ImageVector              get() = loadX()
    public val CaretDown: ImageVector      get() = loadCaretDown()
    public val CaretUp: ImageVector        get() = loadCaretUp()
    public val CaretRight: ImageVector     get() = loadCaretRight()
    public val CaretLeft: ImageVector      get() = loadCaretLeft()
    public val Check: ImageVector          get() = loadCheck()
    public val CheckCircle: ImageVector    get() = loadCheckCircle()
    public val Minus: ImageVector          get() = loadMinus()
    public val Square: ImageVector         get() = loadSquare()
    public val FrameCorners: ImageVector   get() = loadFrameCorners()
    public val MagnifyingGlass: ImageVector get() = loadMagnifyingGlass()
    public val Eye: ImageVector            get() = loadEye()
    public val EyeSlash: ImageVector       get() = loadEyeSlash()
    public val Folder: ImageVector         get() = loadFolder()
    public val Info: ImageVector           get() = loadInfo()
    public val Warning: ImageVector        get() = loadWarning()
    public val XCircle: ImageVector        get() = loadXCircle()
    public val Question: ImageVector       get() = loadQuestion()
    // … 121 more
}
```

Each icon lives in a private `load*()` function in its own file under `icons/` package. Lazy `get()`
ensures only accessed icons are constructed — identical to Material Icons' lazy backing field pattern.

---

## Part 5: Migration Mapping (Per-Component Recipe)

Exact per-file changes, using Phosphor names throughout.

### AeroCheckbox.kt (`selection/AeroCheckbox.kt`)
- Line 97: `Text("✓", ...)` → `Icon(AeroIcons.Check, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(12.dp))`
- Line 98: `Text("–", ...)` → `Icon(AeroIcons.Minus, contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(12.dp))`

### AeroDropdown.kt (`dropdown/AeroDropdown.kt`)
- Line 108: `Text("▼", ...)` → `Icon(AeroIcons.CaretDown, contentDescription = null, tint = colors.labelText, modifier = Modifier.size(12.dp))`

### AeroNumberSpinner.kt (`input/AeroNumberSpinner.kt`)
- Line 129: `Text("▲", ...)` → `Icon(AeroIcons.CaretUp, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(10.dp))`
- Line 141: `Text("▼", ...)` → `Icon(AeroIcons.CaretDown, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(10.dp))`

### AeroTitleBar.kt (`navigation/AeroTitleBar.kt`)
- Line 107 (minimize call): `glyph = "─"` → replace TitleBarButton to accept `ImageVector`; use `AeroIcons.Minus`
- Line 113 (maximize/restore): `glyph = if (maximized) "❒" else "□"` → `if (maximized) AeroIcons.FrameCorners else AeroIcons.Square`
- Line 125 (close): `glyph = "✕"` → `AeroIcons.X`
- Internal: `TitleBarButton` composable signature changes from `glyph: String` + `Text(glyph)` to `icon: ImageVector` + `Icon(icon, ...)`

### AeroToastHost.kt (`overlay/AeroToastHost.kt`)
- Line 92: `Text("✕", ...)` inside AeroIconButton → `Icon(AeroIcons.X, contentDescription = "Dismiss", tint = colors.onSurface)`

### AeroNotificationBanner.kt (`overlay/AeroNotificationBanner.kt`)
- Line 64: `Text("✕", ...)` inside AeroIconButton → `Icon(AeroIcons.X, contentDescription = "Dismiss", tint = colors.onSurface)`

### AeroContextMenu.kt (`overlay/AeroContextMenu.kt`)
- Line 183: `Text("▶", ...)` → `Icon(AeroIcons.CaretRight, contentDescription = null, tint = colors.labelText, modifier = Modifier.size(12.dp))`

### AeroSearchField.kt (`input/AeroSearchField.kt`)
- Remove `SearchIcon()` composable (lines 81–111) entirely
- Replace `SearchIcon()` call (line 61 area) with `Icon(AeroIcons.MagnifyingGlass, contentDescription = "Search", tint = AeroTheme.colors.labelText, modifier = Modifier.size(14.dp))`
- Line 121: `Text("x", ...)` in ClearButton → `Icon(AeroIcons.X, contentDescription = "Clear", tint = AeroTheme.colors.labelText, modifier = Modifier.size(12.dp))`

### AeroPasswordField.kt (`input/AeroPasswordField.kt`)
- Remove `EyeOpenIcon()` composable (lines 136–164) entirely
- Remove `EyeClosedIcon()` composable (lines 171–205) entirely
- Line 121–122: replace `if (visible) EyeOpenIcon(...) else EyeClosedIcon(...)` with `Icon(if (visible) AeroIcons.Eye else AeroIcons.EyeSlash, contentDescription = if (visible) "Hide password" else "Show password", tint = colors.labelText, modifier = Modifier.size(14.dp))`

### AeroAlertKind.kt (`overlay/AeroAlertKind.kt`)
- Remove all `import androidx.compose.material.icons.*` lines
- Change `icon` property to use `AeroIcons.*`:
  - `Info` → `AeroIcons.Info`
  - `Warning` → `AeroIcons.Warning`
  - `Error` → `AeroIcons.XCircle`
  - `Question` → `AeroIcons.Question`

### AeroBannerKind.kt (`overlay/AeroBannerKind.kt`)
- Remove all `import androidx.compose.material.icons.*` lines
- Change `icon` property to use `AeroIcons.*`:
  - `Info` → `AeroIcons.Info`
  - `Warning` → `AeroIcons.Warning`
  - `Error` → `AeroIcons.XCircle`
  - `Success` → `AeroIcons.CheckCircle`

### build.gradle.kts (`:library`)
- After all migrations complete: remove `implementation(compose.materialIconsExtended)` line.

---

## Feature Landscape

### Table Stakes (v1.1 Must Have)

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| All 15 required migration icons in `AeroIcons` | Cannot remove `materialIconsExtended` without them | LOW | Direct SVG-path port from Phosphor Regular |
| `AeroIcons` Kotlin object with typed `ImageVector` constants | Compile-time safety, IDE autocomplete | LOW | Same pattern as `object Icons` in Material |
| Flat `AeroIcons.IconName` access | Discoverable via IDE at ~138 icons | LOW | No category grouping needed at this scale |
| PascalCase naming mirroring Phosphor source | Kotlin idiomatic; 1-to-1 with phosphoricons.com | LOW | kebab → PascalCase conversion rule |
| All icons 256×256 viewBox, stroke-width 16, rounded caps/joins | Visual consistency | LOW | Port Phosphor Regular spec faithfully |
| Migration of all 10 components off text glyphs | Every component uses `AeroIcons` | MEDIUM | Per-component `Icon()` replacement |
| Showcase `IconsSection` with grid + search | Developers must browse the set visually | MEDIUM | `AeroSearchField` + `LazyVerticalGrid` |
| Removal of `compose.materialIconsExtended` from `:library` | Reduces JAR size; explicit v1.1 goal | LOW | After AeroAlertKind + AeroBannerKind migrated |

### Differentiators (v1.1 Value-Adds)

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| Phosphor Regular aesthetic matches Aero visual style | Rounded stroke, no harsh fills — aligns with glassmorphism and Win7-toolbar-glyph | LOW | Zero design work; Phosphor Regular is already correct aesthetic |
| ~138 icons covers 95% of desktop app needs | Consumer does not need a second icon dependency | LOW | Selection driven by desktop UI pattern analysis |
| MIT-licensed source | No attribution overhead for library consumers | LOW | Phosphor is MIT — port paths directly |
| Single-file `AeroIcons` object | One import, one dependency | LOW | All in `:library` JAR |
| 1-to-1 name traceability to phosphoricons.com | Developer look-up is frictionless | LOW | No renaming layer to maintain |

### Anti-Features (Explicitly NOT in v1.1)

| Anti-Feature | Why Requested | Why Excluded | Alternative |
|--------------|--------------|--------------|-------------|
| Multiple weights (Thin/Light/Bold/Fill/Duotone) | Phosphor-style flexibility | Doubles/triples icon count; all existing components need only Regular; complicates API | Regular only in v1.1; add `AeroIcons.Filled.*` in v2 if consumer demand exists |
| Filled / duotone variants | Richer visual expression | Filled contradicts "soft outline" Aero aesthetic; out of scope per PROJECT.md | Port filled variants in v2 |
| Brand/social logos (GitHub, Slack, Discord, etc.) | Common request | Trademark compliance burden; irrelevant to generic UI library | Not part of Phosphor Regular's intended use; explicitly excluded |
| Currency-specific icons (bitcoin, dollar-sign, euro) | Completeness | Too narrow for v1.1; Phosphor has `currency-*` variants but they serve specialized financial UI | Add on-demand when a specific consumer needs them |
| Renamed icons (e.g. `Home` instead of `House`) | Match Feather/Material naming convention | Creates invisible mapping: devs search phosphoricons.com and find `house`, not `home` | Keep Phosphor names; document in KDoc |
| Custom user icon registration (`AeroIcons.register(...)`) | Plugin extensibility | Overcomplicates API; consumers pass any `ImageVector` to `Icon()` directly | Consumer uses `Icon(myVector)` — no registration API needed |
| Icons in separate `:icons` Gradle module | Modular delivery | Over-engineers for v1.1; decided to keep in `:library` per PROJECT.md | Separate module in v2 if JAR size matters |
| Multi-weight numeric suffix in identifier (`CaretDownRegular`) | Explicit weight in name | Only one weight in v1.1 — suffix is noise; if/when Bold is added, add `AeroIcons.Bold.*` sub-object | `AeroIcons.CaretDown` (no weight suffix for default Regular) |

**Explicitly excluded Phosphor icons (not in the 138-icon list):**
- Brand/logo icons: all `*-logo` variants (amazon-logo, android-logo, apple-logo, etc.)
- Currency: `currency-*`, `bitcoin`, `coins`, `money`
- Weather: `cloud-rain`, `cloud-snow`, `cloud-lightning`, `sun`, `moon`, `thermometer`, `wind`
- Medical: `first-aid`, `heartbeat`, `stethoscope`, `pill`, `syringe`
- Food/beverage: `coffee`, `fork-knife`, `wine`, `beer`
- Highly specific transport: `airplane`, `train`, `bus`, `bicycle`, `boat`
- Emoji-face icons: `smiley`, `smiley-wink`, `smiley-sad`, `skull`
- Decorative/geometric: `star-four`, `hexagon`, `octagon`, `diamond` (keep `warning-diamond` as status icon)
- Duplicate arrows: `arrow-fat-*` variants (keep only `arrow-*` base names)
- Text-editor formatting: `text-aa`, `text-align-*`, `bold`, `italic`, `underline` (text editor toolbar not in scope)

---

## Feature Dependencies

```
AeroIcons object (all 138 ImageVector constants)
    └──required by──> All migrated components
    └──required by──> IconsSection in showcase

Migration — Text Glyphs (→ AeroIcons)
    └──requires──> AeroIcons object (15 required icons available)
    └──affects──> AeroCheckbox       (Check, Minus)
    └──affects──> AeroDropdown       (CaretDown)
    └──affects──> AeroNumberSpinner  (CaretUp, CaretDown)
    └──affects──> AeroTitleBar       (Minus, Square, FrameCorners, X)
    └──affects──> AeroToastHost      (X)
    └──affects──> AeroNotificationBanner (X)
    └──affects──> AeroContextMenu    (CaretRight)
    └──affects──> AeroSearchField    (MagnifyingGlass, X)
    └──affects──> AeroPasswordField  (Eye, EyeSlash)

Migration — Material Icons (→ AeroIcons)
    └──requires──> AeroIcons object (Info, Warning, XCircle, Question, CheckCircle)
    └──affects──> AeroAlertKind      (Info, Warning, XCircle, Question)
    └──affects──> AeroBannerKind     (Info, Warning, XCircle, CheckCircle)
    └──enables──> removal of compose.materialIconsExtended from :library

compose.materialIconsExtended removal
    └──requires──> AeroAlertKind migration complete
    └──requires──> AeroBannerKind migration complete
    └──requires──> grep confirm: zero Icons.* references remain in :library

IconsSection in showcase
    └──requires──> AeroIcons object (all 138)
    └──requires──> AeroSearchField (for live icon name filter)
    └──uses──> LazyVerticalGrid layout
```

### Dependency Notes

- **AeroIcons object must compile before any component migration:** Every component touches `AeroIcons.*`. Build order: `AeroIcons.kt` + all `icons/*.kt` → component migrations.
- **Component migrations are independent of each other:** Once `AeroIcons` compiles, any component can be migrated in any order. No cross-component ordering constraint within the migration step.
- **`materialIconsExtended` removal is the final step:** Remove the Gradle line only after all `Icons.Outlined.*` imports are gone from every file in `:library`. Run a grep to confirm before removing.
- **IconsSection does not block component migration:** Showcase can be updated in parallel.

---

## MVP Definition

### v1.1 Launch With

- [ ] `AeroIcons` object — 138 typed `ImageVector` constants, flat namespace, lazy `get()`
- [ ] All 10 component migrations (AeroCheckbox, AeroDropdown, AeroNumberSpinner, AeroTitleBar, AeroToastHost, AeroNotificationBanner, AeroContextMenu, AeroSearchField, AeroPasswordField — text glyphs + Canvas drawings)
- [ ] `AeroAlertKind` migration — off `Icons.Outlined.*`, onto `AeroIcons.*`
- [ ] `AeroBannerKind` migration — off `Icons.Outlined.*`, onto `AeroIcons.*`
- [ ] `compose.materialIconsExtended` removed from `:library` build
- [ ] `IconsSection` in showcase — `LazyVerticalGrid` of all 138 icons with name labels, `AeroSearchField` live filter

### Deferred to v1.2 or Later

- [ ] Filled icon variants (`AeroIcons.Filled.*`) — only if consumer demand arises post-launch
- [ ] Additional niche icons beyond the 138 — add on-demand from consumer requests
- [ ] Separate `:icons` Gradle module — only if JAR size becomes a measurable concern
- [ ] Bold/Light weight variants — only if aesthetic need is identified

### Anti-Features (Explicitly Never in v1.x)

- Brand/social logos — trademark and maintenance burden
- Multi-weight suffix in identifier names — wait until a second weight exists
- Custom icon registration API — consumers use `ImageVector` directly

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| 15 required migration icons | HIGH | LOW | P1 |
| All component text-glyph migrations | HIGH | MEDIUM | P1 |
| Material Icons replacements (AeroAlertKind/BannerKind) | HIGH | LOW | P1 |
| Full 138-icon set | MEDIUM | MEDIUM | P1 |
| `materialIconsExtended` removal | MEDIUM | LOW | P1 (after migration) |
| IconsSection in showcase | MEDIUM | MEDIUM | P1 |
| Filled/bold icon variants | LOW | HIGH | P3 |
| Separate `:icons` module | LOW | MEDIUM | P3 |

---

## Sources

- [dev778g-me/PhosphorIcon-compose — Kotlin Compose Multiplatform port, confirms PascalCase names including CaretDown, MagnifyingGlass, EyeSlash, SpeakerHigh, DotsThreeVertical, GearSix, TerminalWindow, FolderOpen, PaperPlane, ChatCircle, Envelope, WarningOctagon, Prohibit, FloppyDisk, PencilSimple, BookmarkSimple, TrashSimple, Funnel](https://github.com/dev778g-me/PhosphorIcon-compose) — HIGH confidence (official Kotlin port)
- [phosphor-icons/core — source SVG repository, file naming `{name}-regular.svg`, 1300+ icons](https://github.com/phosphor-icons/core) — HIGH confidence (official source)
- [iconbolt.com phosphor-regular listings — confirms arrow-square-out, lock-simple-open, dots-three-circle-vertical names](https://www.iconbolt.com/iconsets/phosphor-regular/arrow-square-out) — MEDIUM confidence (3rd party mirror)
- [Iconify ph/ collection — confirms wifi-high, wifi-slash, frame-corners, lock-key-open names](https://icon-sets.iconify.design/ph/) — MEDIUM confidence (authoritative icon registry mirror)
- [Phosphor React package — confirms Question component name, speaker-high/low/x/slash series, battery-full/low/empty/medium names, speaker-slash naming](https://github.com/phosphor-icons/react) — HIGH confidence (official React port)
- [phosphoricons.com — official icon browser confirming all names](https://phosphoricons.com/) — HIGH confidence
- [adamglin0/compose-phosphor-icon — secondary Kotlin port, confirms PhosphorIcons namespace structure](https://github.com/adamglin0/compose-phosphor-icon) — MEDIUM confidence
- [v0.app ph/speaker-slash — confirms speaker-slash name in Phosphor](https://www.v0.app/icon/ph/speaker-slash) — MEDIUM confidence

---

*Feature research for: AeroIcons icon set — aero-compose-ui v1.1 milestone (Phosphor Edition)*
*Researched: 2026-04-29*
*Replaces: 2026-04-28 version (Feather-based)*
