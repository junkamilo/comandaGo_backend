package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoDetalleEstadoPromoterTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PedidoDetalleEstadoPromoter promoter;

    @Test
    void promuevePedidoAListo() {
        Pedido pedido = Pedido.builder()
                .id(1L)
                .estado(EstadoPedido.EN_PREPARACION)
                .detalles(List.of(
                        DetallePedido.builder().estado(EstadoDetalle.LISTO).build(),
                        DetallePedido.builder().estado(EstadoDetalle.LISTO).build()))
                .build();
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        promoter.aplicarPromocionPorEstadoDetalles(pedido);

        verify(pedidoRepository).save(pedido);
    }

    @Test
    void promuevePedidoAEntregadoSinLiberarMesa() {
        Pedido pedido = Pedido.builder()
                .id(1L)
                .estado(EstadoPedido.LISTO)
                .detalles(List.of(
                        DetallePedido.builder().estado(EstadoDetalle.ENTREGADO).build(),
                        DetallePedido.builder().estado(EstadoDetalle.ENTREGADO).build()))
                .build();
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        promoter.aplicarPromocionPorEstadoDetalles(pedido);

        verify(pedidoRepository).save(pedido);
    }
}
