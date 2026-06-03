# Pocket 4Cut release keystore generator (run once)
# scripts\generate-release-keystore.bat

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
Set-Location $Root

function Get-AndroidStudioPathFromRegistry {
    $keys = @(
        "HKLM:\SOFTWARE\Android Studio",
        "HKLM:\SOFTWARE\WOW6432Node\Android Studio"
    )
    foreach ($key in $keys) {
        $props = Get-ItemProperty -Path $key -ErrorAction SilentlyContinue
        if ($props -and $props.Path -and (Test-Path $props.Path)) {
            return $props.Path
        }
    }
    return $null
}

function Find-Keytool {
    $candidates = @()

    if ($env:JAVA_HOME) {
        $candidates += "$env:JAVA_HOME\bin\keytool.exe"
    }

    $studioPath = Get-AndroidStudioPathFromRegistry
    if ($studioPath) {
        $candidates += Join-Path $studioPath "jbr\bin\keytool.exe"
    }

    $localProps = Join-Path $Root "local.properties"
    if (Test-Path $localProps) {
        Get-Content $localProps | ForEach-Object {
            if ($_ -match '^\s*studio\.dir=(.+)$') {
                $dir = $matches[1].Trim().Replace('\\', '\')
                $candidates += Join-Path $dir "jbr\bin\keytool.exe"
            }
        }
    }

    $candidates += @(
        "D:\Android Studio\jbr\bin\keytool.exe",
        "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe",
        "C:\Program Files\Android\Android Studio1\jbr\bin\keytool.exe",
        "$env:LOCALAPPDATA\Programs\Android\Android Studio\jbr\bin\keytool.exe"
    )

    foreach ($p in $candidates) {
        if ($p -and (Test-Path $p)) { return $p }
    }

    $found = Get-Command keytool -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }

    return $null
}

$keytool = Find-Keytool
if (-not $keytool) {
    Write-Host "[ERROR] keytool not found." -ForegroundColor Red
    Write-Host "Add to local.properties (optional):"
    Write-Host "  studio.dir=D\:\\Android Studio"
    Write-Host "Or use Android Studio: Build - Generate Signed Bundle/APK"
    exit 1
}

Write-Host "Using keytool: $keytool" -ForegroundColor DarkGray

$keystoreDir = Join-Path $Root "keystore"
$keystoreFile = Join-Path $keystoreDir "release.jks"
$propsFile = Join-Path $Root "keystore.properties"
$credentialsFile = Join-Path $Root "SIGNING_CREDENTIALS.txt"

if (Test-Path $keystoreFile) {
    Write-Host "[SKIP] Keystore already exists: $keystoreFile" -ForegroundColor Yellow
    exit 0
}

function New-RandomPassword([int]$Length = 20) {
    $chars = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    -join (1..$Length | ForEach-Object { $chars[(Get-Random -Maximum $chars.Length)] })
}

$storePassword = New-RandomPassword
$keyPassword = $storePassword
$keyAlias = "pocket4cut"
$validityDays = 10000

New-Item -ItemType Directory -Path $keystoreDir -Force | Out-Null

$dname = "CN=Pocket 4Cut, OU=Mobile, O=Pocket4Cut, L=Seoul, ST=Seoul, C=KR"
Write-Host "Creating keystore..." -ForegroundColor Cyan

& $keytool -genkeypair -v `
    -keystore $keystoreFile `
    -alias $keyAlias `
    -keyalg RSA `
    -keysize 2048 `
    -validity $validityDays `
    -storepass $storePassword `
    -keypass $keyPassword `
    -dname $dname

@"
storePassword=$storePassword
keyPassword=$keyPassword
keyAlias=$keyAlias
storeFile=keystore/release.jks
"@ | ForEach-Object {
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($propsFile, $_, $utf8NoBom)
}

@"
Pocket 4Cut release signing (DO NOT COMMIT)
Created: $(Get-Date -Format "yyyy-MM-dd HH:mm")

Keystore: keystore/release.jks
Alias: $keyAlias
Store password: $storePassword
Key password: $keyPassword

BACK UP keystore/ and this file. Loss = cannot update Play Store app.
"@ | Set-Content -Path $credentialsFile -Encoding UTF8

Write-Host ""
Write-Host "[OK] Done." -ForegroundColor Green
Write-Host "  keystore/release.jks"
Write-Host "  keystore.properties"
Write-Host "  SIGNING_CREDENTIALS.txt"
Write-Host ""
Write-Host "Build:"
Write-Host "  .\gradlew.bat assembleRelease"
Write-Host "  .\gradlew.bat bundleRelease"
