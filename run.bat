@echo off
cd /d "%~dp0"
title Project Capricorn - Launching...
powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0launch_capri.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [!] Direct launch fallback...
    start "" "%~dp0Capri.exe"
)
