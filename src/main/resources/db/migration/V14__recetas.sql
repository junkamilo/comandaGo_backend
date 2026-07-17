-- Recetas reutilizables + vínculo en productos compuestos

CREATE TABLE IF NOT EXISTS recetas (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre               VARCHAR(150) NOT NULL,
    descripcion          TEXT,
    preparacion          TEXT,
    tiempo_total_min     INT CHECK (tiempo_total_min >= 0),
    porciones            INT CHECK (porciones > 0),
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'trg_recetas_actualizacion'
    ) THEN
        CREATE TRIGGER trg_recetas_actualizacion
            BEFORE UPDATE ON recetas
            FOR EACH ROW EXECUTE FUNCTION set_fecha_actualizacion();
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS receta_ingredientes (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    receta_id         BIGINT NOT NULL REFERENCES recetas(id) ON DELETE CASCADE,
    producto_id       BIGINT NOT NULL REFERENCES productos(id) ON DELETE RESTRICT,
    cantidad          NUMERIC(8,2) NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    unidad            VARCHAR(20) NOT NULL DEFAULT 'UND'
                      CHECK (unidad IN ('UND','GR','ML','KG','LT','PORCION')),
    grupo             VARCHAR(50),
    es_removible      BOOLEAN NOT NULL DEFAULT FALSE,
    es_extra          BOOLEAN NOT NULL DEFAULT FALSE,
    precio_extra      NUMERIC(12,2) CHECK (precio_extra >= 0),
    orden             INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_receta_ingrediente UNIQUE (receta_id, producto_id),
    CONSTRAINT chk_receta_extra_precio CHECK (
        es_extra = FALSE OR precio_extra IS NOT NULL
    )
);

CREATE INDEX IF NOT EXISTS idx_receta_ing_receta ON receta_ingredientes(receta_id);
CREATE INDEX IF NOT EXISTS idx_receta_ing_producto ON receta_ingredientes(producto_id);

ALTER TABLE productos ADD COLUMN IF NOT EXISTS receta_id BIGINT REFERENCES recetas(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_productos_receta ON productos(receta_id);
