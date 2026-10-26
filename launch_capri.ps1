# Capri Desktop Launcher (100% Local Cyberpunk Construct)
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if (-not $scriptDir) { $scriptDir = "C:\Users\regre\Downloads\capri-desktop-main" }

Write-Host "==========================================================" -ForegroundColor Magenta
Write-Host " PROJECT CAPRICORN // CYBERPUNK EDGERUNNERS S2 CONSTRUCT  " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Magenta

# 1. Check Ollama
$ollamaUp = $false
try {
    $res = Invoke-RestMethod -Uri "http://127.0.0.1:11434/api/tags" -Method Get -TimeoutSec 2 -ErrorAction Stop
    if ($res -and $res.models) {
        $ollamaUp = $true
        $names = ($res.models | ForEach-Object { $_.name }) -join ", "
        Write-Host "[+] Ollama active (Models: $names)" -ForegroundColor Green
    }
} catch {
    $ollamaUp = $false
}

if (-not $ollamaUp) {
    Write-Host "[*] Ollama not detected at 127.0.0.1:11434. Starting Ollama..." -ForegroundColor Yellow
    Start-Process -FilePath "ollama" -ArgumentList "serve" -WindowStyle Hidden -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# 2. Check and start Python Helper (PyAutoGUI + Neural Anime Voice bridge)
$helperUp = $false
try {
    $h = Invoke-RestMethod -Uri "http://127.0.0.1:17777/health" -Method Get -TimeoutSec 1 -ErrorAction Stop
    if ($h.ok) { $helperUp = $true }
} catch {
    $helperUp = $false
}

if (-not $helperUp) {
    $helperScript = Join-Path $scriptDir "capri_helper.py"

    $pyCandidates = @(
        "$env:LOCALAPPDATA\Programs\Python\Python310\pythonw.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python310\python.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python312\pythonw.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe",
        "pythonw.exe",
        "python.exe"
    )

    $pyExe = $null
    foreach ($cand in $pyCandidates) {
        if (Test-Path $cand) { $pyExe = $cand; break }
    }
    if (-not $pyExe) { $pyExe = "python.exe" }

    Write-Host "[*] Starting Capri Python Helper (Vision + Neural Voice) via $pyExe..." -ForegroundColor Cyan
    Start-Process -FilePath $pyExe -ArgumentList "`"$helperScript`" 17777" -WorkingDirectory $scriptDir -WindowStyle Hidden -ErrorAction SilentlyContinue
    Start-Sleep -Milliseconds 800
} else {
    Write-Host "[+] Capri Python Helper running on port 17777" -ForegroundColor Green
}

# 3. Start Capri.exe or restore existing instance
$capriExe = Join-Path $scriptDir "Capri.exe"
if (-not (Test-Path $capriExe)) {
    $capriExe = "C:\Users\regre\Downloads\capri-desktop-main\Capri.exe"
}

$identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().User.Value
$restoreName = "Local\Capri.Restore." + $identity
$signaled = $false
try {
    $evt = [System.Threading.EventWaitHandle]::OpenExisting($restoreName)
    $evt.Set() | Out-Null
    $signaled = $true
    Write-Host "[+] Signaled existing Capri to restore to cursor and front!" -ForegroundColor Green
} catch {
    $signaled = $false
}

if (-not $signaled) {
    Write-Host "[*] Launching Capri desktop construct..." -ForegroundColor Cyan
    Start-Process -FilePath $capriExe -WorkingDirectory $scriptDir
}

Write-Host "==========================================================" -ForegroundColor Magenta
Write-Host " Capri is online! Look for her on your desktop screen." -ForegroundColor Green
Write-Host " Cyberdeck Mainframe and Voice Synth are ready!" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Magenta
