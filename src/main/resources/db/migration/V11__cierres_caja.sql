-- Cierres de caja por turno

CREATE TABLE IF NOT EXISTS cierres_caja (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id            BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE RESTRICT,
    fecha_apertura        TIMESTAMPTZ NOT NULL,
    fecha_cierre          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_efectivo        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_tarjeta         NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_nequi           NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_daviplata       NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_transferencia   NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_otros           NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_propinas        NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_general         NUMERIC(12,2) NOT NULL DEFAULT 0,
    efectivo_contado      NUMERIC(12,2),
    diferencia            NUMERIC(12,2),
    pedidos_atendidos     INT NOT NULL DEFAULT 0,
    pedidos_cancelados    INT NOT NULL DEFAULT 0,
    notas                 TEXT,
    CONSTRAINT chk_cierre_fechas CHECK (fecha_cierre > fecha_apertura)
);

CREATE INDEX IF NOT EXISTS idx_cierres_fecha ON cierres_caja(fecha_cierre);
CREATE INDEX IF NOT EXISTS idx_cierres_usuario ON cierres_caja(usuario_id);
