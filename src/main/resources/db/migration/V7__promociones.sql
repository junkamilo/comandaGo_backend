-- =====================================================================
-- PROMOCIONES (la regla de descuento)
-- =====================================================================
CREATE TABLE promociones (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre               VARCHAR(150) NOT NULL,
    descripcion          TEXT,

    tipo                 VARCHAR(30) NOT NULL
                         CHECK (tipo IN ('PORCENTAJE','MONTO_FIJO','PAGA_X_LLEVA_Y')),

    valor_porcentaje     NUMERIC(5,2),
    valor_monto          NUMERIC(12,2),
    paga_cantidad        INT,
    lleva_cantidad       INT,

    fecha_inicio         TIMESTAMPTZ NOT NULL,
    fecha_fin            TIMESTAMPTZ,

    uso_maximo           INT,
    uso_actual           INT NOT NULL DEFAULT 0,

    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_promo_porcentaje CHECK (
        tipo <> 'PORCENTAJE'
        OR (valor_porcentaje IS NOT NULL AND valor_porcentaje > 0 AND valor_porcentaje <= 100)
    ),

    CONSTRAINT chk_promo_monto CHECK (
        tipo <> 'MONTO_FIJO'
        OR (valor_monto IS NOT NULL AND valor_monto > 0)
    ),

    CONSTRAINT chk_promo_paga_lleva CHECK (
        tipo <> 'PAGA_X_LLEVA_Y'
        OR (paga_cantidad IS NOT NULL AND lleva_cantidad IS NOT NULL
            AND paga_cantidad > 0 AND lleva_cantidad > paga_cantidad)
    ),

    CONSTRAINT chk_promo_fechas CHECK (
        fecha_fin IS NULL OR fecha_fin > fecha_inicio
    ),

    CONSTRAINT chk_promo_uso CHECK (
        uso_maximo IS NULL OR uso_actual <= uso_maximo
    )
);

CREATE TRIGGER trg_promociones_actualizacion
    BEFORE UPDATE ON promociones
    FOR EACH ROW EXECUTE FUNCTION set_fecha_actualizacion();

CREATE INDEX idx_promociones_activo_fechas ON promociones(activo, fecha_inicio, fecha_fin);

-- =====================================================================
-- PROMOCION_PRODUCTOS (tabla puente: qué productos cubre cada promo)
-- =====================================================================
CREATE TABLE promocion_productos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    promocion_id    BIGINT NOT NULL REFERENCES promociones(id) ON DELETE CASCADE,
    producto_id     BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,

    CONSTRAINT uq_promocion_producto UNIQUE (promocion_id, producto_id)
);

CREATE INDEX idx_promo_prod_promocion ON promocion_productos(promocion_id);
CREATE INDEX idx_promo_prod_producto ON promocion_productos(producto_id);
