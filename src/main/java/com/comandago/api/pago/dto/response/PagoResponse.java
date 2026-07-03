package com.comandago.api.pago.dto.response;

import com.comandago.api.pago.enums.MetodoPago;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record PagoResponse(
        Long id,
        Long pedidoId,
        Long usuarioId,
        MetodoPago metodo,
        BigDecimal monto,
        String referencia,
        OffsetDateTime fechaPago
) {
}
