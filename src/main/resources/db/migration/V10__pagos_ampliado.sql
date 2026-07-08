-- Ampliación de pagos: estado, propina, efectivo/vuelto, PSE

ALTER TABLE pagos ADD COLUMN IF NOT EXISTS estado VARCHAR(30);
ALTER TABLE pagos ADD COLUMN IF NOT EXISTS propina NUMERIC(12,2);
ALTER TABLE pagos ADD COLUMN IF NOT EXISTS monto_recibido NUMERIC(12,2);
ALTER TABLE pagos ADD COLUMN IF NOT EXISTS proveedor_id VARCHAR(100);
ALTER TABLE pagos ADD COLUMN IF NOT EXISTS notas TEXT;

UPDATE pagos SET estado = 'COMPLETADO' WHERE estado IS NULL;
UPDATE pagos SET propina = 0 WHERE propina IS NULL;

ALTER TABLE pagos ALTER COLUMN estado SET DEFAULT 'COMPLETADO';
ALTER TABLE pagos ALTER COLUMN estado SET NOT NULL;
ALTER TABLE pagos ALTER COLUMN propina SET DEFAULT 0;
ALTER TABLE pagos ALTER COLUMN propina SET NOT NULL;

ALTER TABLE pagos ALTER COLUMN referencia TYPE VARCHAR(150);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'pagos' AND column_name = 'vuelto'
    ) THEN
        ALTER TABLE pagos ADD COLUMN vuelto NUMERIC(12,2) GENERATED ALWAYS AS (
            CASE WHEN monto_recibido IS NOT NULL AND monto_recibido > monto + propina
                 THEN monto_recibido - monto - propina
                 ELSE 0
            END
        ) STORED;
    END IF;
END $$;

ALTER TABLE pagos DROP CONSTRAINT IF EXISTS pagos_metodo_check;
ALTER TABLE pagos ADD CONSTRAINT pagos_metodo_check CHECK (
    metodo IN ('EFECTIVO','TARJETA','TRANSFERENCIA','NEQUI','DAVIPLATA','PSE','OTRO')
);

ALTER TABLE pagos DROP CONSTRAINT IF EXISTS pagos_estado_check;
ALTER TABLE pagos ADD CONSTRAINT pagos_estado_check CHECK (
    estado IN ('PENDIENTE','COMPLETADO','RECHAZADO','REEMBOLSADO')
);

ALTER TABLE pagos DROP CONSTRAINT IF EXISTS chk_pago_efectivo;
ALTER TABLE pagos ADD CONSTRAINT chk_pago_efectivo CHECK (
    metodo <> 'EFECTIVO' OR monto_recibido IS NOT NULL
);

ALTER TABLE pagos DROP CONSTRAINT IF EXISTS chk_pago_propina;
ALTER TABLE pagos ADD CONSTRAINT chk_pago_propina CHECK (propina <= monto);

CREATE INDEX IF NOT EXISTS idx_pagos_estado ON pagos(estado);
CREATE INDEX IF NOT EXISTS idx_pagos_fecha ON pagos(fecha_pago);
CREATE INDEX IF NOT EXISTS idx_pagos_metodo ON pagos(metodo);
CREATE INDEX IF NOT EXISTS idx_pagos_usuario ON pagos(usuario_id);
