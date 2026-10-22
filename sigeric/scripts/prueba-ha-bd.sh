#!/usr/bin/env bash
# PRUEBA 1 - Alta disponibilidad de BASE DE DATOS (sección 10.7 del documento)
# Se desconecta un nodo del clúster Galera mientras el sistema recibe escrituras continuas.
# Éxito = ninguna escritura falla y los nodos restantes siguen aceptando datos.
source "$(dirname "$0")/comun.sh"
NODO="${1:-db1}"

titulo "PRUEBA DE ALTA DISPONIBILIDAD - BASE DE DATOS (se apagará $NODO)"
paso "Estado inicial del clúster:"; tamano_cluster
paso "Iniciando escrituras continuas a través del sistema..."
iniciar_carga; sleep 5
echo "   Últimas escrituras:"; resumen_reciente 10

paso "DESCONECTANDO el nodo $NODO  (docker stop $NODO)"
docker stop "$NODO" >/dev/null
sleep 8
paso "Clúster con un nodo menos:"; tamano_cluster
echo "   Escrituras mientras $NODO está caído:"; resumen_reciente 15

paso "Insertando un dato DIRECTAMENTE en cada nodo que sigue vivo:"
for n in db1 db2 db3; do
  [ "$n" = "$NODO" ] && continue
  if sql "$n" "INSERT INTO sigeric.latido(nodo_app,nodo_bd,fecha) VALUES('manual','$n',NOW(3))"; then
    echo "   ${V}✔ $n aceptó la escritura${X}"
  else
    echo "   ${R}✘ $n rechazó la escritura${X}"
  fi
done
sleep 4

paso "RECONECTANDO $NODO (docker start $NODO): se sincroniza solo con lo que se perdió"
docker start "$NODO" >/dev/null
esperar_sano "$NODO"; sleep 3
paso "Clúster recuperado:"; tamano_cluster
detener_carga; sleep 1
resultado_final
