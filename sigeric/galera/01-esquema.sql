-- Esquema de SIGERIC. Se ejecuta UNA sola vez en db1 al crear el clúster;
-- Galera lo replica automáticamente a db2 y db3.
-- Nota Galera: todas las tablas deben ser InnoDB y tener PRIMARY KEY.
USE sigeric;

CREATE TABLE IF NOT EXISTS usuario (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  nombre     VARCHAR(120) NOT NULL,
  email      VARCHAR(120) NOT NULL UNIQUE,
  password   VARCHAR(100) NOT NULL,
  rol        VARCHAR(20)  NOT NULL,           -- CIUDADANO | ADESCO | MUNICIPAL
  municipio  VARCHAR(80)  NOT NULL,
  creado     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS reporte (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  codigo       VARCHAR(20)  NOT NULL UNIQUE,
  categoria    VARCHAR(20)  NOT NULL,         -- BACHE | ALUMBRADO | FUGA | OTRO
  titulo       VARCHAR(150) NOT NULL,
  descripcion  TEXT         NOT NULL,
  direccion    VARCHAR(200) NOT NULL,
  municipio    VARCHAR(80)  NOT NULL,
  estado       VARCHAR(20)  NOT NULL,         -- RECIBIDO | EN_REVISION | EN_PROCESO | RESUELTO | RECHAZADO
  prioridad    VARCHAR(10)  NOT NULL,         -- BAJA | MEDIA | ALTA
  autor_id     BIGINT       NOT NULL,
  creado       DATETIME     NOT NULL,
  actualizado  DATETIME     NOT NULL,
  nodo_app     VARCHAR(40),                   -- instancia de Spring Boot que lo registró
  nodo_bd      VARCHAR(40),                   -- nodo Galera que recibió la escritura
  CONSTRAINT fk_reporte_autor FOREIGN KEY (autor_id) REFERENCES usuario(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS historial_estado (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  reporte_id      BIGINT      NOT NULL,
  estado_anterior VARCHAR(20),
  estado_nuevo    VARCHAR(20) NOT NULL,
  comentario      VARCHAR(500),
  usuario_id      BIGINT      NOT NULL,
  fecha           DATETIME    NOT NULL,
  nodo_app        VARCHAR(40),
  nodo_bd         VARCHAR(40),
  CONSTRAINT fk_hist_reporte FOREIGN KEY (reporte_id) REFERENCES reporte(id),
  CONSTRAINT fk_hist_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
) ENGINE=InnoDB;

-- Tabla de "latidos": la usan los scripts de prueba de alta disponibilidad
-- para escribir continuamente mientras se apagan nodos.
CREATE TABLE IF NOT EXISTS latido (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  nodo_app  VARCHAR(40) NOT NULL,
  nodo_bd   VARCHAR(40) NOT NULL,
  fecha     DATETIME(3) NOT NULL
) ENGINE=InnoDB;

-- Sesiones HTTP compartidas (Spring Session JDBC): si se cae una instancia de la app,
-- la otra conoce la sesión y el usuario NO pierde su login.
CREATE TABLE IF NOT EXISTS SPRING_SESSION (
  PRIMARY_ID            CHAR(36)     NOT NULL,
  SESSION_ID            CHAR(36)     NOT NULL,
  CREATION_TIME         BIGINT       NOT NULL,
  LAST_ACCESS_TIME      BIGINT       NOT NULL,
  MAX_INACTIVE_INTERVAL INT          NOT NULL,
  EXPIRY_TIME           BIGINT       NOT NULL,
  PRINCIPAL_NAME        VARCHAR(100),
  CONSTRAINT SPRING_SESSION_PK PRIMARY KEY (PRIMARY_ID)
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;
CREATE UNIQUE INDEX SPRING_SESSION_IX1 ON SPRING_SESSION (SESSION_ID);
CREATE INDEX SPRING_SESSION_IX2 ON SPRING_SESSION (EXPIRY_TIME);
CREATE INDEX SPRING_SESSION_IX3 ON SPRING_SESSION (PRINCIPAL_NAME);

CREATE TABLE IF NOT EXISTS SPRING_SESSION_ATTRIBUTES (
  SESSION_PRIMARY_ID CHAR(36)     NOT NULL,
  ATTRIBUTE_NAME     VARCHAR(200) NOT NULL,
  ATTRIBUTE_BYTES    MEDIUMBLOB   NOT NULL,
  CONSTRAINT SPRING_SESSION_ATTRIBUTES_PK PRIMARY KEY (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
  CONSTRAINT SPRING_SESSION_ATTRIBUTES_FK FOREIGN KEY (SESSION_PRIMARY_ID)
    REFERENCES SPRING_SESSION (PRIMARY_ID) ON DELETE CASCADE
) ENGINE=InnoDB ROW_FORMAT=DYNAMIC;

-- Usuarios de demostración (contraseña de todos: sigeric123)
INSERT INTO usuario (nombre, email, password, rol, municipio) VALUES
 ('Ana Ciudadana',      'ciudadano@sigeric.sv', '$2a$10$BsgS4iduLkbD.TG5d3r2VeIzaAmqZJr6bb12Qp30KQUqDTGbYJuWC', 'CIUDADANO', 'San Salvador Centro'),
 ('Carlos ADESCO',      'adesco@sigeric.sv',    '$2a$10$BsgS4iduLkbD.TG5d3r2VeIzaAmqZJr6bb12Qp30KQUqDTGbYJuWC', 'ADESCO',    'San Salvador Centro'),
 ('Marta Municipal',    'municipal@sigeric.sv', '$2a$10$BsgS4iduLkbD.TG5d3r2VeIzaAmqZJr6bb12Qp30KQUqDTGbYJuWC', 'MUNICIPAL', 'San Salvador Centro');

INSERT INTO reporte (codigo, categoria, titulo, descripcion, direccion, municipio, estado, prioridad, autor_id, creado, actualizado, nodo_app, nodo_bd) VALUES
 ('SIG-2026-A1B2C3', 'BACHE',     'Bache profundo frente a escuela', 'Bache de aprox. 1 metro que ya causó accidentes de motos.', 'Calle Arce y 19 Av. Norte', 'San Salvador Centro', 'EN_PROCESO', 'ALTA',  1, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 1 DAY, 'semilla', 'db1'),
 ('SIG-2026-D4E5F6', 'ALUMBRADO', 'Tres luminarias apagadas',        'Pasaje completamente oscuro desde hace dos semanas.',          'Col. Escalón, Pasaje 3',    'San Salvador Centro', 'RECIBIDO',   'MEDIA', 1, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY, 'semilla', 'db1'),
 ('SIG-2026-G7H8J9', 'FUGA',      'Fuga de agua potable en acera',   'Sale agua limpia de la acera día y noche.',                    'Av. Bernal #45',            'San Salvador Centro', 'RESUELTO',   'ALTA',  2, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 3 DAY, 'semilla', 'db1');

INSERT INTO historial_estado (reporte_id, estado_anterior, estado_nuevo, comentario, usuario_id, fecha, nodo_app, nodo_bd) VALUES
 (1, NULL, 'RECIBIDO', 'Reporte creado', 1, NOW() - INTERVAL 5 DAY, 'semilla', 'db1'),
 (1, 'RECIBIDO', 'EN_PROCESO', 'Cuadrilla asignada', 3, NOW() - INTERVAL 1 DAY, 'semilla', 'db1'),
 (2, NULL, 'RECIBIDO', 'Reporte creado', 1, NOW() - INTERVAL 2 DAY, 'semilla', 'db1'),
 (3, NULL, 'RECIBIDO', 'Reporte creado', 2, NOW() - INTERVAL 9 DAY, 'semilla', 'db1'),
 (3, 'RECIBIDO', 'RESUELTO', 'Tubería reparada por ANDA', 3, NOW() - INTERVAL 3 DAY, 'semilla', 'db1');
