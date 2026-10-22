#!/usr/bin/env bash
# PRUEBA 2 - Alta disponibilidad de APLICACIÓN (sección 10.7 del documento)
# Se detiene una de las dos instancias de Spring Boot mientras el sistema recibe peticiones.
# Éxito = el usuario no nota interrupción: ninguna petición falla.
source "$(dirname "$0")/comun.sh"
APP="${1:-app1}"

titulo "PRUEBA DE ALTA DISPONIBILIDAD - APLICACIÓN (se detendrá $APP)"
paso "Iniciando peticiones continuas a través del balanceador NGINX..."
iniciar_carga; sleep 5
echo "   NGINX reparte entre las dos instancias:"; resumen_reciente 14

paso "DETENIENDO la instancia $APP  (docker stop $APP)"
docker stop "$APP" >/dev/null
sleep 8
echo "   Peticiones mientras $APP está detenida (todas las atiende la otra):"; resumen_reciente 15

paso "REINICIANDO $APP (docker start $APP)"
docker start "$APP" >/dev/null
esperar_app "$APP"; sleep 12
echo "   $APP vuelve a la rotación automáticamente:"; resumen_reciente 14
detener_carga; sleep 1
resultado_final
