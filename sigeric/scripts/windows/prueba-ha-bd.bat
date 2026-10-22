@echo off
REM Doble clic para ejecutar prueba-ha-bd.ps1 sin cambiar la politica de ejecucion de PowerShell
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0prueba-ha-bd.ps1" %*
pause
