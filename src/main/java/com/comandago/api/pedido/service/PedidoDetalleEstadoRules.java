package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;

import java.util.List;

public final class PedidoDetalleEstadoRules {

    private PedidoDetalleEstadoRules() {
    }

    public static boolean debePromoverPedidoAListo(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.EN_PREPARACION) {
            return false;
        }
        return pedido.getDetalles().stream()
                .allMatch(d -> d.getEstado() == EstadoDetalle.LISTO
                        || d.getEstado() == EstadoDetalle.CANCELADO);
    }

    public static boolean puedeEntregarDetalle(DetallePedido detalle) {
        return detalle.getEstado() == EstadoDetalle.LISTO;
    }

    public static List<DetallePedido> detallesActivos(Pedido pedido) {
        return pedido.getDetalles().stream()
                .filter(d -> d.getEstado() != EstadoDetalle.CANCELADO)
                .toList();
    }

    public static boolean todosActivosEntregados(Pedido pedido) {
        List<DetallePedido> activos = detallesActivos(pedido);
        return !activos.isEmpty()
                && activos.stream().allMatch(d -> d.getEstado() == EstadoDetalle.ENTREGADO);
    }

    public static boolean todosActivosListosParaEntregar(Pedido pedido) {
        List<DetallePedido> activos = detallesActivos(pedido);
        return !activos.isEmpty()
                && activos.stream().allMatch(d -> d.getEstado() == EstadoDetalle.LISTO);
    }

    public static boolean debePromoverPedidoAEntregado(Pedido pedido) {
        if (pedido.getEstado() == EstadoPedido.ENTREGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            return false;
        }
        return todosActivosEntregados(pedido);
    }
}
