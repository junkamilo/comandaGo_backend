-- Auditoría ligera en usuarios

ALTER TABLE usuarios
    ADD COLUMN creado_por_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL,
    ADD COLUMN modificado_por_id BIGINT REFERENCES usuarios(id) ON DELETE SET NULL;
