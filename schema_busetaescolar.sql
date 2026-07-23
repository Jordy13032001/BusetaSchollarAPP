-- =========================================================
-- Base de datos: BusetaEscolarApp
-- Motor: PostgreSQL 14+
-- =========================================================

-- ---------- TIPOS ENUM ----------
CREATE TYPE rol_usuario AS ENUM ('padre', 'chofer', 'admin');
CREATE TYPE turno_ruta AS ENUM ('MANANA', 'TARDE');
CREATE TYPE estado_viaje AS ENUM ('PROGRAMADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO');
CREATE TYPE tipo_incidente AS ENUM ('TRAFICO', 'ACCIDENTE', 'RETRASO', 'OTRO');
CREATE TYPE estado_incidente AS ENUM ('ABIERTO', 'RESUELTO');
CREATE TYPE tipo_notificacion AS ENUM ('CERCA', 'SUBIO', 'FINALIZADA', 'ALERTA');
CREATE TYPE parentesco_tipo AS ENUM ('MADRE', 'PADRE', 'TUTOR');

-- ---------- USUARIOS ----------
-- Nota: el correo es el identificador de login real de la app (no se pide un
-- nombre de usuario aparte), por eso no existe una columna nombre_usuario.
CREATE TABLE usuarios (
    id_usuario      SERIAL PRIMARY KEY,
    password_hash   VARCHAR(255) NOT NULL,
    rol             rol_usuario NOT NULL,
    nombre_completo VARCHAR(120) NOT NULL,
    correo          VARCHAR(120) NOT NULL UNIQUE,
    telefono        VARCHAR(20),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- COLEGIOS ----------
CREATE TABLE colegios (
    id_colegio  SERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL,
    direccion   VARCHAR(200),
    lat         DOUBLE PRECISION,
    lng         DOUBLE PRECISION
);

-- ---------- PERFIL CHOFER (1:1 con usuarios) ----------
CREATE TABLE perfil_chofer (
    id_chofer       INTEGER PRIMARY KEY REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    licencia        VARCHAR(30),
    foto_url        VARCHAR(255),
    -- Reemplaza al users.price del esquema provisional: tarifa que ve el padre
    -- al contratar un chofer (AddChildActivity).
    tarifa_mensual  DECIMAL(10, 2) NOT NULL DEFAULT 50.00
);

-- ---------- PERFIL PADRE (1:1 con usuarios) ----------
CREATE TABLE perfil_padre (
    id_padre    INTEGER PRIMARY KEY REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    foto_url    VARCHAR(255)
);

-- ---------- BUSES ----------
CREATE TABLE buses (
    id_bus              SERIAL PRIMARY KEY,
    placa               VARCHAR(15) NOT NULL UNIQUE,
    modelo              VARCHAR(60),
    capacidad           INTEGER NOT NULL,
    id_chofer_asignado  INTEGER REFERENCES perfil_chofer(id_chofer)
);

-- ---------- RUTAS ----------
CREATE TABLE rutas (
    id_ruta             SERIAL PRIMARY KEY,
    nombre              VARCHAR(80) NOT NULL,       -- Ej: "Ruta MaÃ±ana"
    turno               turno_ruta NOT NULL,
    id_colegio          INTEGER NOT NULL REFERENCES colegios(id_colegio),
    id_chofer           INTEGER REFERENCES perfil_chofer(id_chofer),
    id_bus              INTEGER REFERENCES buses(id_bus),
    hora_salida_estimada TIME
);

-- ---------- PARADAS ----------
CREATE TABLE paradas (
    id_parada       SERIAL PRIMARY KEY,
    id_ruta         INTEGER NOT NULL REFERENCES rutas(id_ruta) ON DELETE CASCADE,
    orden           INTEGER NOT NULL,
    nombre          VARCHAR(150) NOT NULL,   -- Ej: "Casa de Juan PÃ©rez"
    hora_estimada   TIME,
    -- Nullable: el padre puede escribir la direcciÃ³n a mano sin usar el
    -- selector de mapa, en cuyo caso no hay coordenadas todavÃ­a.
    lat             DOUBLE PRECISION,
    lng             DOUBLE PRECISION,
    UNIQUE (id_ruta, orden)
);

-- ---------- ESTUDIANTES ----------
CREATE TABLE estudiantes (
    id_estudiante   SERIAL PRIMARY KEY,
    nombre_completo VARCHAR(120) NOT NULL,
    fecha_nacimiento DATE,
    foto_url        VARCHAR(255),
    grado           VARCHAR(30),
    id_colegio      INTEGER NOT NULL REFERENCES colegios(id_colegio),
    id_ruta         INTEGER REFERENCES rutas(id_ruta),
    id_parada       INTEGER REFERENCES paradas(id_parada)
);

-- ---------- PADRES <-> ESTUDIANTES (N:M) ----------
CREATE TABLE padres_estudiantes (
    id_padre        INTEGER NOT NULL REFERENCES perfil_padre(id_padre) ON DELETE CASCADE,
    id_estudiante   INTEGER NOT NULL REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    parentesco      parentesco_tipo NOT NULL,
    PRIMARY KEY (id_padre, id_estudiante)
);

-- ---------- VIAJES (instancia diaria de una ruta) ----------
CREATE TABLE viajes (
    id_viaje        SERIAL PRIMARY KEY,
    id_ruta         INTEGER NOT NULL REFERENCES rutas(id_ruta),
    fecha           DATE NOT NULL,
    hora_inicio     TIMESTAMP,
    hora_fin        TIMESTAMP,
    estado          estado_viaje NOT NULL DEFAULT 'PROGRAMADO',
    -- Antes incluÃ­a hora_inicio, pero esa columna nace en NULL y Postgres no
    -- aplica unicidad entre NULLs: permitÃ­a crear varios viajes del mismo dÃ­a.
    UNIQUE (id_ruta, fecha)
);

-- ---------- ASISTENCIAS ----------
CREATE TABLE asistencias (
    id_asistencia   SERIAL PRIMARY KEY,
    id_viaje        INTEGER NOT NULL REFERENCES viajes(id_viaje) ON DELETE CASCADE,
    id_estudiante   INTEGER NOT NULL REFERENCES estudiantes(id_estudiante),
    subio           BOOLEAN NOT NULL DEFAULT FALSE,
    hora_registro   TIMESTAMP NOT NULL DEFAULT NOW(),
    motivo          VARCHAR(100),      -- Ej: "Enfermedad" (solo aplica si subio = false)
    observacion     TEXT,
    UNIQUE (id_viaje, id_estudiante)
);

-- ---------- INCIDENTES ----------
-- Antes exigÃ­a id_chofer NOT NULL, pero hoy los incidentes tambiÃ©n los
-- reporta el padre (texto libre, ver IncidenteActivity.kt) y no siempre estÃ¡n
-- ligados a un chofer/viaje. id_usuario_reporta identifica a quien reporta
-- (padre o chofer); id_chofer queda opcional para cuando aplica.
CREATE TABLE incidentes (
    id_incidente        SERIAL PRIMARY KEY,
    id_viaje            INTEGER REFERENCES viajes(id_viaje),
    id_chofer           INTEGER REFERENCES perfil_chofer(id_chofer),
    id_usuario_reporta  INTEGER NOT NULL REFERENCES usuarios(id_usuario),
    tipo                tipo_incidente NOT NULL,
    mensaje             TEXT NOT NULL,
    lat                 DOUBLE PRECISION,
    lng                 DOUBLE PRECISION,
    estado              estado_incidente NOT NULL DEFAULT 'ABIERTO',
    fecha_hora          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- NOTIFICACIONES ----------
CREATE TABLE notificaciones (
    id_notificacion     SERIAL PRIMARY KEY,
    id_usuario_destino  INTEGER NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    id_viaje            INTEGER REFERENCES viajes(id_viaje),
    titulo              VARCHAR(120) NOT NULL,
    mensaje             VARCHAR(255) NOT NULL,
    tipo                tipo_notificacion NOT NULL,
    leida               BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_hora          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- UBICACIONES EN VIVO DEL BUS (alta frecuencia de escritura) ----------
CREATE TABLE ubicaciones_bus (
    id_ubicacion    BIGSERIAL PRIMARY KEY,
    id_viaje        INTEGER NOT NULL REFERENCES viajes(id_viaje) ON DELETE CASCADE,
    lat             DOUBLE PRECISION NOT NULL,
    lng             DOUBLE PRECISION NOT NULL,
    fecha_hora      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ---------- Ã�NDICES ----------
CREATE INDEX idx_estudiantes_ruta ON estudiantes(id_ruta);
CREATE INDEX idx_estudiantes_parada ON estudiantes(id_parada);
CREATE INDEX idx_paradas_ruta ON paradas(id_ruta);
CREATE INDEX idx_viajes_ruta_fecha ON viajes(id_ruta, fecha);
CREATE INDEX idx_asistencias_viaje ON asistencias(id_viaje);
CREATE INDEX idx_asistencias_estudiante ON asistencias(id_estudiante);
CREATE INDEX idx_incidentes_chofer ON incidentes(id_chofer);
CREATE INDEX idx_incidentes_viaje ON incidentes(id_viaje);
CREATE INDEX idx_incidentes_usuario_reporta ON incidentes(id_usuario_reporta);
CREATE INDEX idx_notificaciones_usuario ON notificaciones(id_usuario_destino, leida);
CREATE INDEX idx_ubicaciones_viaje_fecha ON ubicaciones_bus(id_viaje, fecha_hora);
