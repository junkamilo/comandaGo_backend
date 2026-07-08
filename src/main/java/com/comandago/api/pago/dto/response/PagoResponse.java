package com.comandago.api.pago.dto.response;

import com.comandago.api.pago.enums.EstadoTransaccionPago;
import com.comandago.api.pago.enums.MetodoPago;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PagoResponse(
        Long id,
        Long pedidoId,
        String numeroPedido,
        String cajeroNombre,
        MetodoPago metodo,
        EstadoTransaccionPago estado,
        BigDecimal monto,
        BigDecimal propina,
        BigDecimal montoRecibido,
        BigDecimal vuelto,
        String referencia,
        String proveedorId,
        String notas,
        OffsetDateTime fechaPago
) {
}
