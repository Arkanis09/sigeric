# PRUEBA 3 - Falla simultánea en AMBAS capas: se apaga un nodo de BD y una instancia
# de la app al mismo tiempo (peor caso tolerable con 3 nodos de BD y 2 de aplicación).
. (Join-Path $PSScriptRoot 'comun.ps1')
try {
    Titulo "PRUEBA COMBINADA - se apagan db1 y app1 al mismo tiempo"
    Paso "Estado inicial:"; Tamano-Cluster
    Iniciar-Carga; Start-Sleep -Seconds 6
    Write-Host "   Escrituras normales:"; Resumen-Reciente 10

    Paso "APAGANDO db1 y app1 a la vez"
    docker stop db1 app1 | Out-Null
    Start-Sleep -Seconds 10
    Paso "Estado con fallas:"; Tamano-Cluster
    Write-Host "   Escrituras con db1 y app1 caídos:"; Resumen-Reciente 15

    Paso "Recuperando db1 y app1"
    docker start db1 app1 | Out-Null
    Esperar-Sano db1; Esperar-App app1; Start-Sleep -Seconds 10
    Paso "Estado final:"; Tamano-Cluster
    Write-Host "   Escrituras tras la recuperación:"; Resumen-Reciente 14
    Detener-Carga; Start-Sleep -Seconds 1
    Resultado-Final
} finally { Detener-Carga }
