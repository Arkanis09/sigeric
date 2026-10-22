@echo off
REM Doble clic para ejecutar estado.ps1 sin cambiar la politica de ejecucion de PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0estado.ps1" %*
pause
