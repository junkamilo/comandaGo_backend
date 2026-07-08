package com.comandago.api.pedido;

import com.comandago.api.AbstractIntegrationTest;
import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.dto.request.DetalleEstadoRequest;
import com.comandago.api.pedido.dto.request.CancelarDetallesRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.PedidoCreateRequest;
import com.comandago.api.pedido.dto.request.PedidoEstadoRequest;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.enums.OrigenPedido;
import com.comandago.api.pedido.service.DetallePedidoService;
import com.comandago.api.pedido.service.PedidoService;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private DetallePedidoService detallePedidoService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private MesaRepository mesaRepository;

    private Long productoId;
    private Long mesaId;

    @BeforeEach
    void setUp() {
        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nombre("Test")
                .orden(0)
                .activo(true)
                .build());
        Producto producto = productoRepository.save(Producto.builder()
                .categoria(categoria)
                .nombre("Hamburguesa")
                .precio(new BigDecimal("15000.00"))
                .activo(true)
                .disponible(true)
                .orden(0)
                .esPromocion(false)
                .build());
        Mesa mesa = mesaRepository.save(Mesa.builder()
                .numero("T1")
                .estado(EstadoMesa.LIBRE)
                .activo(true)
                .qrToken("qr-test-token")
                .build());
        productoId = producto.getId();
        mesaId = mesa.getId();
    }

    @Test
    void crearPedido_recalculaTotalConImpoconsumo() {
        PedidoCreateRequest request = pedidoMesaQr(2);

        var response = pedidoService.crear(request);

        assertThat(response.numeroPedido()).isNotBlank();
        assertThat(response.subtotal()).isEqualByComparingTo("30000.00");
        assertThat(response.impuestos()).isEqualByComparingTo("2400.00");
        assertThat(response.total()).isEqualByComparingTo("32400.00");
        assertThat(response.detalles()).hasSize(1);
        assertThat(response.mesaNumero()).isEqualTo("T1");

        Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.OCUPADA);
    }

    @Test
    void cancelarPedido_liberaMesa() {
        var creado = pedidoService.crear(pedidoMesaQr(1));

        pedidoService.cancelar(creado.id());

        Mesa mesa = mesaRepository.findById(mesaId).orElseThrow();
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.LIBRE);
    }

    @Test
    void detalleListo_promuevePedidoAListo() {
        var creado = pedidoService.crear(pedidoMesaQr(1));
        Long detalleId = creado.detalles().get(0).id();

        pedidoService.actualizarEstado(creado.id(), estadoEnPreparacion());

        DetalleEstadoRequest estadoRequest = new DetalleEstadoRequest();
        estadoRequest.setEstado(EstadoDetalle.LISTO);
        detallePedidoService.actualizarEstado(creado.id(), detalleId, estadoRequest);

        var actualizado = pedidoService.obtenerPorId(creado.id());
        assertThat(actualizado.estado()).isEqualTo(EstadoPedido.LISTO);
    }

    @Test
    void cancelarDetalle_excluyeCanceladosDelSubtotal() {
        var creado = pedidoService.crear(pedidoMesaQr(2));
        var segundoDetalle = detallePedidoService.agregar(creado.id(), detalleRequest(1));

        CancelarDetallesRequest request = new CancelarDetallesRequest();
        request.setDetalleIds(Set.of(segundoDetalle.id()));
        var actualizado = pedidoService.cancelarDetalles(creado.id(), request);

        assertThat(actualizado.detalles()).hasSize(2);
        assertThat(actualizado.detalles().stream().filter(d -> d.estado() == EstadoDetalle.CANCELADO).count())
                .isEqualTo(1);
        assertThat(actualizado.subtotal()).isEqualByComparingTo("30000.00");
    }

    private PedidoCreateRequest pedidoMesaQr(int cantidad) {
        PedidoCreateRequest request = new PedidoCreateRequest();
        request.setOrigen(OrigenPedido.MESA_QR);
        request.setMesaId(mesaId);
        DetallePedidoItemRequest detalle = new DetallePedidoItemRequest();
        detalle.setProductoId(productoId);
        detalle.setCantidad(cantidad);
        request.setDetalles(List.of(detalle));
        return request;
    }

    private PedidoEstadoRequest estadoEnPreparacion() {
        PedidoEstadoRequest request = new PedidoEstadoRequest();
        request.setEstado(EstadoPedido.EN_PREPARACION);
        return request;
    }

    private DetallePedidoItemRequest detalleRequest(int cantidad) {
        DetallePedidoItemRequest detalle = new DetallePedidoItemRequest();
        detalle.setProductoId(productoId);
        detalle.setCantidad(cantidad);
        return detalle;
    }
}
