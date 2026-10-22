#!/usr/bin/env bash
# PRUEBA 3 - Falla simultánea en AMBAS capas: se apaga un nodo de BD y una instancia
# de la app al mismo tiempo (peor caso tolerable con 3 nodos de BD y 2 de aplicación).
source "$(dirname "$0")/comun.sh"

titulo "PRUEBA COMBINADA - se apagan db1 y app1 al mismo tiempo"
paso "Estado inicial:"; tamano_cluster
iniciar_carga; sleep 5
echo "   Escrituras normales:"; resumen_reciente 10

paso "APAGANDO db1 y app1 a la vez"
docker stop db1 app1 >/dev/null
sleep 10
paso "Estado con fallas:"; tamano_cluster
echo "   Escrituras con db1 y app1 caídos:"; resumen_reciente 15

paso "Recuperando db1 y app1"
docker start db1 app1 >/dev/null
esperar_sano db1; esperar_app app1; sleep 10
paso "Estado final:"; tamano_cluster
echo "   Escrituras tras la recuperación:"; resumen_reciente 14
detener_carga; sleep 1
resultado_final
