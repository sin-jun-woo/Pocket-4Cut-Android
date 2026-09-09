# Pocket 4Cut E2E smoke test via adb (device must be unlocked)
param(
    [string]$Serial = "",
    [switch]$FreshStart,
    [string]$AdbPath = ""
)

if ($AdbPath) {
    if (-not (Test-Path -LiteralPath $AdbPath -PathType Leaf)) {
        throw "The supplied -AdbPath does not point to an executable file."
    }
    $adb = (Resolve-Path -LiteralPath $AdbPath).Path
} else {
    $adb = $null
    foreach ($sdkRoot in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)) {
        if (-not $sdkRoot) { continue }
        $candidate = Join-Path $sdkRoot "platform-tools/adb.exe"
        if (Test-Path -LiteralPath $candidate -PathType Leaf) {
            $adb = $candidate
            break
        }
    }
    if (-not $adb) {
        $adbCommand = Get-Command adb -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($adbCommand) { $adb = $adbCommand.Source }
    }
    if (-not $adb) {
        throw "ADB was not found. Set ANDROID_HOME or ANDROID_SDK_ROOT, add adb to PATH, or pass -AdbPath."
    }
}
# Without -Serial, adb requires a single connected device.
$adbArgs = if ($Serial) { @("-s", $Serial) } else { @() }

function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$AdbCommand)
    & $adb @adbArgs @AdbCommand 2>&1
}

function Tap([int]$x, [int]$y) {
    Invoke-Adb shell input tap $x $y | Out-Null
    Start-Sleep -Milliseconds 400
}

function Get-Clickables {
    Invoke-Adb shell uiautomator dump /sdcard/p4c_e2e.xml | Out-Null
    $xml = (Invoke-Adb shell cat /sdcard/p4c_e2e.xml | Out-String)
    [regex]::Matches($xml, 'package="com.pocket4cut"[^>]*clickable="true"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"') | ForEach-Object {
        [PSCustomObject]@{
            X = [int](([int]$_.Groups[1].Value + [int]$_.Groups[3].Value) / 2)
            Y = [int](([int]$_.Groups[2].Value + [int]$_.Groups[4].Value) / 2)
        }
    }
}

function Tap-BottomCta {
    param([int]$MinY = 1500)
    $pts = Get-Clickables
    if (-not $pts) {
        Write-Host "  fallback tap 540,2400"
        Tap 540 2400
        return $true
    }
    $cta = $pts | Where-Object { $_.X -gt 200 -and $_.X -lt 880 -and $_.Y -ge $MinY } | Sort-Object Y -Descending | Select-Object -First 1
    if (-not $cta) {
        Write-Host "  fallback tap 540,2400"
        Tap 540 2400
        return $true
    }
    Write-Host "  tap bottom CTA $($cta.X),$($cta.Y)"
    Tap $cta.X $cta.Y
    return $true
}

function Wait-CaptureDone {
    Write-Host ">> waiting capture to finish..."
    for ($i = 0; $i -lt 90; $i++) {
        Start-Sleep -Seconds 1
        $pts = @(Get-Clickables)
        $sideControls = @($pts | Where-Object { $_.X -gt 850 }).Count
        $gridLike = @($pts | Where-Object { $_.Y -gt 600 -and $_.Y -lt 2100 -and $_.X -lt 850 }).Count
        if ($sideControls -eq 0 -and $gridLike -ge 2) {
            Write-Host "  capture done (grid=$gridLike)"
            return
        }
    }
    Write-Host "  capture wait timeout, continuing anyway"
}

function Tap-GridFirstN([int]$n) {
    $pts = Get-Clickables | Where-Object { $_.Y -gt 500 -and $_.Y -lt 2200 } | Sort-Object Y, X
    $i = 0
    foreach ($p in $pts) {
        if ($i -ge $n) { break }
        Write-Host "  tap grid #$i $($p.X),$($p.Y)"
        Tap $p.X $p.Y
        $i++
    }
}

function Wait-Phase([string]$label, [int]$seconds) {
    Write-Host ">> wait $label (${seconds}s)"
    Start-Sleep -Seconds $seconds
}

function Assert-NoFatal {
    $fatals = Invoke-Adb logcat -d | Select-String "FATAL EXCEPTION" | Select-String "pocket4cut"
    if ($fatals) {
        Write-Host "[FAIL] FATAL:" -ForegroundColor Red
        $fatals | ForEach-Object { Write-Host $_ }
        return $false
    }
    return $true
}

Write-Host "=== Pocket 4Cut E2E ===" -ForegroundColor Cyan

if ($FreshStart) {
    Invoke-Adb logcat -c | Out-Null
    Invoke-Adb shell am force-stop com.pocket4cut | Out-Null
    Invoke-Adb shell pm grant com.pocket4cut android.permission.CAMERA | Out-Null
    Invoke-Adb shell input keyevent KEYCODE_WAKEUP | Out-Null
    Invoke-Adb shell input swipe 540 2200 540 900 250 | Out-Null
    Start-Sleep -Seconds 1
    Invoke-Adb shell am start -n com.pocket4cut/.MainActivity | Out-Null
    Wait-Phase "launch+home" 4

    Write-Host ">> HOME -> frame select"
    Tap 288 913
    Wait-Phase "frame select" 2

    Write-Host ">> pick 2-cut (fast path)"
    Tap 540 575
    Tap-BottomCta | Out-Null
    Wait-Phase "capture bind" 5

    Write-Host ">> start capture"
    Tap-BottomCta -MinY 2000 | Out-Null
    Wait-CaptureDone
}

$clickables = @(Get-Clickables)
Write-Host ">> current clickables: $($clickables.Count)"

if ($clickables.Count -ge 6) {
    Write-Host ">> SELECTION (pick 2 for 2-cut or 4 for 4-cut)"
    $gridCount = ($clickables | Where-Object { $_.Y -gt 500 -and $_.Y -lt 2200 }).Count
    $pick = if ($gridCount -ge 8) { 4 } else { 2 }
    Tap-GridFirstN $pick
    Start-Sleep -Seconds 1
    Tap-BottomCta | Out-Null
    Wait-Phase "layout" 3
}

Write-Host ">> LAYOUT confirm"
Tap-BottomCta | Out-Null
Wait-Phase "frame flow" 4

Write-Host ">> FRAME -> color pick (first card)"
Tap 540 520
Wait-Phase "color palette" 3

Write-Host ">> COLOR confirm"
Tap-BottomCta | Out-Null
Wait-Phase "edit" 4

Write-Host ">> EDIT -> detail edit"
Tap-BottomCta | Out-Null
Wait-Phase "detail edit load" 5

Write-Host ">> DETAIL EDIT render"
for ($s = 0; $s -lt 3; $s++) {
    Invoke-Adb shell input swipe 540 2000 540 900 250 | Out-Null
    Start-Sleep -Milliseconds 500
}
Tap 540 2350
Wait-Phase "render collage" 25

Write-Host ">> RESULT screen checks"
$clickables = @(Get-Clickables)
Write-Host "   clickables on result: $($clickables.Count)"

$resultCountOutput = (Invoke-Adb shell 'if [ -d /sdcard/Android/data/com.pocket4cut/files/Pictures/Pocket4Cut/results ]; then find /sdcard/Android/data/com.pocket4cut/files/Pictures/Pocket4Cut/results -maxdepth 1 -type f -name "*_result.jpg" | wc -l; else echo unavailable; fi' | Out-String).Trim()
$resultCount = 0
if ([int]::TryParse($resultCountOutput, [ref]$resultCount)) {
    Write-Host ">> saved result file count: $resultCount"
} else {
    Write-Host ">> saved result file count: unavailable"
}

$sessionReadStatus = (Invoke-Adb shell 'run-as com.pocket4cut sh -c "test -r files/sessions.json" >/dev/null 2>&1; echo $?' | Out-String).Trim()
Write-Host ">> session metadata readable: $($sessionReadStatus -eq '0')"

if (Assert-NoFatal) {
    Write-Host "[OK] E2E finished without FATAL crash" -ForegroundColor Green
} else {
    exit 1
}
