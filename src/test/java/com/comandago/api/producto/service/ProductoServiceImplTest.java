package com.comandago.api.producto.service;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.producto.dto.mapper.ProductoMapper;
import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
class ProductoServiceImplTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoMapper productoMapper;

    @InjectMocks
    private ProductoServiceImpl productoService;

    @Test
    void crear_enCategoriaHojaActiva_guardaProducto() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Bandeja paisa");
        request.setPrecio(new BigDecimal("25000"));
        request.setEsPromocion(false);

        Categoria hoja = Categoria.builder().id(2L).nombre("Corrientes").activo(true).build();
        Producto producto = Producto.builder().categoria(hoja).build();
        Producto guardado = Producto.builder().id(1L).categoria(hoja).nombre("Bandeja paisa")
                .precio(new BigDecimal("25000")).esPromocion(false).build();

        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(hoja));
        when(categoriaRepository.existsByCategoriaPadreId(2L)).thenReturn(false);
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(
                ProductoResponse.builder().id(1L).nombre("Bandeja paisa").precioFinal(new BigDecimal("25000")).build());

        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setId(1L);
            return guardado;
        });

        ProductoResponse response = productoService.crear(request);

        assertThat(response.nombre()).isEqualTo("Bandeja paisa");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void crear_enCategoriaPadreConHijas_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(1L);
        request.setNombre("Sancocho");
        request.setPrecio(new BigDecimal("18000"));

        Categoria padre = Categoria.builder().id(1L).nombre("Almuerzos").activo(true).build();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(padre));
        when(categoriaRepository.existsByCategoriaPadreId(1L)).thenReturn(true);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("subcategorías");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crear_enCategoriaInactiva_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(3L);
        request.setNombre("Postre");
        request.setPrecio(new BigDecimal("8000"));

        Categoria inactiva = Categoria.builder().id(3L).nombre("Postres").activo(false).build();
        when(categoriaRepository.findById(3L)).thenReturn(Optional.of(inactiva));

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactiva");
    }

    @Test
    void crear_promocionConPrecioMayorOIgual_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Promo");
        request.setPrecio(new BigDecimal("10000"));
        request.setPrecioPromocion(new BigDecimal("12000"));
        request.setEsPromocion(true);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("menor al precio normal");
    }

    @Test
    void crear_sinPromocion_limpiaPrecioPromocionEnMapper() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Agua");
        request.setPrecio(new BigDecimal("3000"));
        request.setPrecioPromocion(new BigDecimal("2500"));
        request.setEsPromocion(false);

        Categoria hoja = Categoria.builder().id(2L).activo(true).build();
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(hoja));
        when(categoriaRepository.existsByCategoriaPadreId(2L)).thenReturn(false);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(
                ProductoResponse.builder().id(1L).build());

        productoService.crear(request);

        verify(productoMapper).applyCreate(any(Producto.class), org.mockito.ArgumentMatchers.eq(request));
    }

    @Test
    void listarMenu_conCategoriaId_filtraPorCategoria() {
        Categoria hoja = Categoria.builder().id(2L).nombre("Corrientes").activo(true).build();
        Producto producto = Producto.builder().id(1L).categoria(hoja).nombre("Sancocho")
                .precio(new BigDecimal("18000")).esPromocion(false).activo(true).disponible(true).build();

        when(productoRepository.findByActivoTrueAndDisponibleTrueAndCategoriaIdOrderByOrdenAsc(2L))
                .thenReturn(List.of(producto));
        when(productoMapper.toResponse(producto)).thenReturn(
                ProductoResponse.builder().id(1L).categoriaId(2L).nombre("Sancocho").build());

        List<ProductoResponse> menu = productoService.listarMenu(2L);

        assertThat(menu).hasSize(1);
        assertThat(menu.get(0).categoriaId()).isEqualTo(2L);
    }

    @Test
    void listarPromociones_retornaSoloPromocionesActivas() {
        Categoria hoja = Categoria.builder().id(2L).activo(true).build();
        Producto promo = Producto.builder().id(1L).categoria(hoja).nombre("Combo")
                .precio(new BigDecimal("20000")).precioPromocion(new BigDecimal("15000"))
                .esPromocion(true).activo(true).disponible(true).build();

        when(productoRepository.findByEsPromocionTrueAndActivoTrueAndDisponibleTrueOrderByOrdenAsc())
                .thenReturn(List.of(promo));
        when(productoMapper.toResponse(promo)).thenReturn(
                ProductoResponse.builder().id(1L).esPromocion(true).precioFinal(new BigDecimal("15000")).build());

        List<ProductoResponse> promociones = productoService.listarPromociones();

        assertThat(promociones).hasSize(1);
        assertThat(promociones.get(0).esPromocion()).isTrue();
    }

    @Test
    void menuDelDia_retornaActivosDisponiblesYAgotados() {
        Categoria hoja = Categoria.builder().id(2L).nombre("Fuertes").activo(true).orden(1).build();
        Producto disponible = Producto.builder().id(1L).categoria(hoja).nombre("Sancocho")
                .precio(new BigDecimal("26000")).disponible(true).activo(true).build();
        Producto agotado = Producto.builder().id(2L).categoria(hoja).nombre("Bandeja")
                .precio(new BigDecimal("30000")).disponible(false).activo(true).build();

        when(productoRepository.findByActivoTrueOrderByCategoriaOrdenAscOrdenAsc())
                .thenReturn(List.of(disponible, agotado));
        when(productoMapper.toResponse(disponible)).thenReturn(
                ProductoResponse.builder().id(1L).nombre("Sancocho").disponible(true).activo(true).build());
        when(productoMapper.toResponse(agotado)).thenReturn(
                ProductoResponse.builder().id(2L).nombre("Bandeja").disponible(false).activo(true).build());

        List<ProductoResponse> menu = productoService.menuDelDia();

        assertThat(menu).hasSize(2);
        assertThat(menu).extracting(ProductoResponse::disponible).containsExactly(true, false);
    }

    @Test
    void crear_categoriaInexistente_lanzaResourceNotFoundException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(99L);
        request.setNombre("Test");
        request.setPrecio(new BigDecimal("5000"));

        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
