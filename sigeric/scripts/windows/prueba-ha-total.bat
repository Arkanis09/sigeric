@echo off
REM Doble clic para ejecutar prueba-ha-total.ps1 sin cambiar la politica de ejecucion de PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0prueba-ha-total.ps1" %*
pause
