package com.comandago.api.pedido.dto.mapper;

import com.comandago.api.pedido.dto.response.DetallePedidoResponse;
import com.comandago.api.pedido.dto.response.PedidoResponse;
import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PedidoMapper {

    public PedidoResponse toResponse(Pedido pedido) {
        return PedidoResponse.builder()
                .id(pedido.getId())
                .numeroPedido(pedido.getNumeroPedido())
                .usuarioId(pedido.getUsuario() != null ? pedido.getUsuario().getId() : null)
                .mesaId(pedido.getMesa() != null ? pedido.getMesa().getId() : null)
                .origen(pedido.getOrigen())
                .estado(pedido.getEstado())
                .estadoPago(pedido.getEstadoPago())
                .clienteNombre(pedido.getClienteNombre())
                .clienteTelefono(pedido.getClienteTelefono())
                .direccionEntrega(pedido.getDireccionEntrega())
                .subtotal(pedido.getSubtotal())
                .descuento(pedido.getDescuento())
                .impuestos(pedido.getImpuestos())
                .costoEnvio(pedido.getCostoEnvio())
                .total(pedido.getTotal())
                .notas(pedido.getNotas())
                .fechaPedido(pedido.getFechaPedido())
                .fechaActualizacion(pedido.getFechaActualizacion())
                .detalles(pedido.getDetalles() != null
                        ? pedido.getDetalles().stream().map(this::toDetalleResponse).toList()
                        : List.of())
                .build();
    }

    public DetallePedidoResponse toDetalleResponse(DetallePedido detalle) {
        return DetallePedidoResponse.builder()
                .id(detalle.getId())
                .productoId(detalle.getProducto().getId())
                .nombreProducto(detalle.getNombreProducto())
                .cantidad(detalle.getCantidad())
                .precioUnitario(detalle.getPrecioUnitario())
                .subtotal(detalle.getSubtotal())
                .estado(detalle.getEstado())
                .notasPreparacion(detalle.getNotasPreparacion())
                .build();
    }
}
