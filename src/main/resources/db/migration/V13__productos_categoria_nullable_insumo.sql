-- Categoría opcional solo para insumos (interno vs vendible)

ALTER TABLE productos ALTER COLUMN categoria_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_productos_tipo_categoria'
    ) THEN
        ALTER TABLE productos
            ADD CONSTRAINT chk_productos_tipo_categoria
            CHECK (tipo = 'INSUMO' OR categoria_id IS NOT NULL);
    END IF;
END $$;
