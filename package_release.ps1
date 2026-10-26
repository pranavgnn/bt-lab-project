# Capri Desktop // Release Packaging Script
# Creates Capri-Desktop-v1.2.0.zip for GitHub Releases distribution

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $scriptDir) { $scriptDir = "." }

$version = "v1.2.0"
$zipName = "Capri-Desktop-$version.zip"
$zipPath = Join-Path $scriptDir $zipName
$stagingDir = Join-Path $env:TEMP "Capri-Staging"

Write-Host "Packaging $zipName..." -ForegroundColor Cyan

if (Test-Path $stagingDir) { Remove-Item $stagingDir -Recurse -Force }
New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null

# Files to bundle
$filesToCopy = @(
    "Capri.exe",
    "capri_helper.py",
    "capri_chatterbox_conds.pt",
    "setup_voice.bat",
    "setup_voice.ps1",
    "run.bat",
    "launch_capri.ps1",
    "README.md"
)

foreach ($f in $filesToCopy) {
    $src = Join-Path $scriptDir $f
    if (Test-Path $src) {
        Copy-Item -Path $src -Destination $stagingDir -Force
        Write-Host "  [+] Copied: $f" -ForegroundColor Green
    } else {
        Write-Host "  [!] Missing optional file: $f" -ForegroundColor Yellow
    }
}

# Copy assets folder
$assetsSrc = Join-Path $scriptDir "assets"
if (Test-Path $assetsSrc) {
    Copy-Item -Path $assetsSrc -Destination $stagingDir -Recurse -Force
    Write-Host "  [+] Copied: assets/" -ForegroundColor Green
}

# Create ZIP archive
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::CreateFromDirectory($stagingDir, $zipPath)

Remove-Item $stagingDir -Recurse -Force -ErrorAction SilentlyContinue

$zipItem = Get-Item $zipPath
$zipSizeMb = [Math]::Round($zipItem.Length / 1MB, 2)

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  PACKAGE CREATED SUCCESSFULLY: $zipName ($zipSizeMb MB)" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "  Location: $zipPath" -ForegroundColor White
Write-Host "  Attach this file to your GitHub Release: $version" -ForegroundColor Cyan
