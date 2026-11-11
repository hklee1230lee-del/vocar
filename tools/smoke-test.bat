@echo off
rem Build, install, launch, then walk through the screens in tools\smoke-steps.txt. Results: build\run\
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-on-emulator.ps1" -Smoke %*
