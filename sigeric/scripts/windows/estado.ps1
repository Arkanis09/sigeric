# Muestra el estado de todos los componentes de SIGERIC.
. (Join-Path $PSScriptRoot 'comun.ps1')
Titulo "Contenedores"
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
Titulo "Clúster MariaDB Galera"
Tamano-Cluster
Titulo "Respuesta a través del balanceador (5 peticiones)"
1..5 | ForEach-Object {
    try { $r = Invoke-RestMethod -Uri "$URL/api/estado" -TimeoutSec 5; "   atendió $($r.nodoApp) -> $($r.nodoBd) (clúster de $($r.wsrep_cluster_size))" }
    catch { Write-Host "   sin respuesta" -ForegroundColor Red }
}
