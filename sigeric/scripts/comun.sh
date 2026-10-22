#!/usr/bin/env bash
# Funciones compartidas por los scripts de prueba de alta disponibilidad.
cd "$(dirname "$0")/.."
URL="${SIGERIC_URL:-http://localhost:8080}"
ROOT_PW="${DB_ROOT_PASSWORD:-RootSigeric2026}"
TMP_DIR="$(mktemp -d)"
trap 'kill "$CARGA_PID" 2>/dev/null; rm -rf "$TMP_DIR"' EXIT

V=$'\e[32m'; R=$'\e[31m'; A=$'\e[33m'; N=$'\e[1m'; X=$'\e[0m'

titulo() { echo; echo "${N}=== $* ===${X}"; }
paso()   { echo "${A}>>> $*${X}"; }

sql() { docker exec "$1" mariadb -uroot -p"$ROOT_PW" -N -s -e "$2" 2>/dev/null; }

tamano_cluster() {
  for n in db1 db2 db3; do
    t=$(sql "$n" "SHOW STATUS LIKE 'wsrep_cluster_size'" | awk '{print $2}')
    if [ -n "$t" ]; then echo "   $n: ${V}activo${X} (ve un clúster de $t nodos)"; else echo "   $n: ${R}APAGADO${X}"; fi
  done
}

esperar_sano() {   # esperar_sano <contenedor>
  for _ in $(seq 1 60); do
    [ "$(docker inspect -f '{{.State.Health.Status}}' "$1" 2>/dev/null)" = healthy ] && return 0
    sleep 2
  done
  echo "${R}$1 no volvió a estar sano${X}"; return 1
}

esperar_app() {    # esperar_app <contenedor>: hasta que Spring Boot termine de arrancar
  for _ in $(seq 1 60); do
    docker exec nginx wget -q -O /dev/null "http://$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$1"):8080/actuator/health" 2>/dev/null && return 0
    sleep 2
  done
  echo "${R}$1 no volvió a responder${X}"; return 1
}

# Carga continua: una ESCRITURA por el balanceador cada 0.2 s, registrando cada resultado.
iniciar_carga() {
  : > "$TMP_DIR/log"
  (
    while true; do
      r=$(curl -s -m 20 -X POST "$URL/api/latido" -w ' HTTP%{http_code} T%{time_total}')
      t=${r##*T}
      if [[ "$r" == *'"id"'*HTTP200* ]]; then
        echo "OK $(echo "$r" | grep -o '"nodoApp":"[^"]*","nodoBd":"[^"]*"' | sed 's/"nodoApp":"//;s/","nodoBd":"/->/;s/"//') $t" >> "$TMP_DIR/log"
      else
        echo "FALLO $(echo "${r% T*}" | tr -d '\n' | grep -o 'HTTP[0-9]*$') $t" >> "$TMP_DIR/log"
      fi
      sleep 0.2
    done
  ) &
  CARGA_PID=$!
}

detener_carga() { kill "$CARGA_PID" 2>/dev/null; wait "$CARGA_PID" 2>/dev/null; }

resumen_reciente() {   # quién atendió las últimas N escrituras
  tail -n "${1:-15}" "$TMP_DIR/log" | awk '{print $2}' | sort | uniq -c | sed 's/^/     /'
}

resultado_final() {
  local ok fallos
  ok=$(grep -c '^OK' "$TMP_DIR/log"); fallos=$(grep -c '^FALLO' "$TMP_DIR/log")
  titulo "RESULTADO"
  echo "   Escrituras exitosas: ${V}${ok}${X}"
  echo "   Escrituras fallidas: $([ "$fallos" -eq 0 ] && echo "${V}0${X}" || echo "${R}${fallos}${X}")"
  echo "   Tiempo de respuesta máximo: $(awk '{print $NF}' "$TMP_DIR/log" | sort -g | tail -1) s  (promedio: $(awk '{s+=$NF} END {printf "%.3f", s/NR}' "$TMP_DIR/log") s)"
  echo "   Distribución (instancia -> nodo BD):"; grep '^OK' "$TMP_DIR/log" | awk '{print $2}' | sort | uniq -c | sed 's/^/     /'
  echo "   Filas en la tabla 'latido' en cada nodo (deben ser iguales):"
  for n in db1 db2 db3; do printf "     %s: %s\n" "$n" "$(sql "$n" 'SELECT COUNT(*) FROM sigeric.latido')"; done
  grep '^FALLO' "$TMP_DIR/log" | head -3 | sed 's/^/     /'
  if [ "$fallos" -eq 0 ]; then
    echo; echo "${V}${N}✔ PRUEBA SUPERADA: el sistema siguió funcionando y escribiendo sin errores.${X}"
  else
    echo; echo "${R}${N}✘ Hubo $fallos escrituras fallidas.${X}"
  fi
}
