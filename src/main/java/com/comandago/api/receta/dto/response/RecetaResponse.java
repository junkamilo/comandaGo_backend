package com.comandago.api.receta.dto.response;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Builder
public record RecetaResponse(
        Long id,
        String nombre,
        String descripcion,
        String preparacion,
        Integer tiempoTotalMin,
        Integer porciones,
        Boolean activo,
        List<IngredienteResponse> ingredientes,
        Map<String, List<IngredienteResponse>> ingredientesPorCategoria,
        Integer totalIngredientes,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
