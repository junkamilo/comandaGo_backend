package com.comandago.api.promocion.service;

import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.promocion.dto.mapper.PromocionMapper;
import com.comandago.api.promocion.dto.request.PromocionRequest;
import com.comandago.api.promocion.dto.response.PromocionResponse;
import com.comandago.api.promocion.entity.Promocion;
import com.comandago.api.promocion.entity.TipoPromocion;
import com.comandago.api.promocion.repository.PromocionRepository;
import com.comandago.api.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PromocionServiceImplTest {

    @Mock
    private PromocionRepository promocionRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private PromocionMapper promocionMapper;

    @InjectMocks
    private PromocionServiceImpl promocionService;

    @Test
    void crear_porcentaje_ok() {
        PromocionRequest request = new PromocionRequest(
                "Happy Hour",
                "20% en bebidas",
                TipoPromocion.PORCENTAJE,
                new BigDecimal("20"),
                null,
                null,
                null,
                OffsetDateTime.now().minusHours(1),
                null,
                null,
                true,
                Set.of(1L)
        );
        Producto producto = Producto.builder().id(1L).nombre("Cerveza").activo(true).build();

        when(productoRepository.findAllById(Set.of(1L))).thenReturn(List.of(producto));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> {
            Promocion p = inv.getArgument(0);
            p.setId(10L);
            return p;
        });
        when(promocionMapper.toResponse(any(Promocion.class))).thenReturn(
                new PromocionResponse(
                        10L, "Happy Hour", null, TipoPromocion.PORCENTAJE,
                        new BigDecimal("20"), null, null, null,
                        request.fechaInicio(), null, null, 0, true, true,
                        List.of(), OffsetDateTime.now()));

        PromocionResponse response = promocionService.crear(request);

        assertThat(response.id()).isEqualTo(10L);
        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).save(captor.capture());
        assertThat(captor.getValue().getValorPorcentaje()).isEqualByComparingTo("20");
        assertThat(captor.getValue().getUsoActual()).isZero();
    }

    @Test
    void crear_porcentajeSinValor_lanzaBusinessException() {
        PromocionRequest request = new PromocionRequest(
                "Promo",
                null,
                TipoPromocion.PORCENTAJE,
                null,
                null,
                null,
                null,
                OffsetDateTime.now(),
                null,
                null,
                true,
                Set.of(1L)
        );

        assertThatThrownBy(() -> promocionService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("valor_porcentaje");
    }

    @Test
    void crear_pagaLleva_invalido_lanzaBusinessException() {
        PromocionRequest request = new PromocionRequest(
                "2x2",
                null,
                TipoPromocion.PAGA_X_LLEVA_Y,
                null,
                null,
                2,
                2,
                OffsetDateTime.now(),
                null,
                null,
                true,
                Set.of(1L)
        );

        assertThatThrownBy(() -> promocionService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayor a la que paga");
    }

    @Test
    void crear_productoInactivo_lanzaBusinessException() {
        PromocionRequest request = new PromocionRequest(
                "Promo",
                null,
                TipoPromocion.MONTO_FIJO,
                null,
                new BigDecimal("1000"),
                null,
                null,
                OffsetDateTime.now(),
                null,
                null,
                true,
                Set.of(1L)
        );
        Producto inactivo = Producto.builder().id(1L).nombre("Pizza").activo(false).build();

        when(productoRepository.findAllById(Set.of(1L))).thenReturn(List.of(inactivo));

        assertThatThrownBy(() -> promocionService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void actualizar_cambiaTipo_limpiaCamposAnteriores() {
        Promocion existente = Promocion.builder()
                .id(5L)
                .tipo(TipoPromocion.PORCENTAJE)
                .valorPorcentaje(new BigDecimal("10"))
                .usoActual(3)
                .activo(true)
                .build();
        PromocionRequest request = new PromocionRequest(
                "Monto fijo",
                null,
                TipoPromocion.MONTO_FIJO,
                null,
                new BigDecimal("2000"),
                null,
                null,
                OffsetDateTime.now().minusDays(1),
                null,
                null,
                true,
                Set.of(2L)
        );
        Producto producto = Producto.builder().id(2L).nombre("Hamburguesa").activo(true).build();

        when(promocionRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(productoRepository.findAllById(Set.of(2L))).thenReturn(List.of(producto));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(promocionMapper.toResponse(any(Promocion.class))).thenReturn(
                new PromocionResponse(
                        5L, "Monto fijo", null, TipoPromocion.MONTO_FIJO,
                        null, new BigDecimal("2000"), null, null,
                        request.fechaInicio(), null, null, 3, true, true,
                        List.of(), OffsetDateTime.now()));

        promocionService.actualizar(5L, request);

        ArgumentCaptor<Promocion> captor = ArgumentCaptor.forClass(Promocion.class);
        verify(promocionRepository).save(captor.capture());
        Promocion guardada = captor.getValue();
        assertThat(guardada.getValorPorcentaje()).isNull();
        assertThat(guardada.getValorMonto()).isEqualByComparingTo("2000");
        assertThat(guardada.getUsoActual()).isEqualTo(3);
    }

    @Test
    void desactivar_marcaActivoFalse() {
        Promocion promocion = Promocion.builder().id(1L).activo(true).build();
        when(promocionRepository.findById(1L)).thenReturn(Optional.of(promocion));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> inv.getArgument(0));

        promocionService.desactivar(1L);

        assertThat(promocion.getActivo()).isFalse();
    }

    @Test
    void incrementarUso_sumaUno() {
        Promocion promocion = Promocion.builder().id(1L).usoActual(4).activo(true).build();
        when(promocionRepository.findById(1L)).thenReturn(Optional.of(promocion));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(inv -> inv.getArgument(0));

        promocionService.incrementarUso(1L);

        assertThat(promocion.getUsoActual()).isEqualTo(5);
    }
}
