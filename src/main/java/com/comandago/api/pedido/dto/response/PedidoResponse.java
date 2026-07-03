package com.comandago.api.pedido.dto.response;

import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.enums.OrigenPedido;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Builder
public record PedidoResponse(
        Long id,
        String numeroPedido,
        Long usuarioId,
        String meseroNombre,
        Long mesaId,
        String mesaNumero,
        OrigenPedido origen,
        EstadoPedido estado,
        EstadoPago estadoPago,
        String clienteNombre,
        String clienteTelefono,
        String direccionEntrega,
        BigDecimal subtotal,
        BigDecimal descuento,
        BigDecimal impuestos,
        BigDecimal costoEnvio,
        BigDecimal total,
        String notas,
        OffsetDateTime fechaPedido,
        OffsetDateTime fechaActualizacion,
        List<DetallePedidoResponse> detalles
) {
}
