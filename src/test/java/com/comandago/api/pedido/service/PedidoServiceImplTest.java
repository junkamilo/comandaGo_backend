package com.comandago.api.pedido.service;

import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.dto.mapper.PedidoMapper;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.PedidoCreateRequest;
import com.comandago.api.pedido.dto.request.PedidoEstadoRequest;
import com.comandago.api.pedido.dto.response.PedidoResponse;
import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.enums.OrigenPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.security.UsuarioPrincipal;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private PedidoNumeroGenerator numeroGenerator;

    @Mock
    private PedidoMapper pedidoMapper;

    @Mock
    private PedidoTotalesCalculator totalesCalculator;

    @Mock
    private PedidoMesaCoordinator mesaCoordinator;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PedidoServiceImpl pedidoService;

    @Test
    void crear_mesaMeseroSinAuth_lanzaBusinessException() {
        PedidoCreateRequest request = new PedidoCreateRequest();
        request.setOrigen(OrigenPedido.MESA_MESERO);
        request.setMesaId(1L);

        TestSecurityContext.runAs(null, () -> assertThatThrownBy(() -> pedidoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("autenticación"));
    }

    @Test
    void crear_productoNoDisponible_lanzaBusinessException() {
        PedidoCreateRequest request = pedidoMesaQrRequest();
        Mesa mesa = Mesa.builder().id(1L).activo(true).build();
        Producto producto = Producto.builder().id(10L).nombre("Pizza").activo(true).disponible(false).build();

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> pedidoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está disponible");
    }

    @Test
    void actualizarEstado_transicionInvalida_lanzaBusinessException() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.POR_CONFIRMAR).build();
        PedidoEstadoRequest request = new PedidoEstadoRequest();
        request.setEstado(EstadoPedido.ENTREGADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.actualizarEstado(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancelar_pedidoEntregado_lanzaBusinessException() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.ENTREGADO).build();

        when(pedidoRepository.findByIdWithDetalles(1L)).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("entregado");
    }

    @Test
    void cancelar_pedidoActivo_cancelaDetallesYLliberaMesa() {
        Mesa mesa = Mesa.builder().id(5L).estado(EstadoMesa.OCUPADA).build();
        DetallePedido detalle = DetallePedido.builder().estado(EstadoDetalle.PENDIENTE).build();
        Pedido pedido = Pedido.builder()
                .id(1L)
                .estado(EstadoPedido.EN_PREPARACION)
                .mesa(mesa)
                .detalles(List.of(detalle))
                .build();

        when(pedidoRepository.findByIdWithDetalles(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenReturn(
                PedidoResponse.builder().id(1L).estado(EstadoPedido.CANCELADO).build());

        pedidoService.cancelar(1L);

        verify(mesaCoordinator).liberarMesaSiCorresponde(pedido);
        assertThat(detalle.getEstado()).isEqualTo(EstadoDetalle.CANCELADO);
    }

    @Test
    void actualizarEstado_entregado_liberaMesa() {
        Pedido pedido = Pedido.builder().id(1L).estado(EstadoPedido.LISTO).build();
        PedidoEstadoRequest request = new PedidoEstadoRequest();
        request.setEstado(EstadoPedido.ENTREGADO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pedidoMapper.toResponse(any(Pedido.class))).thenReturn(
                PedidoResponse.builder().id(1L).estado(EstadoPedido.ENTREGADO).build());

        pedidoService.actualizarEstado(1L, request);

        verify(mesaCoordinator).liberarMesaSiCorresponde(pedido);
    }

    @Test
    void crear_mesaMeseroConAuth_noUsaUsuarioIdDelBody() {
        PedidoCreateRequest request = pedidoMesaMeseroRequest();
        Mesa mesa = Mesa.builder().id(1L).activo(true).build();
        Producto producto = Producto.builder().id(10L).nombre("Pizza").activo(true).disponible(true)
                .precio(BigDecimal.TEN).build();
        UsuarioPrincipal principal = new UsuarioPrincipal(99L, "mesero@test.com", "hash", Rol.MESERO, true);

        when(numeroGenerator.generar()).thenReturn("20260703-0001");
        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(pedidoRepository.findByIdWithDetalles(1L)).thenAnswer(inv -> {
            Pedido p = Pedido.builder().id(1L).build();
            return Optional.of(p);
        });
        when(pedidoMapper.toResponse(any(Pedido.class)))
                .thenReturn(PedidoResponse.builder().id(1L).build());

        TestSecurityContext.runAs(principal, () -> {
            when(usuarioRepository.findById(99L)).thenReturn(Optional.of(
                    Usuario.builder().id(99L).nombre("Juan").build()));
            pedidoService.crear(request);
        });

        verify(usuarioRepository, never()).findById(50L);
        verify(mesaCoordinator).ocuparMesa(mesa);
        verify(totalesCalculator).aplicarImpuestos(ArgumentMatchers.<Pedido>any());
    }

    private PedidoCreateRequest pedidoMesaQrRequest() {
        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        item.setProductoId(10L);
        item.setCantidad(1);

        PedidoCreateRequest request = new PedidoCreateRequest();
        request.setOrigen(OrigenPedido.MESA_QR);
        request.setMesaId(1L);
        request.setDetalles(List.of(item));
        return request;
    }

    private PedidoCreateRequest pedidoMesaMeseroRequest() {
        PedidoCreateRequest request = pedidoMesaQrRequest();
        request.setOrigen(OrigenPedido.MESA_MESERO);
        request.setUsuarioId(50L);
        return request;
    }
}
