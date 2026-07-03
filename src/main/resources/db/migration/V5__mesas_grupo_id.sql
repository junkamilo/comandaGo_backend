ALTER TABLE mesas ADD COLUMN grupo_id VARCHAR(36);

CREATE INDEX idx_mesas_grupo ON mesas(grupo_id);
