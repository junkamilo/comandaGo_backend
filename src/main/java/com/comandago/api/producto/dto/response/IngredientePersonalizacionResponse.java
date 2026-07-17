package com.comandago.api.producto.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record IngredientePersonalizacionResponse(
        Long productoId,
        String nombre,
        Long categoriaId,
        String categoriaNombre,
        Boolean esRemovible,
        Integer orden,
        List<AlternativaInsumoResponse> alternativas
) {
}
