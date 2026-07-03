-- =====================================================================
-- comandoGo · Esquema inicial (PostgreSQL 14+)
-- Migración: V1__init_comandogo.sql
-- =====================================================================

CREATE OR REPLACE FUNCTION set_fecha_actualizacion()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE usuarios (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre               VARCHAR(100) NOT NULL,
    email                VARCHAR(150) NOT NULL UNIQUE,
    password_hash        VARCHAR(255) NOT NULL,
    telefono             VARCHAR(30),
    rol                  VARCHAR(50) NOT NULL
                         CHECK (rol IN ('ADMIN','MESERO','COCINERO','RECEPCIONISTA','CAJERO')),
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    ultimo_acceso        TIMESTAMPTZ,
    fecha_creacion       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_usuarios_actualizacion
    BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION set_fecha_actualizacion();

CREATE TABLE mesas (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero      VARCHAR(20) NOT NULL UNIQUE,
    nombre      VARCHAR(50),
    capacidad   INT CHECK (capacidad > 0),
    qr_token    VARCHAR(100) UNIQUE,
    estado      VARCHAR(30) NOT NULL DEFAULT 'LIBRE'
                CHECK (estado IN ('LIBRE','OCUPADA','RESERVADA','INACTIVA')),
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categorias (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  TEXT,
    imagen_url   VARCHAR(255),
    orden        INT NOT NULL DEFAULT 0,
    activo       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE productos (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    categoria_id           BIGINT NOT NULL REFERENCES categorias(id) ON DELETE RESTRICT,
    nombre                 VARCHAR(150) NOT NULL,
    descripcion            TEXT,
    precio                 NUMERIC(12,2) NOT NULL CHECK (precio >= 0),
    precio_promocion       NUMERIC(12,2) CHECK (precio_promocion >= 0),
    imagen_url             VARCHAR(255),
    tiempo_preparacion_min INT CHECK (tiempo_preparacion_min >= 0),
    es_promocion           BOOLEAN NOT NULL DEFAULT FALSE,
    disponible             BOOLEAN NOT NULL DEFAULT TRUE,
    activo                 BOOLEAN NOT NULL DEFAULT TRUE,
    orden                  INT NOT NULL DEFAULT 0,
    fecha_creacion         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_promocion
        CHECK (es_promocion = FALSE OR precio_promocion IS NOT NULL)
);

CREATE TRIGGER trg_productos_actualizacion
    BEFORE UPDATE ON productos
    FOR EACH ROW EXECUTE FUNCTION set_fecha_actualizacion();

CREATE TABLE pedidos (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_pedido        VARCHAR(20) NOT NULL UNIQUE,
    usuario_id           BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    mesa_id              BIGINT REFERENCES mesas(id) ON DELETE SET NULL,
    origen               VARCHAR(50) NOT NULL
                         CHECK (origen IN ('MESA_MESERO','MESA_QR','WEB_DOMICILIO')),
    estado               VARCHAR(50) NOT NULL DEFAULT 'POR_CONFIRMAR'
                         CHECK (estado IN ('POR_CONFIRMAR','EN_PREPARACION','LISTO','EN_CAMINO','ENTREGADO','CANCELADO')),
    estado_pago          VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
                         CHECK (estado_pago IN ('PENDIENTE','PARCIAL','PAGADO','REEMBOLSADO')),
    cliente_nombre       VARCHAR(120),
    cliente_telefono     VARCHAR(30),
    direccion_entrega    TEXT,
    subtotal             NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (subtotal  >= 0),
    descuento            NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    impuestos            NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (impuestos >= 0),
    costo_envio          NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (costo_envio >= 0),
    total                NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    notas                TEXT,
    fecha_pedido         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_origen_destino CHECK (
        (origen IN ('MESA_MESERO','MESA_QR') AND mesa_id IS NOT NULL)
        OR
        (origen = 'WEB_DOMICILIO' AND direccion_entrega IS NOT NULL)
    )
);

CREATE TRIGGER trg_pedidos_actualizacion
    BEFORE UPDATE ON pedidos
    FOR EACH ROW EXECUTE FUNCTION set_fecha_actualizacion();

CREATE TABLE detalle_pedidos (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id          BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE CASCADE,
    producto_id        BIGINT NOT NULL REFERENCES productos(id) ON DELETE RESTRICT,
    nombre_producto    VARCHAR(150) NOT NULL,
    cantidad           INT NOT NULL CHECK (cantidad > 0),
    precio_unitario    NUMERIC(12,2) NOT NULL CHECK (precio_unitario >= 0),
    subtotal           NUMERIC(12,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    estado             VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
                       CHECK (estado IN ('PENDIENTE','EN_PREPARACION','LISTO','ENTREGADO','CANCELADO')),
    notas_preparacion  TEXT
);

CREATE TABLE pagos (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pedido_id    BIGINT NOT NULL REFERENCES pedidos(id) ON DELETE RESTRICT,
    usuario_id   BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    metodo       VARCHAR(30) NOT NULL
                 CHECK (metodo IN ('EFECTIVO','TARJETA','TRANSFERENCIA','NEQUI','DAVIPLATA','OTRO')),
    monto        NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    referencia   VARCHAR(100),
    fecha_pago   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION recalcular_total_pedido()
RETURNS TRIGGER AS $$
DECLARE
    v_pedido_id BIGINT := COALESCE(NEW.pedido_id, OLD.pedido_id);
    v_subtotal  NUMERIC(12,2);
BEGIN
    SELECT COALESCE(SUM(subtotal), 0) INTO v_subtotal
    FROM detalle_pedidos
    WHERE pedido_id = v_pedido_id;

    UPDATE pedidos
    SET subtotal = v_subtotal,
        total    = v_subtotal - descuento + impuestos + costo_envio,
        fecha_actualizacion = CURRENT_TIMESTAMP
    WHERE id = v_pedido_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_recalcular_total
    AFTER INSERT OR UPDATE OR DELETE ON detalle_pedidos
    FOR EACH ROW EXECUTE FUNCTION recalcular_total_pedido();

CREATE INDEX idx_productos_categoria   ON productos(categoria_id);
CREATE INDEX idx_productos_activo_disp ON productos(activo, disponible);
CREATE INDEX idx_pedidos_estado        ON pedidos(estado);
CREATE INDEX idx_pedidos_fecha         ON pedidos(fecha_pedido);
CREATE INDEX idx_pedidos_mesa          ON pedidos(mesa_id);
CREATE INDEX idx_detalle_pedido        ON detalle_pedidos(pedido_id);
CREATE INDEX idx_detalle_estado        ON detalle_pedidos(estado);
CREATE INDEX idx_pagos_pedido          ON pagos(pedido_id);
