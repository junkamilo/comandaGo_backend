-- Tipo de producto + composición (receta de compuestos)

ALTER TABLE productos ADD COLUMN IF NOT EXISTS tipo VARCHAR(20);

UPDATE productos SET tipo = 'NORMAL' WHERE tipo IS NULL;

ALTER TABLE productos ALTER COLUMN tipo SET DEFAULT 'NORMAL';
ALTER TABLE productos ALTER COLUMN tipo SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_productos_tipo'
    ) THEN
        ALTER TABLE productos
            ADD CONSTRAINT chk_productos_tipo
            CHECK (tipo IN ('COMPUESTO', 'INSUMO', 'NORMAL'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_productos_tipo ON productos(tipo);

CREATE TABLE IF NOT EXISTS producto_insumos (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    producto_compuesto_id  BIGINT NOT NULL REFERENCES productos(id) ON DELETE CASCADE,
    producto_insumo_id     BIGINT NOT NULL REFERENCES productos(id) ON DELETE RESTRICT,
    cantidad               NUMERIC(8,2) NOT NULL DEFAULT 1 CHECK (cantidad > 0),
    unidad                 VARCHAR(20) NOT NULL DEFAULT 'UND'
                           CHECK (unidad IN ('UND','GR','ML','KG','LT','PORCION')),
    es_removible           BOOLEAN NOT NULL DEFAULT TRUE,
    es_extra               BOOLEAN NOT NULL DEFAULT FALSE,
    precio_extra           NUMERIC(12,2) CHECK (precio_extra >= 0),
    orden                  INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_composicion UNIQUE (producto_compuesto_id, producto_insumo_id),
    CONSTRAINT chk_no_autocomposicion
        CHECK (producto_compuesto_id <> producto_insumo_id),
    CONSTRAINT chk_extra_precio
        CHECK (es_extra = FALSE OR precio_extra IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_composicion_compuesto ON producto_insumos(producto_compuesto_id);
CREATE INDEX IF NOT EXISTS idx_composicion_insumo ON producto_insumos(producto_insumo_id);
