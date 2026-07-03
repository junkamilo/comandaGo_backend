ALTER TABLE categorias
    ADD COLUMN IF NOT EXISTS categoria_padre_id BIGINT
        REFERENCES categorias(id) ON DELETE SET NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_categoria_no_autopadre'
    ) THEN
        ALTER TABLE categorias
            ADD CONSTRAINT chk_categoria_no_autopadre
            CHECK (categoria_padre_id IS NULL OR categoria_padre_id <> id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_categorias_padre ON categorias(categoria_padre_id);
