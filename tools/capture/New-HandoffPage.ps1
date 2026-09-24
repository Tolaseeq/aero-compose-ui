<#
.SYNOPSIS
    Builds the offline D-06 hand-off page: before/after/highlight rows for every showcase key,
    one contact-sheet block per UI-test component x theme, the after-only MCP captures, and the
    Plan 11 drift/unconfirmed lists, all in one local HTML file with relative image paths.
.DESCRIPTION
    PowerShell 5.1 only. Reads `diff\compare-showcase.json` and `diff\compare-uitest.json` under
    -CapturesRoot (written by `Compare-AeroCaptures.ps1 -Mode Compare`) to find every captured key
    and its outside-noise status, locates the before/after run directories that actually hold a
    complete set of reference captures for that key set, and writes
    `<CapturesRoot>\handoff\index.html`. Every `src` is a path relative to that file
    (`../old-kt.../...`, `../new-kt.../...`, `../diff/...`, `../sheets/...`) so the page opens and
    renders fully offline, with no absolute `C:` path anywhere. All embedded text (drift/unconfirmed
    list contents) is HTML-escaped. A self-check re-parses every `src` in the generated HTML and
    verifies the file exists before printing `AERO_HANDOFF OK <n images>`.
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$CapturesRoot,
    [Parameter(Mandatory = $true)][string]$DriftMd,
    [Parameter(Mandatory = $true)][string]$UnconfirmedMd
)

Set-StrictMode -Version 2
$ErrorActionPreference = 'Stop'

$CapturesRoot = (Resolve-Path -LiteralPath $CapturesRoot).Path
$handoffDir = Join-Path $CapturesRoot 'handoff'
if (-not (Test-Path -LiteralPath $handoffDir)) { New-Item -ItemType Directory -Path $handoffDir -Force | Out-Null }

function HtmlEscape([string]$text) {
    if ($null -eq $text) { return '' }
    return [System.Net.WebUtility]::HtmlEncode($text)
}

function ToRelSrc([string]$absPath) {
    # $absPath is an absolute path under $CapturesRoot; return a forward-slash path relative to
    # <CapturesRoot>\handoff\index.html (one level down from $CapturesRoot).
    $rel = $absPath.Substring($CapturesRoot.Length).TrimStart('\', '/')
    return '../' + ($rel -replace '\\', '/')
}

# -------------------------------------------------------------------------------------------
# Locate the before/after reference run directory for a Kind: the one directory under
# <root>\<kind>\ whose files cover every key in the compare JSON (showcase: <Theme>\<Section>
# -p<N>-c1.png; uitest: <Component>\<Theme>\<state>.png). Multiple directories can be complete
# (e.g. old-toolchain runA/runB, new-toolchain uitest runA/runB); "runA" is preferred when it is
# one of the complete candidates (it is the documented reference capture throughout Phase 21),
# otherwise the alphabetically first complete candidate is used.
# -------------------------------------------------------------------------------------------
function Get-KeyRelativePath([string]$key, [string]$kind) {
    $parts = $key -split '/'
    if ($kind -eq 'showcase') {
        $theme = $parts[0]; $section = $parts[1]; $page = $parts[2] -replace '^p', ''
        return Join-Path $theme "$section-p$page-c1.png"
    }
    else {
        $comp = $parts[0]; $theme = $parts[1]; $state = $parts[2]
        return Join-Path (Join-Path $comp $theme) "$state.png"
    }
}

function Find-CompleteRunDir([string]$toolchainRoot, [string]$kind, [string[]]$keys) {
    $kindRoot = Join-Path $toolchainRoot $kind
    if (-not (Test-Path -LiteralPath $kindRoot)) { return $null }
    $candidates = @(Get-ChildItem -LiteralPath $kindRoot -Directory | Sort-Object Name)
    $complete = New-Object System.Collections.Generic.List[string]
    foreach ($c in $candidates) {
        $ok = $true
        foreach ($k in $keys) {
            $p = Join-Path $c.FullName (Get-KeyRelativePath $k $kind)
            if (-not (Test-Path -LiteralPath $p)) { $ok = $false; break }
        }
        if ($ok) { $complete.Add($c.Name) }
    }
    if ($complete.Count -eq 0) { return $null }
    if ($complete.Contains('runA')) { return Join-Path $kindRoot 'runA' }
    return Join-Path $kindRoot $complete[0]
}

$oldRoot = Get-ChildItem -LiteralPath $CapturesRoot -Directory -Filter 'old-kt*' | Select-Object -First 1
$newRoot = Get-ChildItem -LiteralPath $CapturesRoot -Directory -Filter 'new-kt*' | Select-Object -First 1
if (-not $oldRoot -or -not $newRoot) { throw 'AERO_HANDOFF: could not find old-kt*/new-kt* toolchain directories under -CapturesRoot' }

$diffDir = Join-Path $CapturesRoot 'diff'
$scJsonPath = Join-Path $diffDir 'compare-showcase.json'
$utJsonPath = Join-Path $diffDir 'compare-uitest.json'
if (-not (Test-Path -LiteralPath $scJsonPath)) { throw "AERO_HANDOFF: missing $scJsonPath" }
if (-not (Test-Path -LiteralPath $utJsonPath)) { throw "AERO_HANDOFF: missing $utJsonPath" }

$scJson = Get-Content -LiteralPath $scJsonPath -Raw | ConvertFrom-Json
$utJson = Get-Content -LiteralPath $utJsonPath -Raw | ConvertFrom-Json

$scKeys = @($scJson.keys.PSObject.Properties.Name | Sort-Object)
$utKeys = @($utJson.keys.PSObject.Properties.Name | Sort-Object)

$scBeforeDir = Find-CompleteRunDir $oldRoot.FullName 'showcase' $scKeys
$scAfterDir = Find-CompleteRunDir $newRoot.FullName 'showcase' $scKeys
$utBeforeDir = Find-CompleteRunDir $oldRoot.FullName 'ui-tests' $utKeys
$utAfterDir = Find-CompleteRunDir $newRoot.FullName 'ui-tests' $utKeys
if (-not $scBeforeDir -or -not $scAfterDir) { throw 'AERO_HANDOFF: could not find a complete before/after showcase run directory' }
if (-not $utBeforeDir -or -not $utAfterDir) { throw 'AERO_HANDOFF: could not find a complete before/after ui-tests run directory' }

function Test-KeyHasOutside($entry) {
    if (-not $entry.regions) { return $false }
    foreach ($r in $entry.regions) { if ($r.classification -eq 'outsideNoise') { return $true } }
    return $false
}

# -------------------------------------------------------------------------------------------
# Showcase rows: one per key, before | after | highlight, outside-noise rows first and badged.
# -------------------------------------------------------------------------------------------
$scRows = New-Object System.Collections.Generic.List[object]
foreach ($key in $scKeys) {
    $entry = $scJson.keys.$key
    $hasOutside = Test-KeyHasOutside $entry
    $relKeyPath = Get-KeyRelativePath $key 'showcase'
    $beforeAbs = Join-Path $scBeforeDir $relKeyPath
    $afterAbs = Join-Path $scAfterDir $relKeyPath
    $hlAbs = Join-Path $diffDir (Join-Path 'showcase' (($key -replace '/', '\') + '.png'))
    $scRows.Add([pscustomobject]@{
            Key        = $key
            HasOutside = $hasOutside
            DiffPx     = if ($entry.PSObject.Properties.Name -contains 'diffPx') { $entry.diffPx } else { 0 }
            Before     = $beforeAbs
            After      = $afterAbs
            Highlight  = $hlAbs
        })
}
$scRows = @($scRows | Sort-Object @{Expression = { -not $_.HasOutside } }, Key)

# -------------------------------------------------------------------------------------------
# UI-test contact-sheet blocks: one per Component/Theme group, badged when any of its states
# carries an outside-noise diff.
# -------------------------------------------------------------------------------------------
$sheetsDir = Join-Path $CapturesRoot 'sheets\ui-tests'
$utGroupOutside = @{}
foreach ($key in $utKeys) {
    $parts = $key -split '/'
    $groupKey = "$($parts[0])_$($parts[1])"
    if (Test-KeyHasOutside $utJson.keys.$key) { $utGroupOutside[$groupKey] = $true }
}
$sheetFiles = @()
if (Test-Path -LiteralPath $sheetsDir) {
    $sheetFiles = @(Get-ChildItem -LiteralPath $sheetsDir -Filter '*.png' | Sort-Object Name)
}
$utBlocks = New-Object System.Collections.Generic.List[object]
foreach ($f in $sheetFiles) {
    $groupKey = [System.IO.Path]::GetFileNameWithoutExtension($f.Name)
    $hasOutside = $utGroupOutside.ContainsKey($groupKey)
    $utBlocks.Add([pscustomobject]@{ GroupKey = $groupKey; HasOutside = $hasOutside; Sheet = $f.FullName })
}
$utBlocks = @($utBlocks | Sort-Object @{Expression = { -not $_.HasOutside } }, GroupKey)

# -------------------------------------------------------------------------------------------
# After-only MCP captures (D-08): every <Component>\<Theme>\opened.png under mcp-after-only.
# -------------------------------------------------------------------------------------------
$mcpDir = Join-Path $newRoot.FullName 'mcp-after-only'
$mcpShots = New-Object System.Collections.Generic.List[object]
if (Test-Path -LiteralPath $mcpDir) {
    foreach ($compDir in (Get-ChildItem -LiteralPath $mcpDir -Directory | Sort-Object Name)) {
        foreach ($themeDir in (Get-ChildItem -LiteralPath $compDir.FullName -Directory | Sort-Object Name)) {
            $img = Join-Path $themeDir.FullName 'opened.png'
            if (Test-Path -LiteralPath $img) {
                $mcpShots.Add([pscustomobject]@{ Component = $compDir.Name; Theme = $themeDir.Name; Path = $img })
            }
        }
    }
}

# -------------------------------------------------------------------------------------------
# Drift / unconfirmed list text, embedded verbatim (HTML-escaped) in <pre> blocks.
# -------------------------------------------------------------------------------------------
$driftText = Get-Content -LiteralPath $DriftMd -Raw
$unconfirmedText = Get-Content -LiteralPath $UnconfirmedMd -Raw

# -------------------------------------------------------------------------------------------
# Render HTML.
# -------------------------------------------------------------------------------------------
$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine('<!DOCTYPE html>')
[void]$sb.AppendLine('<html lang="en">')
[void]$sb.AppendLine('<head>')
[void]$sb.AppendLine('<meta charset="utf-8">')
[void]$sb.AppendLine('<title>Phase 21 hand-off: post-upgrade visual comparison</title>')
[void]$sb.AppendLine('<style>')
[void]$sb.AppendLine('body { font-family: Segoe UI, sans-serif; margin: 16px; background: #1e1e1e; color: #ddd; }')
[void]$sb.AppendLine('h1, h2 { color: #fff; }')
[void]$sb.AppendLine('.row { display: flex; gap: 8px; align-items: flex-start; margin-bottom: 6px; padding: 8px; background: #2a2a2a; border-radius: 4px; }')
[void]$sb.AppendLine('.row.badged { outline: 2px solid #ff5555; }')
[void]$sb.AppendLine('.cell { text-align: center; }')
[void]$sb.AppendLine('.cell img { max-width: 260px; max-height: 200px; border: 1px solid #555; background: #000; }')
[void]$sb.AppendLine('.cell .label { font-size: 11px; color: #aaa; }')
[void]$sb.AppendLine('.key { font-family: Consolas, monospace; font-size: 12px; min-width: 220px; }')
[void]$sb.AppendLine('.badge { display: inline-block; background: #ff5555; color: #fff; padding: 1px 6px; border-radius: 3px; font-size: 10px; margin-left: 6px; }')
[void]$sb.AppendLine('pre { background: #111; color: #ddd; padding: 12px; overflow-x: auto; white-space: pre-wrap; }')
[void]$sb.AppendLine('</style>')
[void]$sb.AppendLine('</head>')
[void]$sb.AppendLine('<body>')
[void]$sb.AppendLine('<h1>Phase 21: post-upgrade visual comparison (D-06 hand-off)</h1>')
[void]$sb.AppendLine("<p>Showcase: $($scRows.Count) keys ($((@($scRows | Where-Object { $_.HasOutside })).Count) with an outside-noise difference, shown first). UI tests: $($utBlocks.Count) component x theme contact sheets ($((@($utBlocks | Where-Object { $_.HasOutside })).Count) with an outside-noise difference, shown first).</p>")

[void]$sb.AppendLine('<h2>Showcase: before / after / highlight, per Theme/Section/Page</h2>')
foreach ($r in $scRows) {
    $rowClass = if ($r.HasOutside) { 'row badged' } else { 'row' }
    $badge = if ($r.HasOutside) { '<span class="badge">DIFFERS</span>' } else { '' }
    [void]$sb.AppendLine("<div class=`"$rowClass`">")
    [void]$sb.AppendLine("<div class=`"key`">$(HtmlEscape $r.Key)$badge<div class=`"label`">diffPx=$($r.DiffPx)</div></div>")
    [void]$sb.AppendLine("<div class=`"cell`"><img src=`"$(HtmlEscape (ToRelSrc $r.Before))`" loading=`"lazy`"><div class=`"label`">before</div></div>")
    [void]$sb.AppendLine("<div class=`"cell`"><img src=`"$(HtmlEscape (ToRelSrc $r.After))`" loading=`"lazy`"><div class=`"label`">after</div></div>")
    if (Test-Path -LiteralPath $r.Highlight) {
        [void]$sb.AppendLine("<div class=`"cell`"><img src=`"$(HtmlEscape (ToRelSrc $r.Highlight))`" loading=`"lazy`"><div class=`"label`">highlight</div></div>")
    }
    [void]$sb.AppendLine('</div>')
}

[void]$sb.AppendLine('<h2>UI tests: contact sheets (before | after | diff per state), per Component/Theme</h2>')
foreach ($b in $utBlocks) {
    $rowClass = if ($b.HasOutside) { 'row badged' } else { 'row' }
    $badge = if ($b.HasOutside) { '<span class="badge">DIFFERS</span>' } else { '' }
    [void]$sb.AppendLine("<div class=`"$rowClass`">")
    [void]$sb.AppendLine("<div class=`"key`">$(HtmlEscape $b.GroupKey)$badge</div>")
    [void]$sb.AppendLine("<div class=`"cell`"><img src=`"$(HtmlEscape (ToRelSrc $b.Sheet))`" style=`"max-width:660px;max-height:none;`" loading=`"lazy`"></div>")
    [void]$sb.AppendLine('</div>')
}

[void]$sb.AppendLine('<h2>After-only MCP inspection (D-08, no baseline): AeroDialog / AeroAlertDialog</h2>')
foreach ($m in $mcpShots) {
    [void]$sb.AppendLine('<div class="row">')
    [void]$sb.AppendLine("<div class=`"key`">$(HtmlEscape $m.Component) / $(HtmlEscape $m.Theme)</div>")
    [void]$sb.AppendLine("<div class=`"cell`"><img src=`"$(HtmlEscape (ToRelSrc $m.Path))`" loading=`"lazy`"><div class=`"label`">opened (no before)</div></div>")
    [void]$sb.AppendLine('</div>')
}

[void]$sb.AppendLine('<h2>Drift list (21-DRIFT.md)</h2>')
[void]$sb.AppendLine("<pre>$(HtmlEscape $driftText)</pre>")
[void]$sb.AppendLine('<h2>Unconfirmed list (21-UNCONFIRMED.md)</h2>')
[void]$sb.AppendLine("<pre>$(HtmlEscape $unconfirmedText)</pre>")

[void]$sb.AppendLine('</body>')
[void]$sb.AppendLine('</html>')

$indexPath = Join-Path $handoffDir 'index.html'
$sb.ToString() | Set-Content -LiteralPath $indexPath -Encoding UTF8

# -------------------------------------------------------------------------------------------
# Self-check: every src="..." in the generated HTML must resolve to a real file relative to
# the handoff directory.
# -------------------------------------------------------------------------------------------
$html = Get-Content -LiteralPath $indexPath -Raw
$srcMatches = [System.Text.RegularExpressions.Regex]::Matches($html, 'src="([^"]+)"')
$missing = New-Object System.Collections.Generic.List[string]
$checked = 0
foreach ($m in $srcMatches) {
    $src = [System.Net.WebUtility]::HtmlDecode($m.Groups[1].Value)
    $resolved = Join-Path $handoffDir ($src -replace '/', '\')
    $checked++
    if (-not (Test-Path -LiteralPath $resolved)) { $missing.Add($src) }
}
if ($missing.Count -gt 0) {
    throw "AERO_HANDOFF: $($missing.Count) of $checked image(s) do not resolve, e.g. $($missing[0])"
}

Write-Output "AERO_HANDOFF OK $checked images"
