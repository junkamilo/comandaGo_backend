package com.comandago.api.categoria.dto.response;

import lombok.Builder;

@Builder
public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        Integer orden,
        Boolean activo
) {
}
