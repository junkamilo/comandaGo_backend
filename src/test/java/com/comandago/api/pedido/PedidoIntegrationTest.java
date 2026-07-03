package com.comandago.api.pedido;

import com.comandago.api.AbstractIntegrationTest;
import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.PedidoCreateRequest;
import com.comandago.api.pedido.enums.OrigenPedido;
import com.comandago.api.pedido.service.PedidoService;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PedidoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PedidoService pedidoService;

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
    void crearPedido_recalculaTotal() {
        PedidoCreateRequest request = new PedidoCreateRequest();
        request.setOrigen(OrigenPedido.MESA_MESERO);
        request.setMesaId(mesaId);
        DetallePedidoItemRequest detalle = new DetallePedidoItemRequest();
        detalle.setProductoId(productoId);
        detalle.setCantidad(2);
        request.setDetalles(List.of(detalle));

        var response = pedidoService.crear(request);

        assertThat(response.numeroPedido()).isNotBlank();
        assertThat(response.total()).isEqualByComparingTo("30000.00");
        assertThat(response.detalles()).hasSize(1);
    }
}
