#!/bin/bash
# Decide cómo arranca cada nodo Galera, sin intervención manual:
#  1. Primer arranque del nodo "semilla" (db1): crea el clúster (--wsrep-new-cluster).
#  2. Si algún compañero está en el clúster PRIMARIO: se une a él y se sincroniza solo (IST/SST).
#  3. Si hay compañeros encendidos pero ninguno es primario (se perdió el quórum, p. ej. cayeron
#     2 de 3 nodos): se elige al nodo con los datos más recientes y se reactiva el clúster con él.
#  4. Si NINGÚN nodo está encendido (se apagó todo): cada nodo publica su último seqno en un
#     volumen compartido y el que tenga los datos más recientes levanta el clúster de nuevo.
#  En todos los casos gana quien tenga los datos más recientes, así no se pierden escrituras
#  ni se crean "dos cerebros".
set -e
DATADIR=/var/lib/mysql
GRASTATE="$DATADIR/grastate.dat"
COORD=/coord
PARES="172.28.50.11 172.28.50.12 172.28.50.13"
TOTAL_NODOS=3
MI_IP=$(hostname -i | awk '{print $1}')

log() { echo ">>> [$HOSTNAME] $*"; }

if [ "$NODO_SEMILLA" = "1" ] && [ ! -d "$DATADIR/mysql" ]; then
  log "Primer arranque: creando el clúster Galera"
  cp /sigeric/01-esquema.sql /docker-entrypoint-initdb.d/
  exec docker-entrypoint.sh mariadbd --wsrep-new-cluster "$@"
fi

# ¿El proceso Galera del compañero está encendido? (puerto de replicación 4567)
par_encendido() { (exec 3<>"/dev/tcp/$1/4567") 2>/dev/null; }

# Estado del compañero vía SQL: "Primary <seqno>", "non-Primary <seqno>" o vacío si no responde
estado_par() {
  mariadb -h "$1" -uroot -p"$MARIADB_ROOT_PASSWORD" --connect-timeout=2 -N -s -e \
    "SELECT MAX(IF(VARIABLE_NAME='WSREP_CLUSTER_STATUS',VARIABLE_VALUE,NULL)),
            MAX(IF(VARIABLE_NAME='WSREP_LAST_COMMITTED',VARIABLE_VALUE,NULL))
       FROM information_schema.GLOBAL_STATUS
      WHERE VARIABLE_NAME IN ('WSREP_CLUSTER_STATUS','WSREP_LAST_COMMITTED')" 2>/dev/null | tr '\t' ' ' || true
}

unirse() {
  rm -f "$COORD/$HOSTNAME"
  log "Uniéndome al clúster primario"
  exec docker-entrypoint.sh mariadbd "$@"
}

reiniciar_cluster() {
  rm -f "$COORD/$HOSTNAME"
  sed -i 's/^safe_to_bootstrap:.*/safe_to_bootstrap: 1/' "$GRASTATE"
  exec docker-entrypoint.sh mariadbd --wsrep-new-cluster "$@"
}

mi_seqno() {
  local s
  s=$(awk '/^seqno:/ {print $2}' "$GRASTATE" 2>/dev/null)
  if [ -z "$s" ] || [ "$s" = "-1" ]; then
    # Apagado brusco: se recupera la última posición desde InnoDB
    chown -R mysql:mysql "$DATADIR"
    gosu mysql mariadbd --wsrep-recover --log-error=/tmp/recover.log >/dev/null 2>&1 || true
    s=$(grep -o 'Recovered position: .*' /tmp/recover.log | tail -1 | awk -F: '{print $NF}')
  fi
  echo "${s:--1}"
}

mkdir -p "$COORD"
SEQNO=""
SIN_PRIMARIO=0
log "Buscando el clúster..."
for i in $(seq 1 150); do
  ALGUNO_ENCENDIDO=0
  NO_PRIMARIOS=""
  for ip in $PARES; do
    [ "$ip" = "$MI_IP" ] && continue
    par_encendido "$ip" || continue
    ALGUNO_ENCENDIDO=1
    est=$(estado_par "$ip")
    case "$est" in
      Primary*)     unirse "$@" ;;
      non-Primary*) NO_PRIMARIOS="$NO_PRIMARIOS ${est#non-Primary } $ip" ;;
    esac
  done

  # Nodo nuevo sin datos (db2/db3 en su primer arranque): solo puede esperar a un primario
  if [ ! -f "$GRASTATE" ]; then
    [ "$i" = 1 ] && log "Nodo nuevo: esperando a que el clúster esté activo..."
    sleep 2; continue
  fi
  [ -z "$SEQNO" ] && SEQNO=$(mi_seqno)

  if [ -n "$NO_PRIMARIOS" ]; then
    # Caso 3: hay nodos encendidos sin quórum. Se espera ~15 s por si es algo pasajero.
    SIN_PRIMARIO=$((SIN_PRIMARIO + 1))
    if [ "$SIN_PRIMARIO" -ge 8 ]; then
      # Candidatos: nodos encendidos (prioridad 1 en empate, no necesitan copiar datos) y yo
      CAND=$(echo $NO_PRIMARIOS | xargs -n2 | awk '{print $1, 1, $2}'; echo "$SEQNO 0 $MI_IP")
      GANADOR=$(echo "$CAND" | sort -k1,1nr -k2,2nr -k3,3 | head -1)
      G_IP=$(echo "$GANADOR" | awk '{print $3}')
      if [ "$G_IP" = "$MI_IP" ]; then
        log "Sin quórum y tengo los datos más recientes (seqno $SEQNO): reinicio el clúster"
        reiniciar_cluster "$@"
      fi
      log "Sin quórum: reactivo el clúster en $G_IP, que tiene los datos más recientes"
      # MariaDB 11 usa wsrep_provider_pc_bootstrap; versiones anteriores, wsrep_provider_options
      mariadb -h "$G_IP" -uroot -p"$MARIADB_ROOT_PASSWORD" --connect-timeout=2 -e \
        "SET GLOBAL wsrep_provider_pc_bootstrap=ON" 2>/dev/null \
      || mariadb -h "$G_IP" -uroot -p"$MARIADB_ROOT_PASSWORD" --connect-timeout=2 -e \
        "SET GLOBAL wsrep_provider_options='pc.bootstrap=YES'" 2>/dev/null || true
      SIN_PRIMARIO=0
      sleep 3
    fi
  elif [ "$ALGUNO_ENCENDIDO" = 0 ]; then
    # Caso 4: no hay ningún nodo encendido. Elección por volumen compartido.
    SIN_PRIMARIO=0
    echo "$SEQNO" > "$COORD/$HOSTNAME"
    [ $((i % 10)) = 1 ] && log "Ningún nodo activo. Mi último seqno es $SEQNO; esperando a los demás para elegir"
    N=$(ls "$COORD" | wc -l)
    # Con todos presentes se decide ya; tras ~60 s basta con la mayoría (quórum)
    if [ "$N" -ge "$TOTAL_NODOS" ] || { [ "$i" -gt 30 ] && [ "$N" -ge 2 ]; }; then
      GANADOR=$(for f in "$COORD"/*; do echo "$(cat "$f") $(basename "$f")"; done \
                | sort -k1,1nr -k2,2 | head -1 | awk '{print $2}')
      if [ "$GANADOR" = "$HOSTNAME" ]; then
        log "Tengo los datos más recientes: reinicio el clúster"
        reiniciar_cluster "$@"
      fi
    fi
  fi
  # (Si hay compañeros encendidos que aún no responden por SQL, están arrancando: se espera.)
  sleep 2
done
log "Tiempo de espera agotado; se reintentará (restart policy)"
exit 1
