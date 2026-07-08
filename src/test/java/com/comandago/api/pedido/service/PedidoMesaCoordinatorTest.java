package com.comandago.api.pedido.service;

import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoMesaCoordinatorTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private PedidoMesaCoordinator mesaCoordinator;

    @Test
    void liberarMesa_noLiberaSiHayCuentaPendienteEntregada() {
        Mesa mesa = Mesa.builder().id(1L).estado(EstadoMesa.OCUPADA).build();
        Pedido pedido = Pedido.builder().id(10L).mesa(mesa).estado(EstadoPedido.ENTREGADO).build();

        when(pedidoRepository.existsByMesaIdAndEstadoNotAndEstadoPagoNot(
                1L, EstadoPedido.CANCELADO, EstadoPago.PAGADO)).thenReturn(true);

        mesaCoordinator.liberarMesaSiCorresponde(pedido);

        verify(mesaRepository, never()).save(any());
    }

    @Test
    void liberarMesa_liberaCuandoNoHayCuentaPendiente() {
        Mesa mesa = Mesa.builder().id(1L).estado(EstadoMesa.OCUPADA).build();
        Pedido pedido = Pedido.builder().id(10L).mesa(mesa).estado(EstadoPedido.ENTREGADO).build();

        when(pedidoRepository.existsByMesaIdAndEstadoNotAndEstadoPagoNot(
                1L, EstadoPedido.CANCELADO, EstadoPago.PAGADO)).thenReturn(false);
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> inv.getArgument(0));

        mesaCoordinator.liberarMesaSiCorresponde(pedido);

        verify(mesaRepository).save(mesa);
    }
}
