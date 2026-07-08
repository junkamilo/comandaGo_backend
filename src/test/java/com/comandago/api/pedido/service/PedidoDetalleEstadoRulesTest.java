package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoDetalleEstadoRulesTest {

    @Test
    void debePromoverPedidoAListo_cuandoTodosLosItemsEstanListos() {
        Pedido pedido = pedidoEnPreparacion(
                detalleConEstado(EstadoDetalle.LISTO),
                detalleConEstado(EstadoDetalle.LISTO));

        assertThat(PedidoDetalleEstadoRules.debePromoverPedidoAListo(pedido)).isTrue();
    }

    @Test
    void debePromoverPedidoAListo_cuandoHayItemsCancelados() {
        Pedido pedido = pedidoEnPreparacion(
                detalleConEstado(EstadoDetalle.LISTO),
                detalleConEstado(EstadoDetalle.CANCELADO));

        assertThat(PedidoDetalleEstadoRules.debePromoverPedidoAListo(pedido)).isTrue();
    }

    @Test
    void noPromueveSiHayItemsPendientes() {
        Pedido pedido = pedidoEnPreparacion(
                detalleConEstado(EstadoDetalle.LISTO),
                detalleConEstado(EstadoDetalle.PENDIENTE));

        assertThat(PedidoDetalleEstadoRules.debePromoverPedidoAListo(pedido)).isFalse();
    }

    @Test
    void noPromueveSiPedidoNoEstaEnPreparacion() {
        Pedido pedido = pedidoEnPreparacion(detalleConEstado(EstadoDetalle.LISTO));
        pedido.setEstado(EstadoPedido.POR_CONFIRMAR);

        assertThat(PedidoDetalleEstadoRules.debePromoverPedidoAListo(pedido)).isFalse();
    }

    @Test
    void puedeEntregarDetalle_soloCuandoEstaListo() {
        assertThat(PedidoDetalleEstadoRules.puedeEntregarDetalle(detalleConEstado(EstadoDetalle.LISTO))).isTrue();
        assertThat(PedidoDetalleEstadoRules.puedeEntregarDetalle(detalleConEstado(EstadoDetalle.PENDIENTE))).isFalse();
    }

    @Test
    void todosActivosListosParaEntregar_cuandoTodosEstanListos() {
        Pedido pedido = pedidoConEstado(EstadoPedido.LISTO,
                detalleConEstado(EstadoDetalle.LISTO),
                detalleConEstado(EstadoDetalle.LISTO));

        assertThat(PedidoDetalleEstadoRules.todosActivosListosParaEntregar(pedido)).isTrue();
    }

    @Test
    void todosActivosListosParaEntregar_falsoSiHayEnPreparacion() {
        Pedido pedido = pedidoConEstado(EstadoPedido.LISTO,
                detalleConEstado(EstadoDetalle.LISTO),
                detalleConEstado(EstadoDetalle.EN_PREPARACION));

        assertThat(PedidoDetalleEstadoRules.todosActivosListosParaEntregar(pedido)).isFalse();
    }

    @Test
    void debePromoverPedidoAEntregado_cuandoTodosActivosEntregados() {
        Pedido pedido = pedidoConEstado(EstadoPedido.LISTO,
                detalleConEstado(EstadoDetalle.ENTREGADO),
                detalleConEstado(EstadoDetalle.ENTREGADO));

        assertThat(PedidoDetalleEstadoRules.debePromoverPedidoAEntregado(pedido)).isTrue();
    }

    @Test
    void debePromoverPedidoAEntregado_ignoraCancelados() {
        Pedido pedido = pedidoConEstado(EstadoPedido.LISTO,
                detalleConEstado(EstadoDetalle.ENTREGADO),
                detalleConEstado(EstadoDetalle.CANCELADO));

        assertThat(PedidoDetalleEstadoRules.debePromoverPedidoAEntregado(pedido)).isTrue();
    }

    @Test
    void noPromueveAEntregadoSiQuedanActivosSinEntregar() {
        Pedido pedido = pedidoConEstado(EstadoPedido.LISTO,
                detalleConEstado(EstadoDetalle.ENTREGADO),
                detalleConEstado(EstadoDetalle.LISTO));

        assertThat(PedidoDetalleEstadoRules.debePromoverPedidoAEntregado(pedido)).isFalse();
    }

    private Pedido pedidoConEstado(EstadoPedido estado, DetallePedido... detalles) {
        Pedido pedido = new Pedido();
        pedido.setEstado(estado);
        pedido.setDetalles(List.of(detalles));
        return pedido;
    }

    private Pedido pedidoEnPreparacion(DetallePedido... detalles) {
        Pedido pedido = new Pedido();
        pedido.setEstado(EstadoPedido.EN_PREPARACION);
        pedido.setDetalles(List.of(detalles));
        return pedido;
    }

    private DetallePedido detalleConEstado(EstadoDetalle estado) {
        DetallePedido detalle = new DetallePedido();
        detalle.setEstado(estado);
        return detalle;
    }
}
