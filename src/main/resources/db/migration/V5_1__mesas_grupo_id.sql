ALTER TABLE mesas ADD COLUMN IF NOT EXISTS grupo_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_mesas_grupo ON mesas(grupo_id);
