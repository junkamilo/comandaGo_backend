package com.comandago.api.pedido.service;

import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.shared.exception.BusinessException;

import java.util.Map;
import java.util.Set;

public final class PedidoEstadoTransition {

    private static final Map<EstadoPedido, Set<EstadoPedido>> TRANSICIONES = Map.of(
            EstadoPedido.POR_CONFIRMAR, Set.of(EstadoPedido.EN_PREPARACION, EstadoPedido.CANCELADO),
            EstadoPedido.EN_PREPARACION, Set.of(EstadoPedido.LISTO, EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO),
            EstadoPedido.LISTO, Set.of(EstadoPedido.EN_CAMINO, EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO),
            EstadoPedido.EN_CAMINO, Set.of(EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO),
            EstadoPedido.ENTREGADO, Set.of(),
            EstadoPedido.CANCELADO, Set.of()
    );

    private PedidoEstadoTransition() {
    }

    public static void validar(EstadoPedido actual, EstadoPedido nuevo) {
        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new BusinessException(
                    "No se puede cambiar el estado de " + actual + " a " + nuevo);
        }
    }

    public static boolean puedeCancelar(EstadoPedido estado) {
        return estado != EstadoPedido.ENTREGADO && estado != EstadoPedido.CANCELADO;
    }
}
