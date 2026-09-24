<#
.SYNOPSIS
    Capture comparison, noise extraction, before/after diffing and contact sheets for Phase 21
    baseline and verification captures (BASE-03/BASE-04/BASE-05, VER-07/VER-08).
.DESCRIPTION
    Dot-sources AeroCapture.ps1 for AeroCaptureNative/AeroPixels. PowerShell 5.1 only; any extra
    C# defined here stays C# 5 and does its pixel work in LockBits, never a per-pixel PowerShell
    loop. Cell-level (8px grid) merging and classification run in PowerShell because they operate
    over a small number of cells, not raw pixels.

    -Mode SelfTest    Synthetic fixture proof: identical bitmaps -> 0 diff / 0 rects; a 10x10
                       changed block -> exactly one rect containing it; classified insideNoise
                       when a noise region covers it, outsideNoise when a noise region does not.
    -Mode Noise       Groups captures by frame key across -RunDirs, compares every non-reference
                       capture against the first run's first capture, unions the touched 8px
                       cells into padded rects, and writes/merges them into -OutJson.
    -Mode Compare     Compares -BeforeDir's and -AfterDir's reference captures per key, classifies
                       every diff rect against -NoiseJson, writes a highlighted diff PNG per key
                       with a difference into -DiffDir, and a summary + per-key report to -OutJson.
    -Mode ContactSheet Builds one before|after|diff grid PNG per Component/Theme group (uitest) or
                       Theme/Section group (showcase) under -OutDir, from -Dirs <before>,<after>,<diff>.
#>

#Requires -Version 5.1
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('SelfTest', 'Noise', 'Compare', 'ContactSheet')]
    [string]$Mode,

    [string[]]$RunDirs,
    [ValidateSet('showcase', 'uitest')]
    [string]$Kind,
    [string]$OutJson,
    [switch]$Merge,

    [string]$BeforeDir,
    [string]$AfterDir,
    [string]$NoiseJson,
    [string]$DiffDir,

    [string[]]$Dirs,
    [string]$OutDir,

    [int]$CellSize = 8,
    [int]$PadPx = 2
)

Set-StrictMode -Version 2
$ErrorActionPreference = 'Stop'

# Some hosts pass a comma-separated -RunDirs/-Dirs value through as a single string instead of
# splitting it into array elements before binding; normalize either shape the same way as
# Invoke-ShowcaseSweep.ps1 does for -Themes/-Sections.
if ($RunDirs) { $RunDirs = @($RunDirs | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' }) }
if ($Dirs) { $Dirs = @($Dirs | ForEach-Object { $_ -split ',' } | Where-Object { $_ -ne '' }) }

$here = Split-Path -Parent $MyInvocation.MyCommand.Path
. (Join-Path $here 'AeroCapture.ps1')

if (-not ('AeroCompareNative' -as [type])) {
    Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;

public class AeroCompareNative
{
    // Renders `after` dimmed 50%, with every pixel that differs from `before` painted opaque
    // magenta, then outlines each outside-noise rect (green, 2px) on top.
    public static Bitmap MakeDiffImage(Bitmap before, Bitmap after, List<int[]> outsideRects)
    {
        int width = after.Width;
        int height = after.Height;
        Bitmap result = new Bitmap(width, height, PixelFormat.Format32bppArgb);

        BitmapData db = before.LockBits(new Rectangle(0, 0, width, height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        BitmapData da = after.LockBits(new Rectangle(0, 0, width, height), ImageLockMode.ReadOnly, PixelFormat.Format32bppArgb);
        BitmapData dr = result.LockBits(new Rectangle(0, 0, width, height), ImageLockMode.WriteOnly, PixelFormat.Format32bppArgb);
        try
        {
            int strideB = db.Stride;
            int strideA = da.Stride;
            int strideR = dr.Stride;
            byte[] bufB = new byte[strideB * height];
            byte[] bufA = new byte[strideA * height];
            byte[] bufR = new byte[strideR * height];
            Marshal.Copy(db.Scan0, bufB, 0, bufB.Length);
            Marshal.Copy(da.Scan0, bufA, 0, bufA.Length);

            for (int y = 0; y < height; y++)
            {
                int rowB = y * strideB;
                int rowA = y * strideA;
                int rowR = y * strideR;
                for (int x = 0; x < width; x++)
                {
                    int idxB = rowB + x * 4;
                    int idxA = rowA + x * 4;
                    int idxR = rowR + x * 4;
                    bool differs = bufB[idxB] != bufA[idxA] || bufB[idxB + 1] != bufA[idxA + 1] ||
                        bufB[idxB + 2] != bufA[idxA + 2] || bufB[idxB + 3] != bufA[idxA + 3];
                    if (differs)
                    {
                        bufR[idxR] = 255;     // B
                        bufR[idxR + 1] = 0;   // G
                        bufR[idxR + 2] = 255; // R  -> opaque magenta
                        bufR[idxR + 3] = 255; // A
                    }
                    else
                    {
                        bufR[idxR] = (byte)(bufA[idxA] / 2);
                        bufR[idxR + 1] = (byte)(bufA[idxA + 1] / 2);
                        bufR[idxR + 2] = (byte)(bufA[idxA + 2] / 2);
                        bufR[idxR + 3] = bufA[idxA + 3];
                    }
                }
            }
            Marshal.Copy(bufR, 0, dr.Scan0, bufR.Length);
        }
        finally
        {
            before.UnlockBits(db);
            after.UnlockBits(da);
            result.UnlockBits(dr);
        }

        if (outsideRects != null && outsideRects.Count > 0)
        {
            using (Graphics g = Graphics.FromImage(result))
            using (Pen pen = new Pen(Color.FromArgb(255, 0, 255, 0), 2))
            {
                foreach (int[] r in outsideRects)
                {
                    g.DrawRectangle(pen, r[0], r[1], r[2], r[3]);
                }
            }
        }

        return result;
    }
}
"@
}

function Merge-AeroDiffCellsToRegions {
    <#
    .SYNOPSIS
        Merges 8-connected touched cells (from AeroPixels.DiffCells) into padded, clamped
        bounding rects. Operates on the (typically small) cell list, never per-pixel.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][AllowEmptyCollection()][System.Collections.Generic.List[int[]]]$Cells,
        [Parameter(Mandatory = $true)][int]$Width,
        [Parameter(Mandatory = $true)][int]$Height,
        [int]$Cell = 8,
        [int]$Pad = 2
    )

    $regions = New-Object System.Collections.Generic.List[object]
    if ($Cells.Count -eq 0) {
        return , $regions
    }

    $map = @{}
    foreach ($c in $Cells) { $map["$($c[0]),$($c[1])"] = $c }
    $visited = New-Object 'System.Collections.Generic.HashSet[string]'

    foreach ($c in $Cells) {
        $key = "$($c[0]),$($c[1])"
        if ($visited.Contains($key)) { continue }

        $queue = New-Object System.Collections.Generic.Queue[object]
        $queue.Enqueue($c)
        [void]$visited.Add($key)

        $minCx = $c[0]; $maxCx = $c[0]; $minCy = $c[1]; $maxCy = $c[1]
        $totalPx = 0

        while ($queue.Count -gt 0) {
            $cur = $queue.Dequeue()
            $totalPx += $cur[2]
            if ($cur[0] -lt $minCx) { $minCx = $cur[0] }
            if ($cur[0] -gt $maxCx) { $maxCx = $cur[0] }
            if ($cur[1] -lt $minCy) { $minCy = $cur[1] }
            if ($cur[1] -gt $maxCy) { $maxCy = $cur[1] }

            for ($dy = -1; $dy -le 1; $dy++) {
                for ($dx = -1; $dx -le 1; $dx++) {
                    if ($dx -eq 0 -and $dy -eq 0) { continue }
                    $nk = "$($cur[0] + $dx),$($cur[1] + $dy)"
                    if ($map.ContainsKey($nk) -and -not $visited.Contains($nk)) {
                        [void]$visited.Add($nk)
                        $queue.Enqueue($map[$nk])
                    }
                }
            }
        }

        $x = [Math]::Max(0, ($minCx * $Cell) - $Pad)
        $y = [Math]::Max(0, ($minCy * $Cell) - $Pad)
        $rawRight = [Math]::Min($Width, (($maxCx + 1) * $Cell) + $Pad)
        $rawBottom = [Math]::Min($Height, (($maxCy + 1) * $Cell) + $Pad)
        $w = $rawRight - $x
        $h = $rawBottom - $y

        $regions.Add([pscustomobject]@{ x = $x; y = $y; w = $w; h = $h; px = $totalPx })
    }

    return , $regions
}

function Test-AeroRegionFullyInside {
    <#
    .SYNOPSIS
        True when $Region is fully contained in at least one of $NoiseRegions.
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)]$Region,
        [Parameter(Mandatory = $true)]$NoiseRegions
    )
    foreach ($n in $NoiseRegions) {
        if ($Region.x -ge $n.x -and $Region.y -ge $n.y -and
            ($Region.x + $Region.w) -le ($n.x + $n.w) -and
            ($Region.y + $Region.h) -le ($n.y + $n.h)) {
            return $true
        }
    }
    return $false
}

function Get-AeroFrameEntries {
    <#
    .SYNOPSIS
        Enumerates capture PNGs under a run directory and returns {Key, Capture, Path} for the
        given Kind.
    .DESCRIPTION
        showcase: `<RunDir>\<Theme>\<Section>-p<N>-c<K>.png` -> Key "<Theme>/<Section>/p<N>",
        Capture = <K>.
        uitest:   `<RunDir>\<Component>\<Theme>\<state>.png` -> Key "<Component>/<Theme>/<state>",
        Capture = 1 (one file per key per run).
    #>
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][string]$RunDir,
        [Parameter(Mandatory = $true)][ValidateSet('showcase', 'uitest')][string]$Kind
    )

    $entries = New-Object System.Collections.Generic.List[object]
    $root = (Resolve-Path -LiteralPath $RunDir).Path
    $files = Get-ChildItem -LiteralPath $root -Recurse -Filter '*.png' -File

    foreach ($f in $files) {
        $rel = $f.FullName.Substring($root.Length).TrimStart('\')
        $parts = $rel -split '\\'

        if ($Kind -eq 'showcase') {
            if ($parts.Count -ne 2) { continue }
            $theme = $parts[0]
            if ($parts[1] -notmatch '^(?<section>.+)-p(?<page>\d+)-c(?<capture>\d+)\.png$') { continue }
            $section = $Matches['section']
            $page = $Matches['page']
            $capture = [int]$Matches['capture']
            $key = "$theme/$section/p$page"
            $entries.Add([pscustomobject]@{ Key = $key; Capture = $capture; Path = $f.FullName })
        }
        else {
            if ($parts.Count -ne 3) { continue }
            $component = $parts[0]
            $theme = $parts[1]
            $state = [System.IO.Path]::GetFileNameWithoutExtension($parts[2])
            $key = "$component/$theme/$state"
            $entries.Add([pscustomobject]@{ Key = $key; Capture = 1; Path = $f.FullName })
        }
    }

    return , $entries
}

# ---------------------------------------------------------------------------------------------
# Mode: SelfTest
# ---------------------------------------------------------------------------------------------
if ($Mode -eq 'SelfTest') {
    $a = New-Object System.Drawing.Bitmap(64, 64, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $ga = [System.Drawing.Graphics]::FromImage($a)
    $ga.Clear([System.Drawing.Color]::White)
    $ga.Dispose()
    $aCopy = [System.Drawing.Bitmap]($a.Clone())

    # Fixture 1: identical bitmaps -> 0 diff px, 0 rects.
    $cellsIdentical = [AeroPixels]::DiffCells($a, $aCopy, $CellSize)
    $regionsIdentical = Merge-AeroDiffCellsToRegions -Cells $cellsIdentical -Width $a.Width -Height $a.Height -Cell $CellSize -Pad $PadPx
    $diffIdentical = [AeroPixels]::CountDiff($a, $aCopy)
    if ($diffIdentical -ne 0 -or $regionsIdentical.Count -ne 0) {
        throw "AERO_COMPARE_SELFTEST: expected 0 diff px and 0 rects for identical bitmaps, got diff=$diffIdentical rects=$($regionsIdentical.Count)"
    }

    # Fixture 2: a 10x10 block changed at (20,20) -> exactly one rect containing it.
    $b = [System.Drawing.Bitmap]($a.Clone())
    $gb = [System.Drawing.Graphics]::FromImage($b)
    $redBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 255, 0, 0))
    $gb.FillRectangle($redBrush, 20, 20, 10, 10)
    $gb.Dispose()
    $redBrush.Dispose()

    $cellsBlock = [AeroPixels]::DiffCells($a, $b, $CellSize)
    $regionsBlock = Merge-AeroDiffCellsToRegions -Cells $cellsBlock -Width $a.Width -Height $a.Height -Cell $CellSize -Pad $PadPx
    if ($regionsBlock.Count -ne 1) {
        throw "AERO_COMPARE_SELFTEST: expected exactly one rect for the 10x10 block, got $($regionsBlock.Count)"
    }
    $r = $regionsBlock[0]
    $containsBlock = ($r.x -le 20) -and ($r.y -le 20) -and (($r.x + $r.w) -ge 30) -and (($r.y + $r.h) -ge 30)
    if (-not $containsBlock) {
        throw "AERO_COMPARE_SELFTEST: rect ($($r.x),$($r.y),$($r.w),$($r.h)) does not contain (20,20,10,10)"
    }

    # Fixture 3: a noise region covering the rect -> insideNoise.
    $coveringNoise = @([pscustomobject]@{ x = 10; y = 10; w = 30; h = 30 })
    if (-not (Test-AeroRegionFullyInside -Region $r -NoiseRegions $coveringNoise)) {
        throw "AERO_COMPARE_SELFTEST: expected the block rect to classify inside a covering noise region"
    }

    # Fixture 4: a noise region elsewhere -> outsideNoise.
    $elsewhereNoise = @([pscustomobject]@{ x = 0; y = 0; w = 5; h = 5 })
    if (Test-AeroRegionFullyInside -Region $r -NoiseRegions $elsewhereNoise) {
        throw "AERO_COMPARE_SELFTEST: expected the block rect to classify outside an unrelated noise region"
    }

    $a.Dispose()
    $aCopy.Dispose()
    $b.Dispose()

    Write-Output 'AERO_COMPARE_SELFTEST PASS'
    return
}

# ---------------------------------------------------------------------------------------------
# Mode: Noise
# ---------------------------------------------------------------------------------------------
if ($Mode -eq 'Noise') {
    if (-not $RunDirs -or $RunDirs.Count -lt 2) { throw 'Noise mode requires -RunDirs with at least 2 run directories' }
    if (-not $Kind) { throw 'Noise mode requires -Kind' }
    if (-not $OutJson) { throw 'Noise mode requires -OutJson' }

    $allEntries = New-Object System.Collections.Generic.List[object]
    for ($i = 0; $i -lt $RunDirs.Count; $i++) {
        $runEntries = Get-AeroFrameEntries -RunDir $RunDirs[$i] -Kind $Kind
        foreach ($e in $runEntries) {
            $allEntries.Add([pscustomobject]@{ Key = $e.Key; RunIndex = $i; Capture = $e.Capture; Path = $e.Path })
        }
    }

    $grouped = $allEntries | Group-Object -Property Key
    $result = [ordered]@{}

    foreach ($g in $grouped) {
        $sorted = @($g.Group | Sort-Object RunIndex, Capture)
        $reference = $sorted[0]
        $others = @($sorted | Select-Object -Skip 1)

        $refBitmap = New-Object System.Drawing.Bitmap($reference.Path)
        $touchedCells = @{}
        $dimensionMismatch = 0

        foreach ($o in $others) {
            $otherBitmap = New-Object System.Drawing.Bitmap($o.Path)
            if ($otherBitmap.Width -ne $refBitmap.Width -or $otherBitmap.Height -ne $refBitmap.Height) {
                $dimensionMismatch++
                $otherBitmap.Dispose()
                continue
            }
            $cells = [AeroPixels]::DiffCells($refBitmap, $otherBitmap, $CellSize)
            foreach ($c in $cells) {
                $ck = "$($c[0]),$($c[1])"
                if ($touchedCells.ContainsKey($ck)) {
                    $existing = $touchedCells[$ck]
                    $touchedCells[$ck] = @($existing[0], $existing[1], ($existing[2] + $c[2]))
                }
                else {
                    $touchedCells[$ck] = $c
                }
            }
            $otherBitmap.Dispose()
        }

        $cellList = New-Object System.Collections.Generic.List[int[]]
        foreach ($v in $touchedCells.Values) { $cellList.Add($v) }
        $regions = Merge-AeroDiffCellsToRegions -Cells $cellList -Width $refBitmap.Width -Height $refBitmap.Height -Cell $CellSize -Pad $PadPx
        $refBitmap.Dispose()

        $result[$g.Name] = [ordered]@{
            captures          = $sorted.Count
            stable            = ($regions.Count -eq 0 -and $dimensionMismatch -eq 0)
            dimensionMismatch = $dimensionMismatch
            regions           = @($regions | ForEach-Object { [ordered]@{ x = $_.x; y = $_.y; w = $_.w; h = $_.h; px = $_.px } })
        }
    }

    $existing = [ordered]@{}
    if ($Merge -and (Test-Path -LiteralPath $OutJson)) {
        $raw = Get-Content -LiteralPath $OutJson -Raw | ConvertFrom-Json
        foreach ($prop in $raw.PSObject.Properties) {
            $existing[$prop.Name] = $prop.Value
        }
    }
    $existing[$Kind] = $result

    $directory = Split-Path -Path $OutJson -Parent
    if ($directory -and -not (Test-Path -LiteralPath $directory)) { New-Item -ItemType Directory -Path $directory -Force | Out-Null }
    $existing | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $OutJson -Encoding UTF8

    Write-Output "AERO_COMPARE_NOISE_DONE kind=$Kind keys=$($result.Count)"
    return
}

# ---------------------------------------------------------------------------------------------
# Mode: Compare
# ---------------------------------------------------------------------------------------------
if ($Mode -eq 'Compare') {
    if (-not $BeforeDir -or -not $AfterDir -or -not $Kind -or -not $NoiseJson -or -not $DiffDir -or -not $OutJson) {
        throw 'Compare mode requires -BeforeDir -AfterDir -Kind -NoiseJson -DiffDir -OutJson'
    }

    $noiseData = @{}
    if (Test-Path -LiteralPath $NoiseJson) {
        $raw = Get-Content -LiteralPath $NoiseJson -Raw | ConvertFrom-Json
        if ($raw.PSObject.Properties.Name -contains $Kind) {
            foreach ($prop in $raw.$Kind.PSObject.Properties) {
                $noiseData[$prop.Name] = @($prop.Value.regions | ForEach-Object { [pscustomobject]@{ x = $_.x; y = $_.y; w = $_.w; h = $_.h } })
            }
        }
    }

    $beforeEntries = New-Object System.Collections.Generic.List[object]
    foreach ($e in (Get-AeroFrameEntries -RunDir $BeforeDir -Kind $Kind)) { if ($e.Capture -eq 1) { $beforeEntries.Add($e) } }
    $afterEntries = New-Object System.Collections.Generic.List[object]
    foreach ($e in (Get-AeroFrameEntries -RunDir $AfterDir -Kind $Kind)) { if ($e.Capture -eq 1) { $afterEntries.Add($e) } }

    $beforeMap = @{}
    foreach ($e in $beforeEntries) { $beforeMap[$e.Key] = $e.Path }
    $afterMap = @{}
    foreach ($e in $afterEntries) { $afterMap[$e.Key] = $e.Path }

    $allKeys = @(@($beforeMap.Keys) + @($afterMap.Keys) | Select-Object -Unique | Sort-Object)

    if (-not (Test-Path -LiteralPath $DiffDir)) { New-Item -ItemType Directory -Path $DiffDir -Force | Out-Null }

    $perKey = [ordered]@{}
    $summary = [ordered]@{ keys = 0; identical = 0; insideOnly = 0; withOutside = 0; missing = 0 }

    foreach ($key in $allKeys) {
        $summary.keys++

        if (-not $beforeMap.ContainsKey($key)) {
            $perKey[$key] = [ordered]@{ status = 'missingBefore' }
            $summary.missing++
            continue
        }
        if (-not $afterMap.ContainsKey($key)) {
            $perKey[$key] = [ordered]@{ status = 'missingAfter' }
            $summary.missing++
            continue
        }

        $beforeBmp = New-Object System.Drawing.Bitmap($beforeMap[$key])
        $afterBmp = New-Object System.Drawing.Bitmap($afterMap[$key])

        if ($beforeBmp.Width -ne $afterBmp.Width -or $beforeBmp.Height -ne $afterBmp.Height) {
            $perKey[$key] = [ordered]@{ status = 'dimensionMismatch' }
            $summary.missing++
            $beforeBmp.Dispose()
            $afterBmp.Dispose()
            continue
        }

        $diffPx = [AeroPixels]::CountDiff($beforeBmp, $afterBmp)
        $maxDelta = [AeroPixels]::MaxChannelDelta($beforeBmp, $afterBmp)
        $totalPx = $beforeBmp.Width * $beforeBmp.Height
        $pct = if ($totalPx -gt 0) { [Math]::Round(100.0 * $diffPx / $totalPx, 4) } else { 0 }

        $cells = [AeroPixels]::DiffCells($beforeBmp, $afterBmp, $CellSize)
        $regions = Merge-AeroDiffCellsToRegions -Cells $cells -Width $beforeBmp.Width -Height $beforeBmp.Height -Cell $CellSize -Pad $PadPx

        $keyNoise = @(if ($noiseData.ContainsKey($key)) { $noiseData[$key] } else { @() })
        $classified = New-Object System.Collections.Generic.List[object]
        $outsideRects = New-Object System.Collections.Generic.List[int[]]
        $anyOutside = $false

        foreach ($rg in $regions) {
            $isInside = ($keyNoise.Count -gt 0) -and (Test-AeroRegionFullyInside -Region $rg -NoiseRegions $keyNoise)
            $classified.Add([ordered]@{ x = $rg.x; y = $rg.y; w = $rg.w; h = $rg.h; px = $rg.px; classification = if ($isInside) { 'insideNoise' } else { 'outsideNoise' } })
            if (-not $isInside) {
                $anyOutside = $true
                $outsideRects.Add(@($rg.x, $rg.y, $rg.w, $rg.h))
            }
        }

        if ($diffPx -eq 0) { $summary.identical++ }
        elseif ($anyOutside) { $summary.withOutside++ }
        else { $summary.insideOnly++ }

        if ($diffPx -gt 0) {
            $relPath = ($key -replace '/', '\') + '.png'
            $diffFile = Join-Path $DiffDir $relPath
            $diffFileDir = Split-Path -Path $diffFile -Parent
            if ($diffFileDir -and -not (Test-Path -LiteralPath $diffFileDir)) { New-Item -ItemType Directory -Path $diffFileDir -Force | Out-Null }
            $diffBitmap = [AeroCompareNative]::MakeDiffImage($beforeBmp, $afterBmp, $outsideRects)
            $diffBitmap.Save($diffFile, [System.Drawing.Imaging.ImageFormat]::Png)
            $diffBitmap.Dispose()
        }

        $perKey[$key] = [ordered]@{
            status          = 'compared'
            diffPx          = $diffPx
            pct             = $pct
            maxChannelDelta = $maxDelta
            regions         = $classified
        }

        $beforeBmp.Dispose()
        $afterBmp.Dispose()
    }

    $output = [ordered]@{ summary = $summary; keys = $perKey }
    $directory = Split-Path -Path $OutJson -Parent
    if ($directory -and -not (Test-Path -LiteralPath $directory)) { New-Item -ItemType Directory -Path $directory -Force | Out-Null }
    $output | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $OutJson -Encoding UTF8

    Write-Output "AERO_COMPARE_DONE kind=$Kind keys=$($summary.keys) identical=$($summary.identical) insideOnly=$($summary.insideOnly) withOutside=$($summary.withOutside) missing=$($summary.missing)"
    return
}

# ---------------------------------------------------------------------------------------------
# Mode: ContactSheet
# ---------------------------------------------------------------------------------------------
if ($Mode -eq 'ContactSheet') {
    if (-not $Dirs -or $Dirs.Count -lt 3) { throw 'ContactSheet mode requires -Dirs <before>,<after>,<diff>' }
    if (-not $Kind) { throw 'ContactSheet mode requires -Kind' }
    if (-not $OutDir) { throw 'ContactSheet mode requires -OutDir' }

    $beforeDir = $Dirs[0]
    $afterDir = $Dirs[1]
    $diffDir = $Dirs[2]

    $beforeEntries = New-Object System.Collections.Generic.List[object]
    foreach ($e in (Get-AeroFrameEntries -RunDir $beforeDir -Kind $Kind)) { if ($e.Capture -eq 1) { $beforeEntries.Add($e) } }

    $groups = @{}
    foreach ($e in $beforeEntries) {
        $segs = $e.Key -split '/'
        if ($segs.Count -lt 3) { continue }
        $groupKey = "$($segs[0])/$($segs[1])"
        $state = ($segs[2..($segs.Count - 1)] -join '/')
        if (-not $groups.ContainsKey($groupKey)) { $groups[$groupKey] = New-Object System.Collections.Generic.List[object] }
        $groups[$groupKey].Add([pscustomobject]@{ State = $state; Key = $e.Key; Path = $e.Path })
    }

    if (-not (Test-Path -LiteralPath $OutDir)) { New-Item -ItemType Directory -Path $OutDir -Force | Out-Null }

    foreach ($groupKey in $groups.Keys) {
        $rows = @($groups[$groupKey] | Sort-Object State)
        $cellW = 220
        $cellH = 180
        $labelH = 24
        $sheetW = $cellW * 3
        $sheetH = ($cellH + $labelH) * $rows.Count + $labelH

        $sheet = New-Object System.Drawing.Bitmap($sheetW, $sheetH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($sheet)
        $g.Clear([System.Drawing.Color]::White)
        $font = New-Object System.Drawing.Font('Segoe UI', 10)
        $brush = [System.Drawing.Brushes]::Black
        $g.DrawString('before', $font, $brush, ($cellW * 0) + 4, 2)
        $g.DrawString('after', $font, $brush, ($cellW * 1) + 4, 2)
        $g.DrawString('diff', $font, $brush, ($cellW * 2) + 4, 2)

        $rowIndex = 0
        foreach ($row in $rows) {
            $yTop = $labelH + $rowIndex * ($cellH + $labelH)
            $g.DrawString($row.State, $font, $brush, 4, $yTop)

            $relKey = ($row.Key -replace '/', '\')
            $beforePath = $row.Path
            $afterPath = Join-Path $afterDir ($relKey + '.png')
            $diffPath = Join-Path $diffDir ($relKey + '.png')

            $cols = @($beforePath, $afterPath, $diffPath)
            for ($c = 0; $c -lt 3; $c++) {
                $p = $cols[$c]
                if (Test-Path -LiteralPath $p) {
                    $img = [System.Drawing.Image]::FromFile($p)
                    $destRect = New-Object System.Drawing.Rectangle(($c * $cellW), ($yTop + $labelH), $cellW, $cellH)
                    $g.DrawImage($img, $destRect)
                    $img.Dispose()
                }
            }
            $rowIndex++
        }

        $g.Dispose()
        $font.Dispose()
        $outFile = Join-Path $OutDir (($groupKey -replace '/', '_') + '.png')
        $sheet.Save($outFile, [System.Drawing.Imaging.ImageFormat]::Png)
        $sheet.Dispose()
    }

    Write-Output "AERO_CONTACTSHEET_DONE kind=$Kind groups=$($groups.Count) out=$OutDir"
    return
}
