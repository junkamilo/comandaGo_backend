package com.comandago.api.pedido.dto.response;

import com.comandago.api.pedido.enums.EstadoDetalle;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record DetallePedidoResponse(
        Long id,
        Long productoId,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        EstadoDetalle estado,
        String notasPreparacion
) {
}
