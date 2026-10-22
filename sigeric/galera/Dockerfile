# Imagen del nodo Galera de SIGERIC.
# La configuración se COPIA dentro de la imagen (en vez de montarla) para que funcione igual
# en Linux y en Windows: en Windows los archivos montados aparecen como "escribibles por todos"
# y MariaDB ignora por seguridad los .cnf con esos permisos. También se eliminan posibles
# saltos de línea CRLF de Windows.
FROM mariadb:11.4
COPY galera.cnf /etc/mysql/conf.d/galera.cnf
COPY arranque-nodo.sh /usr/local/bin/arranque-nodo.sh
COPY 01-esquema.sql /sigeric/01-esquema.sql
RUN sed -i 's/\r$//' /etc/mysql/conf.d/galera.cnf /usr/local/bin/arranque-nodo.sh /sigeric/01-esquema.sql \
 && chmod 644 /etc/mysql/conf.d/galera.cnf /sigeric/01-esquema.sql \
 && chmod 755 /usr/local/bin/arranque-nodo.sh
ENTRYPOINT ["/usr/local/bin/arranque-nodo.sh"]
