package com.comandago.api.mesa.service;

import com.comandago.api.mesa.dto.mapper.MesaMapper;
import com.comandago.api.mesa.dto.request.MesaCreateRequest;
import com.comandago.api.mesa.dto.response.MesaResponse;
import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.repository.PedidoRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MesaServiceImplTest {

    @Mock
    private MesaRepository mesaRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private MesaMapper mesaMapper;

    @InjectMocks
    private MesaServiceImpl mesaService;

    @Test
    void agrupar_dosMesasLibres_asignaMismoGrupoIdYMarcaOcupada() {
        Mesa mesa4 = mesaLibre(4L, "4");
        Mesa mesa5 = mesaLibre(5L, "5");

        when(mesaRepository.findAllById(List.of(4L, 5L))).thenReturn(List.of(mesa4, mesa5));
        when(mesaRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mesaRepository.findByGrupoIdIn(anyCollection())).thenAnswer(inv -> {
            String grupoId = mesa4.getGrupoId();
            return List.of(mesa4, mesa5);
        });
        when(mesaMapper.toResponse(eq(mesa4), eq(List.of("5")))).thenReturn(
                MesaResponse.builder().id(4L).numero("4").grupoId("grupo-1").mesasDelGrupo(List.of("5")).build());
        when(mesaMapper.toResponse(eq(mesa5), eq(List.of("4")))).thenReturn(
                MesaResponse.builder().id(5L).numero("5").grupoId("grupo-1").mesasDelGrupo(List.of("4")).build());

        List<MesaResponse> response = mesaService.agrupar(List.of(4L, 5L));

        assertThat(mesa4.getGrupoId()).isNotNull();
        assertThat(mesa4.getGrupoId()).isEqualTo(mesa5.getGrupoId());
        assertThat(mesa4.getEstado()).isEqualTo(EstadoMesa.OCUPADA);
        assertThat(mesa5.getEstado()).isEqualTo(EstadoMesa.OCUPADA);
        assertThat(response).hasSize(2);
        verify(mesaRepository).saveAll(any());
    }

    @Test
    void agrupar_mesaYaEnGrupo_lanzaBusinessException() {
        Mesa mesa4 = mesaLibre(4L, "4");
        mesa4.setGrupoId("existente");
        Mesa mesa5 = mesaLibre(5L, "5");

        when(mesaRepository.findAllById(List.of(4L, 5L))).thenReturn(List.of(mesa4, mesa5));

        assertThatThrownBy(() -> mesaService.agrupar(List.of(4L, 5L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está en un grupo");
    }

    @Test
    void agrupar_unSoloId_lanzaBusinessException() {
        assertThatThrownBy(() -> mesaService.agrupar(List.of(4L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos 2 mesas");
    }

    @Test
    void desagrupar_limpiaGrupoIdYMarcaLibre() {
        Mesa mesa4 = mesaLibre(4L, "4");
        mesa4.setGrupoId("abc-123");
        mesa4.setEstado(EstadoMesa.OCUPADA);
        Mesa mesa5 = mesaLibre(5L, "5");
        mesa5.setGrupoId("abc-123");
        mesa5.setEstado(EstadoMesa.OCUPADA);

        when(mesaRepository.findByGrupoId("abc-123")).thenReturn(List.of(mesa4, mesa5));

        mesaService.desagrupar("abc-123");

        assertThat(mesa4.getGrupoId()).isNull();
        assertThat(mesa5.getGrupoId()).isNull();
        assertThat(mesa4.getEstado()).isEqualTo(EstadoMesa.LIBRE);
        assertThat(mesa5.getEstado()).isEqualTo(EstadoMesa.LIBRE);
        verify(mesaRepository).saveAll(List.of(mesa4, mesa5));
    }

    @Test
    void desagrupar_grupoInexistente_lanzaResourceNotFoundException() {
        when(mesaRepository.findByGrupoId("missing")).thenReturn(List.of());

        assertThatThrownBy(() -> mesaService.desagrupar("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void eliminar_mesaOcupada_lanzaBusinessException() {
        Mesa mesa = mesaLibre(1L, "1");
        mesa.setEstado(EstadoMesa.OCUPADA);

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThatThrownBy(() -> mesaService.eliminar(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ocupada");
    }

    @Test
    void eliminar_mesaEnGrupo_lanzaBusinessException() {
        Mesa mesa = mesaLibre(1L, "1");
        mesa.setGrupoId("abc-123");

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));

        assertThatThrownBy(() -> mesaService.eliminar(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Separa el grupo");
    }

    @Test
    void eliminar_conPedidosActivos_lanzaConflictException() {
        Mesa mesa = mesaLibre(1L, "1");

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.existsByMesaIdAndEstadoIn(eq(1L), any())).thenReturn(true);

        assertThatThrownBy(() -> mesaService.eliminar(1L))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void eliminar_mesaLibre_desactivaYMarcaInactiva() {
        Mesa mesa = mesaLibre(1L, "1");

        when(mesaRepository.findById(1L)).thenReturn(Optional.of(mesa));
        when(pedidoRepository.existsByMesaIdAndEstadoIn(eq(1L), any())).thenReturn(false);

        mesaService.eliminar(1L);

        assertThat(mesa.getActivo()).isFalse();
        assertThat(mesa.getEstado()).isEqualTo(EstadoMesa.INACTIVA);
        verify(mesaRepository).save(mesa);
    }

    @Test
    void listarPiso_incluyeMesasDelGrupo() {
        Mesa mesa4 = mesaLibre(4L, "4");
        mesa4.setGrupoId("abc-123");
        mesa4.setEstado(EstadoMesa.OCUPADA);
        Mesa mesa5 = mesaLibre(5L, "5");
        mesa5.setGrupoId("abc-123");
        mesa5.setEstado(EstadoMesa.OCUPADA);

        when(mesaRepository.findByActivoTrueOrderByNumeroAsc()).thenReturn(List.of(mesa4, mesa5));
        when(mesaRepository.findByGrupoIdIn(anyCollection())).thenReturn(List.of(mesa4, mesa5));
        when(mesaMapper.toResponse(eq(mesa4), eq(List.of("5")))).thenReturn(
                MesaResponse.builder().id(4L).numero("4").grupoId("abc-123").mesasDelGrupo(List.of("5")).build());
        when(mesaMapper.toResponse(eq(mesa5), eq(List.of("4")))).thenReturn(
                MesaResponse.builder().id(5L).numero("5").grupoId("abc-123").mesasDelGrupo(List.of("4")).build());

        List<MesaResponse> piso = mesaService.listarPiso();

        assertThat(piso).hasSize(2);
        assertThat(piso.get(0).mesasDelGrupo()).containsExactly("5");
        assertThat(piso.get(1).mesasDelGrupo()).containsExactly("4");
    }

    @Test
    void crear_conCapacidad_noValidaLimiteOperativo() {
        MesaCreateRequest request = new MesaCreateRequest();
        request.setNumero("12");
        request.setCapacidad(4);

        when(mesaRepository.existsByNumeroIgnoreCase("12")).thenReturn(false);
        when(mesaRepository.existsByQrToken(any())).thenReturn(false);
        when(mesaMapper.toEntity(eq(request), any())).thenReturn(Mesa.builder().numero("12").capacidad(4).build());
        when(mesaRepository.save(any(Mesa.class))).thenAnswer(inv -> {
            Mesa m = inv.getArgument(0);
            m.setId(1L);
            m.setQrToken(UUID.randomUUID().toString());
            m.setEstado(EstadoMesa.LIBRE);
            m.setActivo(true);
            return m;
        });
        when(mesaMapper.toResponse(any(Mesa.class))).thenReturn(
                MesaResponse.builder().id(1L).numero("12").capacidad(4).mesasDelGrupo(List.of()).build());

        MesaResponse response = mesaService.crear(request);

        assertThat(response.capacidad()).isEqualTo(4);
        verify(pedidoRepository, never()).existsByMesaIdAndEstadoIn(any(), any());
    }

    private Mesa mesaLibre(Long id, String numero) {
        return Mesa.builder()
                .id(id)
                .numero(numero)
                .estado(EstadoMesa.LIBRE)
                .activo(true)
                .qrToken(UUID.randomUUID().toString())
                .build();
    }
}
