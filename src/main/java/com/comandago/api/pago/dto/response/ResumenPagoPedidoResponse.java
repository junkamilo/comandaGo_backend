package com.comandago.api.pago.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ResumenPagoPedidoResponse(
        Long pedidoId,
        String numeroPedido,
        BigDecimal totalPedido,
        BigDecimal totalPagado,
        BigDecimal totalPropinas,
        BigDecimal saldoPendiente,
        String estadoPago,
        List<PagoResponse> pagos
) {
}
