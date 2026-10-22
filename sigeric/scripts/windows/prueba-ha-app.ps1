# PRUEBA 2 - Alta disponibilidad de APLICACIÓN (sección 10.7 del documento)
# Se detiene una de las dos instancias de Spring Boot mientras el sistema recibe peticiones.
# Éxito = el usuario no nota interrupción: ninguna petición falla.
param([string]$App = 'app1')
. (Join-Path $PSScriptRoot 'comun.ps1')
try {
    Titulo "PRUEBA DE ALTA DISPONIBILIDAD - APLICACIÓN (se detendrá $App)"
    Paso "Iniciando peticiones continuas a través del balanceador NGINX..."
    Iniciar-Carga; Start-Sleep -Seconds 6
    Write-Host "   NGINX reparte entre las dos instancias:"; Resumen-Reciente 14

    Paso "DETENIENDO la instancia $App  (docker stop $App)"
    docker stop $App | Out-Null
    Start-Sleep -Seconds 8
    Write-Host "   Peticiones mientras $App está detenida (todas las atiende la otra):"; Resumen-Reciente 15

    Paso "REINICIANDO $App (docker start $App)"
    docker start $App | Out-Null
    Esperar-App $App; Start-Sleep -Seconds 12
    Write-Host "   $App vuelve a la rotación automáticamente:"; Resumen-Reciente 14
    Detener-Carga; Start-Sleep -Seconds 1
    Resultado-Final
} finally { Detener-Carga }
