# PRUEBA 1 - Alta disponibilidad de BASE DE DATOS (sección 10.7 del documento)
# Se desconecta un nodo del clúster Galera mientras el sistema recibe escrituras continuas.
# Éxito = ninguna escritura falla y los nodos restantes siguen aceptando datos.
param([string]$Nodo = 'db1')
. (Join-Path $PSScriptRoot 'comun.ps1')
try {
    Titulo "PRUEBA DE ALTA DISPONIBILIDAD - BASE DE DATOS (se apagará $Nodo)"
    Paso "Estado inicial del clúster:"; Tamano-Cluster
    Paso "Iniciando escrituras continuas a través del sistema..."
    Iniciar-Carga; Start-Sleep -Seconds 6
    Write-Host "   Últimas escrituras:"; Resumen-Reciente 10

    Paso "DESCONECTANDO el nodo $Nodo  (docker stop $Nodo)"
    docker stop $Nodo | Out-Null
    Start-Sleep -Seconds 8
    Paso "Clúster con un nodo menos:"; Tamano-Cluster
    Write-Host "   Escrituras mientras $Nodo está caído:"; Resumen-Reciente 15

    Paso "Insertando un dato DIRECTAMENTE en cada nodo que sigue vivo:"
    foreach ($n in 'db1', 'db2', 'db3') {
        if ($n -eq $Nodo) { continue }
        docker exec $n mariadb -uroot "-p$ROOT_PW" -e "INSERT INTO sigeric.latido(nodo_app,nodo_bd,fecha) VALUES('manual','$n',NOW(3))" 2>$null
        if ($LASTEXITCODE -eq 0) { Write-Host "   OK: $n aceptó la escritura" -ForegroundColor Green }
        else { Write-Host "   ERROR: $n rechazó la escritura" -ForegroundColor Red }
    }
    Start-Sleep -Seconds 4

    Paso "RECONECTANDO $Nodo (docker start $Nodo): se sincroniza solo con lo que se perdió"
    docker start $Nodo | Out-Null
    Esperar-Sano $Nodo; Start-Sleep -Seconds 3
    Paso "Clúster recuperado:"; Tamano-Cluster
    Detener-Carga; Start-Sleep -Seconds 1
    Resultado-Final
} finally { Detener-Carga }
