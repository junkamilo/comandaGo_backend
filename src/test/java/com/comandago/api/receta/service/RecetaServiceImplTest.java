package com.comandago.api.receta.service;

import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.producto.enums.UnidadInsumo;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.receta.dto.mapper.RecetaMapper;
import com.comandago.api.receta.dto.request.IngredienteRequest;
import com.comandago.api.receta.dto.request.RecetaRequest;
import com.comandago.api.receta.dto.response.RecetaResponse;
import com.comandago.api.receta.entity.Receta;
import com.comandago.api.receta.repository.RecetaRepository;
import com.comandago.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaServiceImplTest {

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private RecetaMapper recetaMapper;

    @InjectMocks
    private RecetaServiceImpl recetaService;

    @Test
    void crear_conInsumoValido_guardaReceta() {
        RecetaRequest request = requestBase();
        Producto insumo = Producto.builder().id(10L).nombre("Pollo").tipo(TipoProducto.INSUMO).activo(true).build();

        when(recetaRepository.existsByNombreIgnoreCase("Pechuga")).thenReturn(false);
        when(productoRepository.findById(10L)).thenReturn(Optional.of(insumo));
        when(recetaRepository.save(any(Receta.class))).thenAnswer(invocation -> {
            Receta r = invocation.getArgument(0);
            r.setId(1L);
            return r;
        });
        when(recetaRepository.findByIdWithIngredientes(1L)).thenAnswer(invocation -> {
            Receta r = new Receta();
            r.setId(1L);
            r.setNombre("Pechuga");
            r.setIngredientes(new ArrayList<>());
            return Optional.of(r);
        });
        when(recetaMapper.toResponse(any(Receta.class))).thenReturn(
                RecetaResponse.builder().id(1L).nombre("Pechuga").totalIngredientes(1).build());

        RecetaResponse response = recetaService.crear(request);

        assertThat(response.nombre()).isEqualTo("Pechuga");
        verify(recetaRepository).save(any(Receta.class));
    }

    @Test
    void crear_nombreDuplicado_lanzaBusinessException() {
        RecetaRequest request = requestBase();
        when(recetaRepository.existsByNombreIgnoreCase("Pechuga")).thenReturn(true);

        assertThatThrownBy(() -> recetaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe");
        verify(recetaRepository, never()).save(any());
    }

    @Test
    void crear_productoNoInsumo_lanzaBusinessException() {
        RecetaRequest request = requestBase();
        Producto normal = Producto.builder().id(10L).nombre("Coca").tipo(TipoProducto.NORMAL).activo(true).build();

        when(recetaRepository.existsByNombreIgnoreCase("Pechuga")).thenReturn(false);
        when(productoRepository.findById(10L)).thenReturn(Optional.of(normal));

        assertThatThrownBy(() -> recetaService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INSUMO");
        verify(recetaRepository, never()).save(any());
    }

    @Test
    void desactivar_marcaInactiva() {
        Receta receta = new Receta();
        receta.setId(1L);
        receta.setActivo(true);
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(receta));

        recetaService.desactivar(1L);

        assertThat(receta.getActivo()).isFalse();
        verify(recetaRepository).save(receta);
    }

    private RecetaRequest requestBase() {
        RecetaRequest request = new RecetaRequest();
        request.setNombre("Pechuga");
        request.setDescripcion("Almuerzo");
        request.setPreparacion("1. Cocinar");
        request.setTiempoTotalMin(30);
        request.setPorciones(1);
        request.setActivo(true);

        IngredienteRequest ing = new IngredienteRequest();
        ing.setProductoId(10L);
        ing.setCantidad(BigDecimal.ONE);
        ing.setUnidad(UnidadInsumo.UND);
        ing.setEsRemovible(true);
        ing.setOrden(0);
        request.setIngredientes(List.of(ing));
        return request;
    }
}
