-- Tipo PRECIO_FIJO: precio final directo de la promoción (distinto de MONTO_FIJO que es descuento)

ALTER TABLE promociones
    ADD COLUMN valor_precio NUMERIC(12, 2);

ALTER TABLE promociones
    DROP CONSTRAINT IF EXISTS promociones_tipo_check;

ALTER TABLE promociones
    ADD CONSTRAINT promociones_tipo_check
        CHECK (tipo IN ('PORCENTAJE', 'MONTO_FIJO', 'PRECIO_FIJO', 'PAGA_X_LLEVA_Y'));

ALTER TABLE promociones
    ADD CONSTRAINT chk_promo_precio_fijo CHECK (
        tipo <> 'PRECIO_FIJO'
        OR (valor_precio IS NOT NULL AND valor_precio > 0)
    );
