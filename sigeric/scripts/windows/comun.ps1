# Funciones compartidas por los scripts de prueba de alta disponibilidad (Windows / PowerShell 5.1+).
$ErrorActionPreference = 'Continue'
Set-Location (Join-Path $PSScriptRoot '..\..')
$URL = if ($env:SIGERIC_URL) { $env:SIGERIC_URL } else { 'http://localhost:8080' }
$ROOT_PW = if ($env:DB_ROOT_PASSWORD) { $env:DB_ROOT_PASSWORD } else { 'RootSigeric2026' }
$LOG = Join-Path ([System.IO.Path]::GetTempPath()) ("sigeric-ha-" + [guid]::NewGuid().ToString() + ".log")
$script:CargaJob = $null

function Titulo($t) { Write-Host ""; Write-Host "=== $t ===" -ForegroundColor White }
function Paso($t)   { Write-Host ">>> $t" -ForegroundColor Yellow }

function Sql($nodo, $consulta) {
    $r = docker exec $nodo mariadb -uroot "-p$ROOT_PW" -N -s -e $consulta 2>$null
    if ($LASTEXITCODE -ne 0) { return $null }
    return $r
}

function Tamano-Cluster {
    foreach ($n in 'db1', 'db2', 'db3') {
        $r = Sql $n "SHOW STATUS LIKE 'wsrep_cluster_size'"
        if ($r) {
            $t = ($r -split '\s+')[1]
            Write-Host "   ${n}: " -NoNewline; Write-Host "activo" -ForegroundColor Green -NoNewline; Write-Host " (ve un clúster de $t nodos)"
        } else {
            Write-Host "   ${n}: " -NoNewline; Write-Host "APAGADO" -ForegroundColor Red
        }
    }
}

function Esperar-Sano($c) {
    for ($i = 0; $i -lt 60; $i++) {
        if ((docker inspect -f '{{.State.Health.Status}}' $c 2>$null) -eq 'healthy') { return }
        Start-Sleep -Seconds 2
    }
    Write-Host "$c no volvió a estar sano" -ForegroundColor Red
}

function Esperar-App($c) {
    $ip = docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' $c
    for ($i = 0; $i -lt 60; $i++) {
        docker exec nginx wget -q -O /dev/null "http://${ip}:8080/actuator/health" 2>$null
        if ($LASTEXITCODE -eq 0) { return }
        Start-Sleep -Seconds 2
    }
    Write-Host "$c no volvió a responder" -ForegroundColor Red
}

# Carga continua en segundo plano: una ESCRITURA por el balanceador cada 0.2 s.
function Iniciar-Carga {
    Set-Content -Path $LOG -Value $null
    $script:CargaJob = Start-Job -ArgumentList $URL, $LOG -ScriptBlock {
        param($url, $log)
        while ($true) {
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            try {
                $r = Invoke-RestMethod -Method Post -Uri "$url/api/latido" -TimeoutSec 20
                $linea = "OK $($r.nodoApp)->$($r.nodoBd)"
            } catch {
                $codigo = 'SIN_RESPUESTA'
                if ($_.Exception.Response) { $codigo = 'HTTP' + [int]$_.Exception.Response.StatusCode }
                $linea = "FALLO $codigo"
            }
            $sw.Stop()
            Add-Content -Path $log -Value ("$linea " + $sw.Elapsed.TotalSeconds.ToString('0.000', [Globalization.CultureInfo]::InvariantCulture))
            Start-Sleep -Milliseconds 200
        }
    }
}

function Detener-Carga {
    if ($script:CargaJob) { Stop-Job $script:CargaJob; Remove-Job $script:CargaJob -Force; $script:CargaJob = $null }
}

function Leer-Log { if (Test-Path $LOG) { @(Get-Content $LOG | Where-Object { $_ }) } else { @() } }

function Resumen-Reciente($n = 15) {
    Leer-Log | Select-Object -Last $n | ForEach-Object { ($_ -split ' ')[1] } |
        Group-Object | Sort-Object Name | ForEach-Object { "     {0,3} {1}" -f $_.Count, $_.Name }
}

function Resultado-Final {
    $lineas = Leer-Log
    $ok = @($lineas | Where-Object { $_ -like 'OK *' })
    $fallos = @($lineas | Where-Object { $_ -like 'FALLO *' })
    $tiempos = $lineas | ForEach-Object { [double]::Parse(($_ -split ' ')[-1], [Globalization.CultureInfo]::InvariantCulture) }
    $stats = $tiempos | Measure-Object -Maximum -Average
    Titulo "RESULTADO"
    Write-Host "   Escrituras exitosas: " -NoNewline; Write-Host $ok.Count -ForegroundColor Green
    Write-Host "   Escrituras fallidas: " -NoNewline
    if ($fallos.Count -eq 0) { Write-Host 0 -ForegroundColor Green } else { Write-Host $fallos.Count -ForegroundColor Red }
    Write-Host ("   Tiempo de respuesta máximo: {0:0.000} s  (promedio: {1:0.000} s)" -f $stats.Maximum, $stats.Average)
    Write-Host "   Distribución (instancia -> nodo BD):"
    $ok | ForEach-Object { ($_ -split ' ')[1] } | Group-Object | Sort-Object Name | ForEach-Object { "     {0,3} {1}" -f $_.Count, $_.Name }
    Write-Host "   Filas en la tabla 'latido' en cada nodo (deben ser iguales):"
    foreach ($n in 'db1', 'db2', 'db3') { Write-Host "     ${n}: $(Sql $n 'SELECT COUNT(*) FROM sigeric.latido')" }
    $fallos | Select-Object -First 3 | ForEach-Object { Write-Host "     $_" -ForegroundColor Red }
    Write-Host ""
    if ($fallos.Count -eq 0) {
        Write-Host "PRUEBA SUPERADA: el sistema siguió funcionando y escribiendo sin errores." -ForegroundColor Green
    } else {
        Write-Host "Hubo $($fallos.Count) escrituras fallidas." -ForegroundColor Red
    }
    Remove-Item $LOG -ErrorAction SilentlyContinue
}
