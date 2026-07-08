package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetalleEstadoTransitionTest {

    @Test
    void validarCocina_permiteAvanceNormal() {
        DetalleEstadoTransition.validarCocina(EstadoDetalle.PENDIENTE, EstadoDetalle.EN_PREPARACION);
        DetalleEstadoTransition.validarCocina(EstadoDetalle.EN_PREPARACION, EstadoDetalle.LISTO);
    }

    @Test
    void validarCocina_rechazaSaltoDirectoAListo() {
        assertThatThrownBy(() -> DetalleEstadoTransition.validarCocina(EstadoDetalle.PENDIENTE, EstadoDetalle.LISTO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validarCocina_rechazaEntregadoDesdeCocina() {
        assertThatThrownBy(() -> DetalleEstadoTransition.validarCocina(EstadoDetalle.LISTO, EstadoDetalle.ENTREGADO))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validarEntrega_soloDesdeListo() {
        DetalleEstadoTransition.validarEntrega(EstadoDetalle.LISTO);
    }

    @Test
    void validarEntrega_rechazaPendiente() {
        assertThatThrownBy(() -> DetalleEstadoTransition.validarEntrega(EstadoDetalle.PENDIENTE))
                .isInstanceOf(BusinessException.class);
    }
}
