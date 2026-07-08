package com.comandago.api.pedido.service;

import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.shared.exception.BusinessException;

import java.util.Map;
import java.util.Set;

public final class DetalleEstadoTransition {

    private static final Map<EstadoDetalle, Set<EstadoDetalle>> TRANSICIONES_COCINA = Map.of(
            EstadoDetalle.PENDIENTE, Set.of(EstadoDetalle.EN_PREPARACION),
            EstadoDetalle.EN_PREPARACION, Set.of(EstadoDetalle.LISTO),
            EstadoDetalle.LISTO, Set.of(),
            EstadoDetalle.ENTREGADO, Set.of(),
            EstadoDetalle.CANCELADO, Set.of()
    );

    private DetalleEstadoTransition() {
    }

    public static void validarCocina(EstadoDetalle actual, EstadoDetalle nuevo) {
        if (!TRANSICIONES_COCINA.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new BusinessException(
                    "No se puede cambiar el estado de " + actual + " a " + nuevo + " desde cocina");
        }
    }

    public static void validarEntrega(EstadoDetalle actual) {
        if (actual != EstadoDetalle.LISTO) {
            throw new BusinessException(
                    "Solo se puede marcar como entregado un plato que está listo (estado actual: " + actual + ")");
        }
    }
}
