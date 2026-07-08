package com.comandago.api.pedido.service;

import com.comandago.api.pedido.dto.mapper.PedidoMapper;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.ReemplazarDetalleRequest;
import com.comandago.api.pedido.dto.response.DetallePedidoResponse;
import com.comandago.api.pedido.dto.response.PedidoResponse;
import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.DetallePedidoRepository;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.promocion.service.PromocionService;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.promocion.PrecioProductoResolver;
import com.comandago.api.shared.promocion.PrecioProductoResolver.ResultadoPrecioLinea;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetallePedidoServiceTest {

    @Mock
    private DetallePedidoRepository detallePedidoRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private PedidoTotalesCalculator totalesCalculator;

    @Mock
    private PrecioProductoResolver precioProductoResolver;

    @Mock
    private PromocionService promocionService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private PedidoDetalleEstadoPromoter estadoPromoter;

    @InjectMocks
    private DetallePedidoService detallePedidoService;

    @Test
    void agregar_permitePedidoEnPreparacion() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.EN_PREPARACION).detalles(new ArrayList<>()).build();
        Producto producto = Producto.builder()
                .id(2L)
                .nombre("Lomo")
                .activo(true)
                .disponible(true)
                .build();
        DetallePedidoItemRequest request = new DetallePedidoItemRequest();
        request.setProductoId(2L);
        request.setCantidad(1);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto));
        when(precioProductoResolver.resolver(eq(producto), eq(1), any()))
                .thenReturn(new ResultadoPrecioLinea(new BigDecimal("12000"), Optional.empty()));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoMapper.toDetalleResponse(any(DetallePedido.class)))
                .thenReturn(DetallePedidoResponse.builder().id(10L).build());

        DetallePedidoResponse response = detallePedidoService.agregar(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        verify(totalesCalculator).aplicarImpuestos(pedido);
    }

    @Test
    void reemplazar_detallePendiente_reemplazaCorrectamente() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.POR_CONFIRMAR).detalles(new ArrayList<>()).build();
        DetallePedido detalle = DetallePedido.builder()
                .id(10L)
                .pedido(pedido)
                .nombreProducto("Viejo")
                .estado(EstadoDetalle.PENDIENTE)
                .build();
        pedido.getDetalles().add(detalle);
        Producto productoNuevo = Producto.builder()
                .id(5L)
                .nombre("Nuevo")
                .activo(true)
                .disponible(true)
                .build();
        ReemplazarDetalleRequest request = new ReemplazarDetalleRequest();
        request.setNuevoProductoId(5L);
        request.setCantidad(2);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByIdAndPedidoId(10L, 1L)).thenReturn(Optional.of(detalle));
        when(productoRepository.findById(5L)).thenReturn(Optional.of(productoNuevo));
        when(precioProductoResolver.resolver(eq(productoNuevo), eq(2), any()))
                .thenReturn(new ResultadoPrecioLinea(new BigDecimal("8000"), Optional.empty()));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenReturn(PedidoResponse.builder().id(1L).build());

        PedidoResponse response = detallePedidoService.reemplazar(1L, 10L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(detalle.getEstado()).isEqualTo(EstadoDetalle.CANCELADO);
        assertThat(pedido.getDetalles()).hasSize(2);
        verify(totalesCalculator).aplicarImpuestos(pedido);
    }

    @Test
    void reemplazar_detalleEnPreparacion_lanzaBusinessException() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.POR_CONFIRMAR).detalles(new ArrayList<>()).build();
        DetallePedido detalle = DetallePedido.builder()
                .id(10L)
                .pedido(pedido)
                .nombreProducto("Viejo")
                .estado(EstadoDetalle.EN_PREPARACION)
                .build();
        pedido.getDetalles().add(detalle);
        ReemplazarDetalleRequest request = new ReemplazarDetalleRequest();
        request.setNuevoProductoId(5L);
        request.setCantidad(2);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByIdAndPedidoId(10L, 1L)).thenReturn(Optional.of(detalle));

        assertThatThrownBy(() -> detallePedidoService.reemplazar(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no se puede cancelar ni cambiar");
    }

    @Test
    void entregar_detalleListo_marcaEntregadoYPromueve() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.LISTO).detalles(new ArrayList<>()).build();
        DetallePedido detalle = DetallePedido.builder()
                .id(10L)
                .pedido(pedido)
                .nombreProducto("Pechuga")
                .estado(EstadoDetalle.LISTO)
                .build();
        pedido.getDetalles().add(detalle);

        when(pedidoRepository.findByIdWithDetalles(1L)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByIdAndPedidoId(10L, 1L)).thenReturn(Optional.of(detalle));
        when(detallePedidoRepository.save(any(DetallePedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoMapper.toDetalleResponse(any(DetallePedido.class)))
                .thenReturn(DetallePedidoResponse.builder().id(10L).estado(EstadoDetalle.ENTREGADO).build());

        DetallePedidoResponse response = detallePedidoService.entregar(1L, 10L);

        assertThat(response.estado()).isEqualTo(EstadoDetalle.ENTREGADO);
        assertThat(detalle.getEstado()).isEqualTo(EstadoDetalle.ENTREGADO);
        verify(estadoPromoter).aplicarPromocionPorEstadoDetalles(pedido);
    }

    @Test
    void entregar_detallePendiente_lanzaBusinessException() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.EN_PREPARACION).detalles(new ArrayList<>()).build();
        DetallePedido detalle = DetallePedido.builder()
                .id(10L)
                .pedido(pedido)
                .nombreProducto("Mojarra")
                .estado(EstadoDetalle.EN_PREPARACION)
                .build();
        pedido.getDetalles().add(detalle);

        when(pedidoRepository.findByIdWithDetalles(1L)).thenReturn(Optional.of(pedido));
        when(detallePedidoRepository.findByIdAndPedidoId(10L, 1L)).thenReturn(Optional.of(detalle));

        assertThatThrownBy(() -> detallePedidoService.entregar(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("listo");
    }
}
