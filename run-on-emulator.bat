@echo off
rem Build, install and launch on the Pleos Connect emulator. Results: build\run\
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0tools\run-on-emulator.ps1" %*
echo.
pause
