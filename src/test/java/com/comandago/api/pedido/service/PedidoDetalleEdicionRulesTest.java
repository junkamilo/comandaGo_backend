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

class PedidoDetalleEdicionRulesTest {

    @Test
    void validarCancelable_rechazaDetalleEnPreparacion() {
        DetallePedido detalle = new DetallePedido();
        detalle.setNombreProducto("Arroz");
        detalle.setEstado(EstadoDetalle.EN_PREPARACION);

        assertThatThrownBy(() -> PedidoDetalleEdicionRules.validarCancelable(detalle))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no se puede cancelar ni cambiar");
    }

    @Test
    void validarPedidoCancelableCompleto_rechazaDetalleNoPendiente() {
        Pedido pedido = new Pedido();
        pedido.setDetalles(List.of(detalle(EstadoDetalle.PENDIENTE), detalle(EstadoDetalle.LISTO)));

        assertThatThrownBy(() -> PedidoDetalleEdicionRules.validarPedidoCancelableCompleto(pedido))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede cancelar el pedido completo");
    }

    @Test
    void validarPedidoEditable_rechazaPedidoEntregado() {
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.ENTREGADO);

        assertThatThrownBy(() -> PedidoDetalleEdicionRules.validarPedidoEditable(pedido))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No se puede modificar un pedido");
    }

    @Test
    void todosDetallesCancelados_retornaTrue_siTodosCancelados() {
        Pedido pedido = new Pedido();
        pedido.setDetalles(List.of(detalle(EstadoDetalle.CANCELADO), detalle(EstadoDetalle.CANCELADO)));

        assertThat(PedidoDetalleEdicionRules.todosDetallesCancelados(pedido)).isTrue();
    }

    private static DetallePedido detalle(EstadoDetalle estado) {
        DetallePedido detalle = new DetallePedido();
        detalle.setEstado(estado);
        return detalle;
    }
}
