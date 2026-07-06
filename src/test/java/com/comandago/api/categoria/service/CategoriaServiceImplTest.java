package com.comandago.api.categoria.service;

import com.comandago.api.categoria.dto.mapper.CategoriaMapper;
import com.comandago.api.categoria.dto.request.CategoriaCreateRequest;
import com.comandago.api.categoria.dto.request.CategoriaReordenarRequest;
import com.comandago.api.categoria.dto.request.CategoriaUpdateRequest;
import com.comandago.api.categoria.dto.response.CategoriaResponse;
import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ConflictException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.storage.StorageBucket;
import com.comandago.api.storage.service.SupabaseStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CategoriaServiceImplTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaMapper categoriaMapper;

    @Mock
    private SupabaseStorageService supabaseStorageService;

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    @Test
    void crear_subcategoriaValida_guardaConPadre() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Cervezas");
        request.setCategoriaPadreId(1L);

        Categoria padre = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();
        Categoria nueva = Categoria.builder().nombre("Cervezas").activo(true).build();
        Categoria guardada = Categoria.builder().id(2L).nombre("Cervezas").orden(0).activo(true)
                .categoriaPadre(padre).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Cervezas")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(nueva);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(padre));
        when(categoriaRepository.findMaxOrdenEnNivel(1L)).thenReturn(-1);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(guardada);
        when(categoriaMapper.toResponse(guardada)).thenReturn(
                CategoriaResponse.builder()
                        .id(2L)
                        .nombre("Cervezas")
                        .orden(0)
                        .activo(true)
                        .categoriaPadreId(1L)
                        .categoriaPadreNombre("Bebidas")
                        .subcategorias(List.of())
                        .build());

        CategoriaResponse response = categoriaService.crear(request);

        assertThat(response.categoriaPadreId()).isEqualTo(1L);
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void crear_asignaOrdenAutomatico_enNivelRaiz() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Postres");

        Categoria nueva = Categoria.builder().nombre("Postres").activo(true).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Postres")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(nueva);
        when(categoriaRepository.findMaxOrdenEnNivel(null)).thenReturn(2);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> inv.getArgument(0));
        when(categoriaMapper.toResponse(any(Categoria.class))).thenReturn(
                CategoriaResponse.builder().id(3L).nombre("Postres").orden(3).activo(true).subcategorias(List.of()).build());

        categoriaService.crear(request);

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        assertThat(captor.getValue().getOrden()).isEqualTo(3);
    }

    @Test
    void crear_padreInexistente_lanzaResourceNotFoundException() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Cervezas");
        request.setCategoriaPadreId(99L);

        Categoria nueva = Categoria.builder().nombre("Cervezas").activo(true).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Cervezas")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(nueva);
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaService.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void crear_padreQueYaEsSubcategoria_lanzaBusinessException() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Refrescos");
        request.setCategoriaPadreId(2L);

        Categoria abuelo = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();
        Categoria padreSub = Categoria.builder().id(2L).nombre("Cervezas").activo(true)
                .categoriaPadre(abuelo).build();
        Categoria nueva = Categoria.builder().nombre("Refrescos").activo(true).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Refrescos")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(nueva);
        when(categoriaRepository.findById(2L)).thenReturn(Optional.of(padreSub));

        assertThatThrownBy(() -> categoriaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("más de 2 niveles");
    }

    @Test
    void crear_autoPadre_lanzaBusinessException() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Bebidas");
        request.setCategoriaPadreId(1L);

        Categoria existente = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Bebidas")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(existente);

        assertThatThrownBy(() -> categoriaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("su propio padre");
    }

    @Test
    void eliminar_conSubcategoriasActivas_lanzaConflictException() {
        Categoria categoria = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(productoRepository.existsByCategoriaIdAndActivoTrue(1L)).thenReturn(false);
        when(categoriaRepository.existsByCategoriaPadreIdAndActivoTrue(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoriaService.eliminar(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("subcategorías activas");
    }

    @Test
    void menu_armaArbolConHijas() {
        Categoria padre = Categoria.builder().id(1L).nombre("Bebidas").orden(0).activo(true).build();
        Categoria hija = Categoria.builder().id(2L).nombre("Cervezas").orden(0).activo(true)
                .categoriaPadre(padre).build();

        CategoriaResponse hijaResponse = CategoriaResponse.builder()
                .id(2L).nombre("Cervezas").orden(0).activo(true)
                .categoriaPadreId(1L).categoriaPadreNombre("Bebidas")
                .subcategorias(List.of()).build();
        CategoriaResponse padreResponse = CategoriaResponse.builder()
                .id(1L).nombre("Bebidas").orden(0).activo(true)
                .subcategorias(List.of(hijaResponse)).build();

        when(categoriaRepository.findByActivoTrueAndCategoriaPadreIsNullOrderByOrdenAsc())
                .thenReturn(List.of(padre));
        when(categoriaRepository.findByActivoTrueAndCategoriaPadreIdOrderByOrdenAsc(1L))
                .thenReturn(List.of(hija));
        when(categoriaMapper.toResponseConHijas(padre, List.of(hija))).thenReturn(padreResponse);

        List<CategoriaResponse> menu = categoriaService.menu();

        assertThat(menu).hasSize(1);
        assertThat(menu.get(0).subcategorias()).hasSize(1);
        assertThat(menu.get(0).subcategorias().get(0).nombre()).isEqualTo("Cervezas");
        verify(categoriaMapper).toResponseConHijas(eq(padre), eq(List.of(hija)));
    }

    @Test
    void crear_sinImagen_guardaImagenUrlNull() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Postres");

        Categoria nueva = Categoria.builder().nombre("Postres").activo(true).build();
        Categoria guardada = Categoria.builder().id(3L).nombre("Postres").orden(0).activo(true).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Postres")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(nueva);
        when(categoriaRepository.findMaxOrdenEnNivel(null)).thenReturn(-1);
        when(categoriaRepository.save(any(Categoria.class))).thenReturn(guardada);
        when(categoriaMapper.toResponse(guardada)).thenReturn(
                CategoriaResponse.builder().id(3L).nombre("Postres").orden(0).activo(true).subcategorias(List.of()).build());

        categoriaService.crear(request);

        verify(supabaseStorageService, never()).validarUrlDelBucket(any(), any());
        verify(categoriaRepository).save(any(Categoria.class));
    }

    @Test
    void crear_urlInvalida_lanzaBusinessException() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Postres");
        request.setImagenUrl("https://evil.com/x.webp");

        Categoria nueva = Categoria.builder().nombre("Postres").activo(true).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Postres")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(nueva);
        org.mockito.Mockito.doThrow(new BusinessException("URL inválida"))
                .when(supabaseStorageService)
                .validarUrlDelBucket(eq(StorageBucket.CATEGORIAS), eq("https://evil.com/x.webp"));

        assertThatThrownBy(() -> categoriaService.crear(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void actualizar_limpiarImagen_borraStorageYPersisteNull() {
        String url = "https://test.supabase.co/storage/v1/object/public/categorias/old.webp";
        Categoria existente = Categoria.builder().id(1L).nombre("Bebidas").imagenUrl(url).activo(true).build();
        CategoriaUpdateRequest request = new CategoriaUpdateRequest();
        request.setImagenUrl("");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(supabaseStorageService.esUrlDelBucket(eq(StorageBucket.CATEGORIAS), eq(url))).thenReturn(true);
        when(categoriaRepository.save(existente)).thenReturn(existente);
        when(categoriaMapper.toResponse(existente)).thenReturn(
                CategoriaResponse.builder().id(1L).nombre("Bebidas").activo(true).subcategorias(List.of()).build());

        categoriaService.actualizar(1L, request);

        verify(supabaseStorageService).eliminarPorPublicUrl(eq(StorageBucket.CATEGORIAS), eq(url));
        assertThat(existente.getImagenUrl()).isNull();
    }

    @Test
    void reordenar_padres_actualizaOrden() {
        Categoria a = Categoria.builder().id(1L).nombre("A").orden(2).activo(true).build();
        Categoria b = Categoria.builder().id(2L).nombre("B").orden(0).activo(true).build();

        when(categoriaRepository.countByActivoTrueAndCategoriaPadreIsNull()).thenReturn(2L);
        when(categoriaRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(a, b));

        categoriaService.reordenar(new CategoriaReordenarRequest(List.of(2L, 1L), null));

        assertThat(b.getOrden()).isZero();
        assertThat(a.getOrden()).isEqualTo(1);
        verify(categoriaRepository).saveAll(List.of(a, b));
    }

    @Test
    void reordenar_idsDeDistintoPadre_lanzaBusinessException() {
        Categoria padre = Categoria.builder().id(10L).nombre("Padre").activo(true).build();
        Categoria hija = Categoria.builder().id(1L).nombre("Hija").activo(true).categoriaPadre(padre).build();
        Categoria raiz = Categoria.builder().id(2L).nombre("Raiz").activo(true).build();

        when(categoriaRepository.countByActivoTrueAndCategoriaPadreIsNull()).thenReturn(2L);
        when(categoriaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(hija, raiz));

        assertThatThrownBy(() -> categoriaService.reordenar(new CategoriaReordenarRequest(List.of(1L, 2L), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mismo nivel");
    }
}
