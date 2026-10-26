@echo off
cd /d "%~dp0"
title Capri Desktop - Voice & Weights Setup
echo =========================================================
echo    PROJECT CAPRICORN // VOICE WEIGHTS INSTALLER
echo =========================================================
echo.
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0setup_voice.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Setup encountered an issue or was cancelled.
    pause
)
