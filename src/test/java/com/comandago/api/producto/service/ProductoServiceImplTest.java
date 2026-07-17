package com.comandago.api.producto.service;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.producto.dto.mapper.ProductoMapper;
import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoInsumoRequest;
import com.comandago.api.producto.dto.request.ProductoReordenarRequest;
import com.comandago.api.producto.dto.response.PersonalizacionProductoResponse;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.producto.enums.UnidadInsumo;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.receta.entity.Receta;
import com.comandago.api.receta.entity.RecetaIngrediente;
import com.comandago.api.receta.repository.RecetaRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.storage.StorageBucket;
import com.comandago.api.storage.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private RecetaRepository recetaRepository;

    @Mock
    private ProductoMapper productoMapper;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private static final List<TipoProducto> TIPOS_MENU =
            List.of(TipoProducto.COMPUESTO, TipoProducto.NORMAL, TipoProducto.INSUMO);

    @Test
    void crear_enCategoriaHojaActiva_guardaProducto() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Bandeja paisa");
        request.setPrecio(new BigDecimal("25000"));
        request.setEsPromocion(false);

        Categoria hoja = Categoria.builder().id(2L).nombre("Corrientes").activo(true).build();
        Producto guardado = Producto.builder().id(1L).categoria(hoja).nombre("Bandeja paisa")
                .precio(new BigDecimal("25000")).esPromocion(false).tipo(TipoProducto.NORMAL).build();

        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(hoja));
        when(categoriaRepository.existsByCategoriaPadreId(2L)).thenReturn(false);
        when(productoRepository.findMaxOrdenEnCategoria(2L)).thenReturn(-1);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(productoRepository.findByIdWithComposicion(1L)).thenReturn(Optional.of(guardado));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(
                ProductoResponse.builder().id(1L).nombre("Bandeja paisa").precioFinal(new BigDecimal("25000")).build());

        ProductoResponse response = productoService.crear(request);

        assertThat(response.nombre()).isEqualTo("Bandeja paisa");
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void crear_compuestoSinReceta_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Perro");
        request.setPrecio(new BigDecimal("12000"));
        request.setTipo(TipoProducto.COMPUESTO);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("receta");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crear_compuestoConReceta_guardaProducto() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Perro");
        request.setPrecio(new BigDecimal("12000"));
        request.setTipo(TipoProducto.COMPUESTO);
        request.setRecetaId(7L);

        Categoria hoja = Categoria.builder().id(2L).activo(true).build();
        Receta receta = new Receta();
        receta.setId(7L);
        receta.setNombre("Receta perro");
        receta.setActivo(true);

        Producto guardado = Producto.builder().id(1L).categoria(hoja).nombre("Perro")
                .tipo(TipoProducto.COMPUESTO).receta(receta).build();

        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(hoja));
        when(categoriaRepository.existsByCategoriaPadreId(2L)).thenReturn(false);
        when(productoRepository.findMaxOrdenEnCategoria(2L)).thenReturn(-1);
        when(recetaRepository.findById(7L)).thenReturn(Optional.of(receta));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(productoRepository.findByIdWithComposicion(1L)).thenReturn(Optional.of(guardado));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(
                ProductoResponse.builder().id(1L).nombre("Perro").tipo(TipoProducto.COMPUESTO)
                        .recetaId(7L).recetaNombre("Receta perro").build());
        org.mockito.Mockito.doAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            ProductoCreateRequest r = invocation.getArgument(1);
            p.setTipo(r.getTipo());
            return null;
        }).when(productoMapper).applyCreate(any(Producto.class), any(ProductoCreateRequest.class));

        ProductoResponse response = productoService.crear(request);

        assertThat(response.recetaId()).isEqualTo(7L);
        verify(productoRepository).save(any(Producto.class));
    }

    @Test
    void crear_normalConReceta_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Coca");
        request.setPrecio(new BigDecimal("5000"));
        request.setTipo(TipoProducto.NORMAL);
        request.setRecetaId(7L);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("COMPUESTO");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crear_compuestoConComposicionEmbebida_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Perro");
        request.setPrecio(new BigDecimal("12000"));
        request.setTipo(TipoProducto.COMPUESTO);
        request.setRecetaId(7L);
        ProductoInsumoRequest linea = new ProductoInsumoRequest();
        linea.setProductoInsumoId(50L);
        linea.setCantidad(BigDecimal.ONE);
        linea.setUnidad(UnidadInsumo.UND);
        request.setComposicion(List.of(linea));

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("composición embebida");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crear_insumoSinCategoria_guardaProducto() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setNombre("Salchicha ranchera");
        request.setPrecio(BigDecimal.ZERO);
        request.setTipo(TipoProducto.INSUMO);

        Producto guardado = Producto.builder().id(9L).nombre("Salchicha ranchera")
                .tipo(TipoProducto.INSUMO).orden(0).build();

        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setId(9L);
            return p;
        });
        when(productoRepository.findByIdWithComposicion(9L)).thenReturn(Optional.of(guardado));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(
                ProductoResponse.builder().id(9L).nombre("Salchicha ranchera")
                        .tipo(TipoProducto.INSUMO).categoriaId(null).build());

        ProductoResponse response = productoService.crear(request);

        assertThat(response.tipo()).isEqualTo(TipoProducto.INSUMO);
        assertThat(response.categoriaId()).isNull();
        verify(categoriaRepository, never()).findById(any());
    }

    @Test
    void crear_normalSinCategoria_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setNombre("Coca");
        request.setPrecio(new BigDecimal("5000"));
        request.setTipo(TipoProducto.NORMAL);

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("requieren categoría");
        verify(productoRepository, never()).save(any());
    }

    @Test
    void listarInsumos_retornaSoloTipoInsumo() {
        Producto cebolla = Producto.builder().id(1L).nombre("Cebolla").tipo(TipoProducto.INSUMO).activo(true).build();
        when(productoRepository.findByActivoTrueAndTipoOrderByNombreAsc(TipoProducto.INSUMO))
                .thenReturn(List.of(cebolla));
        when(productoMapper.toResponse(cebolla)).thenReturn(
                ProductoResponse.builder().id(1L).nombre("Cebolla").tipo(TipoProducto.INSUMO).build());

        List<ProductoResponse> insumos = productoService.listarInsumos();

        assertThat(insumos).hasSize(1);
        assertThat(insumos.get(0).tipo()).isEqualTo(TipoProducto.INSUMO);
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
        Producto guardado = Producto.builder().id(1L).tipo(TipoProducto.NORMAL).build();
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(hoja));
        when(categoriaRepository.existsByCategoriaPadreId(2L)).thenReturn(false);
        when(productoRepository.findMaxOrdenEnCategoria(2L)).thenReturn(-1);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(productoRepository.findByIdWithComposicion(1L)).thenReturn(Optional.of(guardado));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(
                ProductoResponse.builder().id(1L).build());

        productoService.crear(request);

        verify(productoMapper).applyCreate(any(Producto.class), org.mockito.ArgumentMatchers.eq(request));
    }

    @Test
    void listarMenu_conCategoriaId_incluyeTiposVendibles() {
        Categoria hoja = Categoria.builder().id(2L).nombre("Corrientes").activo(true).build();
        Producto producto = Producto.builder().id(1L).categoria(hoja).nombre("Sancocho")
                .precio(new BigDecimal("18000")).esPromocion(false).activo(true).disponible(true)
                .tipo(TipoProducto.NORMAL).build();

        when(productoRepository.findByActivoTrueAndDisponibleTrueAndCategoriaIdAndTipoInOrderByOrdenAsc(2L, TIPOS_MENU))
                .thenReturn(List.of(producto));
        when(productoMapper.toResponse(producto)).thenReturn(
                ProductoResponse.builder().id(1L).categoriaId(2L).nombre("Sancocho").tipo(TipoProducto.NORMAL).build());

        List<ProductoResponse> menu = productoService.listarMenu(2L);

        assertThat(menu).hasSize(1);
        assertThat(menu.get(0).categoriaId()).isEqualTo(2L);
        verify(productoRepository)
                .findByActivoTrueAndDisponibleTrueAndCategoriaIdAndTipoInOrderByOrdenAsc(2L, TIPOS_MENU);
    }

    @Test
    void listarPromociones_retornaSoloPromocionesActivas() {
        Categoria hoja = Categoria.builder().id(2L).activo(true).build();
        Producto promo = Producto.builder().id(1L).categoria(hoja).nombre("Combo")
                .precio(new BigDecimal("20000")).precioPromocion(new BigDecimal("15000"))
                .esPromocion(true).activo(true).disponible(true).tipo(TipoProducto.NORMAL).build();
        Producto porcionPromo = Producto.builder().id(9L).categoria(hoja).nombre("Porción promo")
                .esPromocion(true).activo(true).disponible(true).tipo(TipoProducto.INSUMO).build();

        when(productoRepository.findByEsPromocionTrueAndActivoTrueAndDisponibleTrueAndCategoriaIsNotNullOrderByOrdenAsc())
                .thenReturn(List.of(promo, porcionPromo));
        when(productoMapper.toResponse(promo)).thenReturn(
                ProductoResponse.builder().id(1L).esPromocion(true).precioFinal(new BigDecimal("15000")).build());
        when(productoMapper.toResponse(porcionPromo)).thenReturn(
                ProductoResponse.builder().id(9L).esPromocion(true).tipo(TipoProducto.INSUMO).build());

        List<ProductoResponse> promociones = productoService.listarPromociones();

        assertThat(promociones).hasSize(2);
        assertThat(promociones.get(0).esPromocion()).isTrue();
    }

    @Test
    void menuDelDia_incluyeVendiblesConCategoriaIncluyendoInsumoVendible() {
        Categoria hoja = Categoria.builder().id(2L).nombre("Fuertes").activo(true).orden(1).build();
        Producto disponible = Producto.builder().id(1L).categoria(hoja).nombre("Sancocho")
                .precio(new BigDecimal("26000")).disponible(true).activo(true).tipo(TipoProducto.NORMAL).build();
        Producto agotado = Producto.builder().id(2L).categoria(hoja).nombre("Bandeja")
                .precio(new BigDecimal("30000")).disponible(false).activo(true).tipo(TipoProducto.NORMAL).build();
        Producto porcion = Producto.builder().id(3L).categoria(hoja).nombre("Porción arroz")
                .disponible(true).activo(true).tipo(TipoProducto.INSUMO).build();

        when(productoRepository.findByActivoTrueAndCategoriaIsNotNullOrderByCategoriaOrdenAscOrdenAsc())
                .thenReturn(List.of(disponible, agotado, porcion));
        when(productoMapper.toResponse(disponible)).thenReturn(
                ProductoResponse.builder().id(1L).nombre("Sancocho").disponible(true).activo(true).build());
        when(productoMapper.toResponse(agotado)).thenReturn(
                ProductoResponse.builder().id(2L).nombre("Bandeja").disponible(false).activo(true).build());
        when(productoMapper.toResponse(porcion)).thenReturn(
                ProductoResponse.builder().id(3L).nombre("Porción arroz").disponible(true)
                        .tipo(TipoProducto.INSUMO).build());

        List<ProductoResponse> menu = productoService.menuDelDia();

        assertThat(menu).hasSize(3);
        assertThat(menu).extracting(ProductoResponse::disponible).containsExactly(true, false, true);
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

    @Test
    void crear_imagenUrlExterna_lanzaBusinessException() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Bandeja");
        request.setPrecio(new BigDecimal("25000"));
        request.setImagenUrl("https://evil.com/foto.webp");

        Categoria hoja = Categoria.builder().id(2L).activo(true).build();
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(hoja));
        when(categoriaRepository.existsByCategoriaPadreId(2L)).thenReturn(false);
        org.mockito.Mockito.doThrow(new BusinessException("URL inválida"))
                .when(supabaseStorageService)
                .validarUrlDelBucket(eq(StorageBucket.PRODUCTOS), eq("https://evil.com/foto.webp"));

        assertThatThrownBy(() -> productoService.crear(request))
                .isInstanceOf(BusinessException.class);
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crear_asignaOrdenAutomatico_enCategoria() {
        ProductoCreateRequest request = new ProductoCreateRequest();
        request.setCategoriaId(2L);
        request.setNombre("Bandeja paisa");
        request.setPrecio(new BigDecimal("25000"));
        request.setEsPromocion(false);

        Categoria hoja = Categoria.builder().id(2L).nombre("Corrientes").activo(true).build();
        Producto guardado = Producto.builder().id(1L).tipo(TipoProducto.NORMAL).orden(3).build();

        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(hoja));
        when(categoriaRepository.existsByCategoriaPadreId(2L)).thenReturn(false);
        when(productoRepository.findMaxOrdenEnCategoria(2L)).thenReturn(2);
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> {
            Producto p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(productoRepository.findByIdWithComposicion(1L)).thenReturn(Optional.of(guardado));
        when(productoMapper.toResponse(any(Producto.class))).thenReturn(
                ProductoResponse.builder().id(1L).nombre("Bandeja paisa").orden(3).build());

        productoService.crear(request);

        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(productoRepository).save(captor.capture());
        assertThat(captor.getValue().getOrden()).isEqualTo(3);
    }

    @Test
    void reordenar_categoria_actualizaOrden() {
        Categoria hoja = Categoria.builder().id(2L).nombre("Corrientes").activo(true).build();
        Producto a = Producto.builder().id(1L).categoria(hoja).orden(2).activo(true).build();
        Producto b = Producto.builder().id(2L).categoria(hoja).orden(0).activo(true).build();

        when(productoRepository.countByActivoTrueAndCategoriaId(2L)).thenReturn(2L);
        when(productoRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(a, b));

        productoService.reordenar(new ProductoReordenarRequest(List.of(2L, 1L), 2L));

        assertThat(b.getOrden()).isZero();
        assertThat(a.getOrden()).isEqualTo(1);
        verify(productoRepository).saveAll(List.of(a, b));
    }

    @Test
    void reordenar_idsDeDistintaCategoria_lanzaBusinessException() {
        Categoria cat1 = Categoria.builder().id(2L).nombre("Corrientes").activo(true).build();
        Categoria cat2 = Categoria.builder().id(3L).nombre("Postres").activo(true).build();
        Producto p1 = Producto.builder().id(1L).categoria(cat1).activo(true).build();
        Producto p2 = Producto.builder().id(2L).categoria(cat2).activo(true).build();

        when(productoRepository.countByActivoTrueAndCategoriaId(2L)).thenReturn(2L);
        when(productoRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(p1, p2));

        assertThatThrownBy(() -> productoService.reordenar(new ProductoReordenarRequest(List.of(1L, 2L), 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("misma categoría");
    }

    @Test
    void reordenar_listaIncompleta_lanzaBusinessException() {
        when(productoRepository.countByActivoTrueAndCategoriaId(2L)).thenReturn(3L);

        assertThatThrownBy(() -> productoService.reordenar(new ProductoReordenarRequest(List.of(1L, 2L), 2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("todos los productos activos");
    }

    @Test
    void obtenerPersonalizacion_incluyeAlternativasDisponiblesMismaCategoria() {
        Categoria granos = Categoria.builder().id(7L).nombre("Porciones de granos").activo(true).build();

        Producto lentejas = Producto.builder().id(20L).nombre("Lentejas").tipo(TipoProducto.INSUMO)
                .categoria(granos).activo(true).disponible(true).build();
        Producto frijoles = Producto.builder().id(21L).nombre("Frijoles").tipo(TipoProducto.INSUMO)
                .categoria(granos).activo(true).disponible(true).build();
        Producto cerdo = Producto.builder().id(40L).nombre("Cerdo").tipo(TipoProducto.INSUMO)
                .categoria(Categoria.builder().id(3L).nombre("Proteína").activo(true).build())
                .activo(true).disponible(true).build();

        Receta receta = new Receta();
        receta.setId(1L);
        receta.setIngredientes(new ArrayList<>());

        RecetaIngrediente riLentejas = new RecetaIngrediente();
        riLentejas.setProducto(lentejas);
        riLentejas.setEsRemovible(true);
        riLentejas.setOrden(0);
        receta.agregarIngrediente(riLentejas);

        RecetaIngrediente riCerdo = new RecetaIngrediente();
        riCerdo.setProducto(cerdo);
        riCerdo.setEsRemovible(false);
        riCerdo.setOrden(1);
        receta.agregarIngrediente(riCerdo);

        Producto compuesto = Producto.builder()
                .id(10L)
                .nombre("Pechuga")
                .tipo(TipoProducto.COMPUESTO)
                .precio(new BigDecimal("12000"))
                .esPromocion(false)
                .receta(receta)
                .build();

        when(productoRepository.findByIdWithComposicion(10L)).thenReturn(Optional.of(compuesto));
        when(productoRepository.findByActivoTrueAndDisponibleTrueAndCategoriaIdAndTipoInOrderByOrdenAsc(
                eq(7L), eq(List.of(TipoProducto.INSUMO))))
                .thenReturn(List.of(lentejas, frijoles));

        PersonalizacionProductoResponse response = productoService.obtenerPersonalizacion(10L);

        assertThat(response.productoId()).isEqualTo(10L);
        assertThat(response.precioBase()).isEqualByComparingTo("12000");
        assertThat(response.ingredientes()).hasSize(2);

        var lineaLentejas = response.ingredientes().get(0);
        assertThat(lineaLentejas.nombre()).isEqualTo("Lentejas");
        assertThat(lineaLentejas.esRemovible()).isTrue();
        assertThat(lineaLentejas.alternativas()).hasSize(1);
        assertThat(lineaLentejas.alternativas().get(0).nombre()).isEqualTo("Frijoles");

        var lineaCerdo = response.ingredientes().get(1);
        assertThat(lineaCerdo.esRemovible()).isFalse();
        assertThat(lineaCerdo.alternativas()).isEmpty();
    }

    @Test
    void obtenerPersonalizacion_productoNoCompuesto_lanza() {
        Producto normal = Producto.builder().id(1L).nombre("Gaseosa").tipo(TipoProducto.NORMAL).build();
        when(productoRepository.findByIdWithComposicion(1L)).thenReturn(Optional.of(normal));

        assertThatThrownBy(() -> productoService.obtenerPersonalizacion(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("compuestos");
    }
}
