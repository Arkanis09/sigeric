#!/usr/bin/env bash
# Muestra el estado de todos los componentes de SIGERIC.
source "$(dirname "$0")/comun.sh"
titulo "Contenedores"
docker compose ps --format 'table {{.Name}}\t{{.Status}}'
titulo "Clúster MariaDB Galera"
tamano_cluster
titulo "Respuesta a través del balanceador (5 peticiones)"
for _ in 1 2 3 4 5; do
  curl -s -m 5 "$URL/api/estado" | grep -o '"nodoApp":"[^"]*","nodoBd":"[^"]*"' || echo "${R}sin respuesta${X}"
done
