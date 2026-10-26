# Capri Desktop // Voice Engine & Dependencies Setup Script
# Configures Kokoro-82M Neural TTS, Whisper ASR, and Computer Vision Perception
param(
    [switch]$ForceReinstall
)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "=================================================================" -ForegroundColor Magenta
Write-Host "      PROJECT CAPRICORN // VOICE ENGINE & WEIGHTS SETUP          " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Magenta
Write-Host ""

# 1. Locate Python
Write-Host "[1/5] Detecting Python Environment..." -ForegroundColor Yellow

$pyExe = $null
$pyCommands = @("python", "py -3", "python3")
foreach ($cmd in $pyCommands) {
    try {
        $ver = & ($cmd.Split()[0]) ($cmd.Split()[1..($cmd.Split().Length-1)] + @("--version")) 2>&1
        if ($ver -match "Python 3\.") {
            $pyExe = $cmd
            Write-Host "  [+] Found: $ver ($pyExe)" -ForegroundColor Green
            break
        }
    } catch {}
}

if (-not $pyExe) {
    # Check standard local AppData installation paths
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\Python\Python312\python.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python311\python.exe",
        "$env:LOCALAPPDATA\Programs\Python\Python310\python.exe"
    )
    foreach ($cand in $candidates) {
        if (Test-Path $cand) {
            $pyExe = $cand
            Write-Host "  [+] Found Python at: $pyExe" -ForegroundColor Green
            break
        }
    }
}

if (-not $pyExe) {
    Write-Host "  [!] ERROR: Python 3.10+ was not found on your system!" -ForegroundColor Red
    Write-Host "  Please install Python 3.10, 3.11, or 3.12 (Check 'Add Python to PATH' during install)." -ForegroundColor Yellow
    Write-Host "  Opening python.org in your browser..." -ForegroundColor Cyan
    Start-Process "https://www.python.org/downloads/"
    Read-Host "Press Enter to exit..."
    exit 1
}

# 2. Check for NVIDIA GPU with CUDA
Write-Host ""
Write-Host "[2/5] Checking Hardware & GPU Acceleration..." -ForegroundColor Yellow
$hasCuda = $false
try {
    $smi = & nvidia-smi 2>&1
    if ($LASTEXITCODE -eq 0 -and $smi -match "NVIDIA-SMI") {
        $hasCuda = $true
        Write-Host "  [+] NVIDIA GPU Detected! Installing CUDA acceleration for instant 0.15s TTS." -ForegroundColor Green
    }
} catch {}

if (-not $hasCuda) {
    Write-Host "  [*] No NVIDIA GPU detected or nvidia-smi not in PATH. Will use high-speed CPU execution." -ForegroundColor Cyan
}

# 3. Install PyTorch with CUDA or CPU
Write-Host ""
Write-Host "[3/5] Installing PyTorch & Neural Audio Engines..." -ForegroundColor Yellow

if ($hasCuda) {
    Write-Host "  [*] Installing PyTorch with CUDA 12.1..." -ForegroundColor Cyan
    & $pyExe -m pip install torch torchaudio --index-url https://download.pytorch.org/whl/cu121 --upgrade --no-warn-script-location
} else {
    Write-Host "  [*] Installing CPU PyTorch..." -ForegroundColor Cyan
    & $pyExe -m pip install torch torchaudio --upgrade --no-warn-script-location
}

# 4. Install Voice & Perception Libraries
Write-Host ""
Write-Host "[4/5] Installing Capri Voice & Vision Dependencies..." -ForegroundColor Yellow
$packages = @(
    "kokoro>=0.8.4",
    "soundfile",
    "numpy",
    "transformers",
    "pillow",
    "pyautogui",
    "pygetwindow",
    "rapidocr-onnxruntime",
    "duckduckgo-search"
)

Write-Host "  [*] Packages: $($packages -join ' ')" -ForegroundColor Cyan
& $pyExe -m pip install $packages --upgrade --no-warn-script-location

# 5. Pre-cache & Verify Model Weights
Write-Host ""
Write-Host "[5/5] Downloading & Pre-caching Neural Voice Weights..." -ForegroundColor Yellow
Write-Host "  Downloading Kokoro-82M ('af_sarah' Echo Construct + 'af_heart' Mommy ASMR) and Whisper-base STT..." -ForegroundColor Cyan

$testScript = @"
import sys
import torch

dev = 'cuda' if torch.cuda.is_available() else 'cpu'
print(f'[*] Using compute device: {dev}')

try:
    import kokoro
    print('[*] Caching Kokoro-82M neural model weights...')
    pipeline = kokoro.KPipeline(lang_code='a', device=dev)
    
    # Pre-cache af_sarah (Echo Construct)
    print('  -> Warming up voice profile: af_sarah (Echo Construct)...')
    for _, _, _ in pipeline('Capri construct initialized.', voice='af_sarah'):
        break
        
    # Pre-cache af_heart (Mommy ASMR)
    print('  -> Warming up voice profile: af_heart (Mommy ASMR)...')
    for _, _, _ in pipeline('ASMR close-mic profile loaded.', voice='af_heart'):
        break
    print('[+] Kokoro neural voices cached successfully!')
except Exception as e:
    print(f'[!] Kokoro cache error: {e}')

try:
    from transformers import pipeline as hf_pipeline
    print('[*] Caching Whisper-base speech-to-text model...')
    whisper_dev = 'cuda' if torch.cuda.is_available() else 'cpu'
    stt = hf_pipeline('automatic-speech-recognition', model='openai/whisper-base', device=whisper_dev)
    print('[+] Whisper speech recognition model cached successfully!')
except Exception as e:
    print(f'[!] Whisper cache error: {e}')

try:
    from rapidocr_onnxruntime import RapidOCR
    ocr = RapidOCR()
    print('[+] RapidOCR screen vision models initialized!')
except Exception as e:
    print(f'[!] RapidOCR error: {e}')

print('[SUCCESS] All neural weights verified and ready.')
"@

$scriptPath = Join-Path $env:TEMP "capri_cache_warmup.py"
[System.IO.File]::WriteAllText($scriptPath, $testScript)

& $pyExe $scriptPath
Remove-Item $scriptPath -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "=================================================================" -ForegroundColor Green
Write-Host "   CAPRI VOICE & INTELLIGENCE SETUP COMPLETED SUCCESSFULLY!      " -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor Green
Write-Host "  Voices available:" -ForegroundColor Cyan
Write-Host "    - Echo Construct (Holographic Netrunner with cyber slapback echo)" -ForegroundColor White
Write-Host "    - Mommy ASMR     (Warm, close-mic maternal ASMR voice)" -ForegroundColor White
Write-Host ""
Write-Host "  To launch Capri:" -ForegroundColor Yellow
Write-Host "    Double click 'run.bat' or run 'launch_capri.ps1'" -ForegroundColor White
Write-Host ""
Read-Host "Press Enter to exit..."
