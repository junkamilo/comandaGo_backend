CREATE OR REPLACE FUNCTION recalcular_total_pedido()
RETURNS TRIGGER AS $$
DECLARE
    v_pedido_id BIGINT := COALESCE(NEW.pedido_id, OLD.pedido_id);
    v_subtotal  NUMERIC(12,2);
BEGIN
    SELECT COALESCE(SUM(subtotal), 0) INTO v_subtotal
    FROM detalle_pedidos
    WHERE pedido_id = v_pedido_id
      AND estado <> 'CANCELADO';

    UPDATE pedidos
    SET subtotal = v_subtotal,
        total = v_subtotal - descuento + impuestos + costo_envio,
        fecha_actualizacion = CURRENT_TIMESTAMP
    WHERE id = v_pedido_id;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;
