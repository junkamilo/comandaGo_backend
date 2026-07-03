package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;

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
}
