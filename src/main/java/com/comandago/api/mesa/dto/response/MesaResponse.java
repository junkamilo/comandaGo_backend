package com.comandago.api.mesa.dto.response;

import com.comandago.api.mesa.enums.EstadoMesa;
import lombok.Builder;

import java.util.List;

@Builder
public record MesaResponse(
        Long id,
        String numero,
        String nombre,
        Integer capacidad,
        String qrToken,
        EstadoMesa estado,
        Boolean activo,
        String grupoId,
        List<String> mesasDelGrupo
) {
}
