package com.comandago.api.mesa.service;

import com.comandago.api.mesa.dto.request.MesaCreateRequest;
import com.comandago.api.mesa.dto.request.MesaUpdateRequest;
import com.comandago.api.mesa.dto.response.MesaResponse;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface MesaService {

    MesaResponse crear(MesaCreateRequest request);

    MesaResponse obtenerPorId(Long id);

    MesaResponse obtenerPorQrToken(String token);

    PageResponse<MesaResponse> listar(EstadoMesa estado, Boolean activo, Pageable pageable);

    List<MesaResponse> listarPiso();

    List<MesaResponse> listarLibres();

    MesaResponse actualizar(Long id, MesaUpdateRequest request);

    MesaResponse actualizarEstado(Long id, EstadoMesa estado);

    List<MesaResponse> agrupar(List<Long> mesaIds);

    void desagrupar(String grupoId);

    void eliminar(Long id);
}
