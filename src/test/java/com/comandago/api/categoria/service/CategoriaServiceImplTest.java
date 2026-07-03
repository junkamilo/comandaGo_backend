package com.comandago.api.categoria.service;

import com.comandago.api.categoria.dto.mapper.CategoriaMapper;
import com.comandago.api.categoria.dto.request.CategoriaCreateRequest;
import com.comandago.api.categoria.dto.response.CategoriaResponse;
import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ConflictException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @InjectMocks
    private CategoriaServiceImpl categoriaService;

    @Test
    void crear_subcategoriaValida_guardaConPadre() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Cervezas");
        request.setOrden(0);
        request.setCategoriaPadreId(1L);

        Categoria padre = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();
        Categoria nueva = Categoria.builder().nombre("Cervezas").orden(0).activo(true).build();
        Categoria guardada = Categoria.builder().id(2L).nombre("Cervezas").orden(0).activo(true)
                .categoriaPadre(padre).build();

        when(categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue("Cervezas")).thenReturn(false);
        when(categoriaMapper.toEntity(request)).thenReturn(nueva);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(padre));
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
    void crear_padreInexistente_lanzaResourceNotFoundException() {
        CategoriaCreateRequest request = new CategoriaCreateRequest();
        request.setNombre("Cervezas");
        request.setOrden(0);
        request.setCategoriaPadreId(99L);

        Categoria nueva = Categoria.builder().nombre("Cervezas").orden(0).activo(true).build();

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
        request.setOrden(0);
        request.setCategoriaPadreId(2L);

        Categoria abuelo = Categoria.builder().id(1L).nombre("Bebidas").activo(true).build();
        Categoria padreSub = Categoria.builder().id(2L).nombre("Cervezas").activo(true)
                .categoriaPadre(abuelo).build();
        Categoria nueva = Categoria.builder().nombre("Refrescos").orden(0).activo(true).build();

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
        request.setOrden(0);
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
}
