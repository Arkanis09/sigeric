# SIGERIC: Sistema de Gestión de Reportes e Infraestructura Comunitaria

Sistema web para reportar **baches, alumbrado público y fugas de agua**, darles seguimiento con un
código único y gestionarlos entre ciudadanía, ADESCO y la alcaldía. Está construido con
**alta disponibilidad a nivel de base de datos y a nivel de aplicación**.

- **Backend y frontend:** Spring Boot 3.5 (Java 21) + Thymeleaf. Todo en Spring Boot, sin Angular.
- **Base de datos:** clúster **MariaDB Galera de 3 nodos**, multi-maestro (los 3 leen y escriben).
- **Aplicación:** **2 instancias idénticas** de Spring Boot, activas a la vez.
- **Balanceador:** **NGINX**, con reintento automático en la otra instancia.
- **Sesiones compartidas:** Spring Session JDBC (si se cae una instancia, el usuario no pierde su login).
- **Todo en Docker:** un solo comando lo levanta, en **Linux (CachyOS) o Windows**.

```
                         ┌──────────────────────────┐
   Navegador ──:8080──▶  │  NGINX (balanceador)     │
                         └──────┬───────────┬───────┘
                                │           │   si una instancia no responde,
                         ┌──────▼───┐ ┌─────▼────┐  NGINX reenvía la petición a la otra
                         │  app1    │ │  app2    │  (Spring Boot, sesiones en la BD)
                         └──┬───────┘ └───────┬──┘
           driver MariaDB   │  conoce los 3   │  si su nodo cae, se pasa al siguiente
           modo "sequential"│     nodos       │  y repite la transacción en curso
                    ┌───────▼─────────────────▼───────┐
                    │   Clúster MariaDB Galera        │
                    │   db1  ◀──▶  db2  ◀──▶  db3     │  replicación síncrona,
                    │   (los 3 aceptan escrituras)    │  quórum = mayoría (2 de 3)
                    └─────────────────────────────────┘
```

---

## 1. Instalar Docker (solo la primera vez)

Funciona igual en **Linux (CachyOS)** y en **Windows**. Lo único que hace falta es Docker.
No hay que instalar Java, Maven ni MariaDB: todo se compila y se ejecuta dentro de contenedores.

### CachyOS / Arch Linux

```bash
sudo pacman -S --needed docker docker-compose docker-buildx git
sudo systemctl enable --now docker
sudo usermod -aG docker $USER
```

**Cierra sesión y vuelve a entrar** (o reinicia) para poder usar `docker` sin `sudo`.
Para comprobarlo: `docker run --rm hello-world`.

> CachyOS usa **fish** como terminal por defecto. Los scripts son de bash, pero se ejecutan igual
> con `./scripts/nombre.sh`.

### Windows 10/11

1. Instalar **Docker Desktop**: https://www.docker.com/products/docker-desktop/ (deja marcada la
   opción **"Use WSL 2"**). Reinicia si te lo pide y **abre Docker Desktop** (debe quedar en verde, "Engine running").
2. Instalar **Git for Windows**: https://git-scm.com/download/win (solo si van a clonar desde GitHub).
3. Recomendado: en Docker Desktop, *Settings → Resources*, darle **al menos 4 GB de RAM**.

Comprobar en PowerShell: `docker run --rm hello-world`.

> El proyecto ya trae todo lo necesario para Windows: `.gitattributes` evita que Git convierta
> los scripts a CRLF, y la configuración de Galera va dentro de su propia imagen, así que los
> permisos de archivos de Windows no afectan.

## 2. Levantar el sistema

En la carpeta del proyecto (terminal de Linux o **PowerShell** en Windows):

```bash
docker compose up -d --build
```

La primera vez tarda unos minutos porque descarga las imágenes y las dependencias de Maven.
Las siguientes veces arranca en menos de un minuto. Cuando termine, abre **http://localhost:8080**.

Para ver el estado de todo:

| Linux | Windows (PowerShell) | Windows (doble clic) |
|---|---|---|
| `./scripts/estado.sh` | `powershell -ExecutionPolicy Bypass -File .\scripts\windows\estado.ps1` | `scripts\windows\estado.bat` |

### Usuarios de demostración (contraseña: `sigeric123`)

| Usuario                | Rol                | Qué puede hacer                                      |
|------------------------|--------------------|------------------------------------------------------|
| `ciudadano@sigeric.sv` | Ciudadano          | Crear reportes y ver los suyos                       |
| `adesco@sigeric.sv`    | ADESCO             | Ver los reportes de la comunidad, validarlos o rechazarlos |
| `municipal@sigeric.sv` | Personal municipal | Gestionar el ciclo completo, prioridad y panel        |

Cualquier persona puede consultar el avance de un reporte, sin iniciar sesión, en **Consultar reporte**.

---

## 3. Probar la alta disponibilidad

### Pruebas automáticas (evidencia para el documento)

Cada script manda **una escritura cada 0.2 s** a través del balanceador, apaga un componente,
lo vuelve a encender y al final cuenta cuántas escrituras fallaron. También compara que los
3 nodos tengan exactamente los mismos datos.

| Prueba | Linux | Windows (doble clic en `scripts\windows\`) |
|---|---|---|
| 1. Se desconecta un nodo de BD (db1) | `./scripts/prueba-ha-bd.sh` | `prueba-ha-bd.bat` |
| 2. Se detiene una instancia de la app (app1) | `./scripts/prueba-ha-app.sh` | `prueba-ha-app.bat` |
| 3. Caen db1 y app1 **al mismo tiempo** | `./scripts/prueba-ha-total.sh` | `prueba-ha-total.bat` |

En Linux se puede elegir el nodo: `./scripts/prueba-ha-bd.sh db2`. En PowerShell:
`powershell -ExecutionPolicy Bypass -File .\scripts\windows\prueba-ha-bd.ps1 -Nodo db2`
(o `-App app2` para la prueba de aplicación).

Resultado esperado: `Escrituras fallidas: 0` y `PRUEBA SUPERADA`.
En la carpeta `evidencias/` está la salida de nuestras corridas, incluidas dos pruebas extra:
apagado brusco (`docker kill`) y desconexión de red de un nodo.

### Prueba visual en vivo (ideal para la sustentación)

1. Abre **http://localhost:8080/estado** y marca *"Escribir un registro de prueba en cada consulta"*.
2. En una terminal (Linux o PowerShell, los comandos son iguales), apaga lo que quieras:
   ```bash
   docker stop db1          # apagar un nodo de base de datos
   docker stop app1         # apagar una instancia de la aplicación
   docker kill db2          # apagado brusco (como un corte de luz)
   docker network disconnect sigeric_sigeric db3   # "desconectar el cable" de un nodo
   ```
3. En la página se ve el clúster pasar de **3/3 a 2/3**, la instancia caída en rojo, y que
   **las peticiones y escrituras siguen saliendo bien** (contador de fallidas en 0).
4. Para recuperar:
   ```bash
   docker start db1 app1 db2
   docker network connect --ip 172.28.50.13 sigeric_sigeric db3
   ```
   Los nodos que vuelven **se sincronizan solos** con lo que se escribió mientras estaban caídos.

También puedes iniciar sesión, apagar la instancia que te está atendiendo (se ve en el pie de
página: `App: app1 · BD: db1`) y recargar: **sigues con la sesión iniciada**, ahora atendido por la otra.

### ¿Por qué no es "sistema 1 falla, pasamos al 2"?

- En la base de datos **no hay principal ni respaldo**. Los 3 nodos Galera son iguales, todos aceptan
  lecturas y escrituras, y cada transacción se confirma en el clúster completo antes de responder
  (replicación síncrona). Es **un solo sistema con 3 copias vivas**.
- Hay **3 nodos y no 2** por el **quórum**. Si un nodo queda aislado, él solo es minoría (1 de 3) y
  deja de aceptar escrituras, mientras los otros 2 (mayoría) siguen. Así nunca hay "dos cerebros"
  escribiendo datos distintos. Con `docker network disconnect` se puede ver: el nodo aislado
  reporta un clúster de 1 y se bloquea solo.
- En la aplicación, **las 2 instancias atienden al mismo tiempo**. NGINX reparte las peticiones y, si una
  no responde, reenvía esa misma petición a la otra sin que el usuario vea un error.

---

- **¿Y si caen 2 de los 3 nodos?** El que queda deja de aceptar escrituras a propósito, porque sin
  mayoría no puede descartar que los otros sigan vivos del otro lado de un corte de red. En cuanto
  vuelve uno, el clúster se reactiva solo con el nodo que tenga los datos más recientes. Con 3 nodos se
  tolera 1 caído; para tolerar 2 harían falta 5 (regla `2f + 1`).

> 📄 Explicación completa, tabla de fallas, preguntas difíciles y guion de la demostración:
> `docs/SIGERIC - Manual y guia de sustentacion.docx`

## 4. Ver las bases de datos

Cada nodo del clúster tiene su propio puerto en la PC, para revisarlos **por separado**:

| Nodo | Host | Puerto | Usuario | Contraseña | Base |
|---|---|---|---|---|---|
| db1 | 127.0.0.1 | **3307** | root | RootSigeric2026 | sigeric |
| db2 | 127.0.0.1 | **3308** | root | RootSigeric2026 | sigeric |
| db3 | 127.0.0.1 | **3309** | root | RootSigeric2026 | sigeric |

**Programas:** HeidiSQL (Windows, https://www.heidisql.com/) o DBeaver Community (Linux y Windows,
https://dbeaver.io/). Tipo de conexión: MariaDB. Creen **una conexión por nodo**.

**Sin instalar nada**, desde la terminal:
```bash
docker exec -it db1 mariadb -uroot -pRootSigeric2026 sigeric
```

Consultas útiles para la demostración:
```sql
SHOW STATUS LIKE 'wsrep_cluster_size';      -- cuántos nodos hay en el clúster (3)
SHOW STATUS LIKE 'wsrep_cluster_status';    -- Primary = tiene quórum y acepta escrituras
SHOW STATUS LIKE 'wsrep_local_state_comment'; -- Synced = al día con los demás
SELECT @@wsrep_node_name;                   -- en qué nodo estoy conectado
SELECT codigo, titulo, estado, nodo_app, nodo_bd FROM reporte;  -- quién registró cada reporte
SELECT nodo_bd, COUNT(*) FROM latido GROUP BY nodo_bd;          -- escrituras de prueba por nodo
```

**Demostración de replicación:** inserten un registro conectados a db1 y consúltenlo en db2 y db3:
aparece al instante. Luego `docker stop db1`, inserten en db2, `docker start db1` y consulten en db1:
el registro que se insertó mientras estaba apagado ya está ahí.

## 5. Comandos útiles

```bash
docker compose ps                    # estado de los contenedores
docker compose logs -f app1          # logs de una instancia
docker logs db2 | grep '>>>'         # cómo arrancó cada nodo Galera
docker compose down                  # apagar todo (los datos se conservan)
docker compose up -d                 # volver a encender
docker compose down -v               # BORRAR todo, incluida la base de datos, y empezar de cero
docker compose up -d --build         # recompilar la app después de cambiar código
```

**Arranque automático del clúster:** si se apaga todo (con `down`, al reiniciar la PC o por un corte
de luz), al encender, los nodos eligen solos al que tiene los datos más recientes para levantar el
clúster, y los demás se unen a él. No hace falta hacer nada a mano.

## 6. Estructura del proyecto

```
sigeric/
├── docker-compose.yml        # 3 nodos Galera + 2 apps + NGINX
├── galera/
│   ├── Dockerfile            # imagen del nodo (incluye la configuración)
│   ├── galera.cnf            # configuración del clúster
│   ├── arranque-nodo.sh      # lógica de arranque/recuperación automática de cada nodo
│   └── 01-esquema.sql        # tablas + datos de demostración
├── nginx/nginx.conf          # balanceador con failover
├── app/                      # aplicación Spring Boot (Maven)
│   ├── Dockerfile
│   └── src/main/java/sv/ufg/sigeric/
│       ├── model/  repo/  service/  web/  api/  config/
│       └── ../resources/templates/   # vistas Thymeleaf
├── docs/                     # documento del proyecto, manual/guía de sustentación (Word), diagramas y capturas
├── scripts/                  # pruebas de alta disponibilidad (bash, Linux)
│   └── windows/              # las mismas pruebas en PowerShell + .bat (Windows)
└── evidencias/               # salida de las pruebas
```

### Desarrollar en el IDE (opcional)

Si quieren abrir `app/` en IntelliJ o VS Code y correrla fuera de Docker, instalen
`sudo pacman -S jdk21-openjdk maven` y levanten solo la base de datos con
`docker compose up -d db1 db2 db3`. Para conectarse desde el host habría que publicar el puerto
3306 de un nodo. Lo más simple es seguir usando `docker compose up -d --build app1 app2`.

## 7. Subirlo a GitHub

GitHub sirve para **guardar y compartir el código**. Cada integrante lo clona y lo levanta con Docker
en su PC, y en la PC de la demostración se hace lo mismo.

**Una sola vez (quien suba el proyecto):**

1. En https://github.com/new crear un repositorio vacío, por ejemplo `sigeric`, **sin** README ni .gitignore.
2. En la carpeta del proyecto:
   ```bash
   git init -b main
   git add .
   git commit -m "SIGERIC: sistema con alta disponibilidad (Galera + 2 instancias + NGINX)"
   git remote add origin https://github.com/USUARIO/sigeric.git
   git push -u origin main
   ```
   GitHub pide usuario y, como contraseña, un **token** (GitHub → Settings → Developer settings →
   Personal access tokens). También se puede usar `gh auth login` si tienen GitHub CLI.

**En cualquier otra PC (Linux o Windows):**

```bash
git clone https://github.com/USUARIO/sigeric.git
cd sigeric
docker compose up -d --build
```

### Publicarlo en internet con un link (opcional, gratis)

Para la demostración **no hace falta**: la alta disponibilidad se prueba igual en la PC. Pero si quieren
que alguien entre desde otra red, un túnel de Cloudflare da un link público `https://...trycloudflare.com`
mientras la PC esté encendida y el sistema corriendo. No necesita cuenta.

```bash
# Instalar (una vez)
sudo pacman -S cloudflared                       # CachyOS
winget install --id Cloudflare.cloudflared       # Windows (luego cierra y abre PowerShell)

# Publicar (déjalo abierto; Ctrl+C para cerrar el link)
cloudflared tunnel --url http://localhost:8080
```

Copia el link `https://xxxx.trycloudflare.com` que aparece en la salida. Las pruebas de alta disponibilidad
(`docker stop db1`, etc.) se ven igual desde ese link, en `/estado`.

**¿Y GitHub Pages?** GitHub Pages **solo sirve archivos estáticos** (HTML, CSS, JS). No puede ejecutar
Java, Spring Boot, MariaDB, NGINX ni Docker, así que no puede alojar este sistema. Para la demostración,
el sistema corre con Docker en la PC de la presentación. Si alguien necesita entrar desde otra máquina
de la misma red, se usa `http://IP-DE-LA-PC:8080`.

## 8. Problemas comunes

| Problema | Solución |
|---|---|
| `permission denied ... docker.sock` (Linux) | Falta cerrar sesión después de `usermod -aG docker`, o usar `sudo` |
| `error during connect ... pipe/dockerDesktopLinuxEngine` (Windows) | Docker Desktop no está abierto: ábrelo y espera a que diga "Engine running" |
| "la ejecución de scripts está deshabilitada" (Windows) | Usa los `.bat` o `powershell -ExecutionPolicy Bypass -File ...` |
| Muy lento o se cierra un contenedor (Windows) | Docker Desktop → Settings → Resources: asignar 4 GB de RAM o más |
| El puerto 8080 está ocupado | En `docker-compose.yml`, cambiar `"8080:80"` por `"8081:80"` |
| `toomanyrequests` al descargar imágenes | Límite de Docker Hub: esperar unos minutos o hacer `docker login` |
| Un nodo no arranca | `docker logs db1` y revisar las líneas `>>>` |
| Puerto 3307/3308/3309 ocupado | En `docker-compose.yml` cambiar `"3307:3306"` por otro, p. ej. `"13307:3306"` |
| Quiero empezar de cero | `docker compose down -v && docker compose up -d --build` |
