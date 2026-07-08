package com.comandago.api.pago.service;

import com.comandago.api.pago.dto.request.RegistrarPagoRequest;
import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.pago.dto.response.ResumenPagoPedidoResponse;
import com.comandago.api.pago.entity.Pago;
import com.comandago.api.pago.enums.EstadoTransaccionPago;
import com.comandago.api.pago.enums.MetodoPago;
import com.comandago.api.pago.repository.PagoRepository;
import com.comandago.api.pago.dto.mapper.PagoMapper;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.pedido.service.PedidoMesaCoordinator;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private com.comandago.api.usuario.repository.UsuarioRepository usuarioRepository;

    @Mock
    private PagoMapper pagoMapper;

    @Mock
    private PedidoMesaCoordinator mesaCoordinator;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PagoServiceImpl pagoService;

    @Test
    void registrar_pagoParcial_dejaPedidoEnParcial() {
        Pedido pedido = pedidoListo(BigDecimal.valueOf(85000));
        RegistrarPagoRequest request = requestEfectivo(BigDecimal.valueOf(30000), BigDecimal.valueOf(50000));

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.totalPagadoPorPedido(1L)).thenReturn(BigDecimal.ZERO, BigDecimal.valueOf(30000));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(10L);
            p.setEstado(EstadoTransaccionPago.COMPLETADO);
            return p;
        });
        when(pagoMapper.toResponse(any(Pago.class))).thenReturn(
                new PagoResponse(10L, 1L, "P-1", null, MetodoPago.EFECTIVO, EstadoTransaccionPago.COMPLETADO,
                        BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.valueOf(50000), BigDecimal.valueOf(20000),
                        null, null, null, null));

        PagoResponse response = pagoService.registrar(request);

        assertThat(response.monto()).isEqualByComparingTo(BigDecimal.valueOf(30000));
        assertThat(pedido.getEstadoPago()).isEqualTo(EstadoPago.PARCIAL);
        verify(mesaCoordinator, never()).liberarMesaSiCorresponde(any());
    }

    @Test
    void registrar_pagoCompleto_marcaPagadoYLliberaMesa() {
        Pedido pedido = pedidoListo(BigDecimal.valueOf(85000));
        RegistrarPagoRequest request = requestEfectivo(BigDecimal.valueOf(85000), BigDecimal.valueOf(100000));

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.totalPagadoPorPedido(1L)).thenReturn(BigDecimal.ZERO, BigDecimal.valueOf(85000));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pagoMapper.toResponse(any(Pago.class))).thenReturn(
                new PagoResponse(10L, 1L, "P-1", null, MetodoPago.EFECTIVO, EstadoTransaccionPago.COMPLETADO,
                        BigDecimal.valueOf(85000), BigDecimal.ZERO, BigDecimal.valueOf(100000), BigDecimal.valueOf(15000),
                        null, null, null, null));

        pagoService.registrar(request);

        assertThat(pedido.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
        verify(mesaCoordinator).liberarMesaSiCorresponde(pedido);
    }

    @Test
    void registrar_sobrepago_lanzaBusinessException() {
        Pedido pedido = pedidoListo(BigDecimal.valueOf(85000));
        RegistrarPagoRequest request = requestEfectivo(BigDecimal.valueOf(90000), BigDecimal.valueOf(100000));

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagoRepository.totalPagadoPorPedido(1L)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> pagoService.registrar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("saldo");
    }

    @Test
    void registrar_efectivoSinMontoRecibido_lanzaBusinessException() {
        Pedido pedido = pedidoListo(BigDecimal.valueOf(85000));
        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setPedidoId(1L);
        request.setMetodo(MetodoPago.EFECTIVO);
        request.setMonto(BigDecimal.valueOf(30000));

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pagoService.registrar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("monto recibido");
    }

    @Test
    void registrar_pedidoCancelado_lanzaBusinessException() {
        Pedido pedido = pedidoListo(BigDecimal.valueOf(85000));
        pedido.setEstado(EstadoPedido.CANCELADO);
        RegistrarPagoRequest request = requestEfectivo(BigDecimal.valueOf(30000), BigDecimal.valueOf(50000));

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pagoService.registrar(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cancelado");
    }

    @Test
    void confirmar_pagoNequi_actualizaEstadoPedido() {
        Pedido pedido = pedidoListo(BigDecimal.valueOf(85000));
        Pago pago = new Pago();
        pago.setId(10L);
        pago.setPedido(pedido);
        pago.setEstado(EstadoTransaccionPago.PENDIENTE);
        pago.setMonto(BigDecimal.valueOf(85000));
        pago.setMetodo(MetodoPago.NEQUI);
        pago.setPropina(BigDecimal.ZERO);

        when(pagoRepository.findById(10L)).thenReturn(Optional.of(pago));
        when(pagoRepository.totalPagadoPorPedido(1L)).thenReturn(BigDecimal.valueOf(85000));
        when(pagoMapper.toResponse(any(Pago.class))).thenReturn(
                new PagoResponse(10L, 1L, "P-1", null, MetodoPago.NEQUI, EstadoTransaccionPago.COMPLETADO,
                        BigDecimal.valueOf(85000), BigDecimal.ZERO, null, null, null, null, null, null));

        pagoService.confirmar(10L);

        assertThat(pago.getEstado()).isEqualTo(EstadoTransaccionPago.COMPLETADO);
        assertThat(pedido.getEstadoPago()).isEqualTo(EstadoPago.PAGADO);
    }

    private Pedido pedidoListo(BigDecimal total) {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setNumeroPedido("P-1");
        pedido.setTotal(total);
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedido.setEstadoPago(EstadoPago.PENDIENTE);
        return pedido;
    }

    private RegistrarPagoRequest requestEfectivo(BigDecimal monto, BigDecimal recibido) {
        RegistrarPagoRequest request = new RegistrarPagoRequest();
        request.setPedidoId(1L);
        request.setMetodo(MetodoPago.EFECTIVO);
        request.setMonto(monto);
        request.setPropina(BigDecimal.ZERO);
        request.setMontoRecibido(recibido);
        return request;
    }
}
