package com.comandago.api.categoria.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        String imagenUrl,
        Integer orden,
        Boolean activo,
        Long categoriaPadreId,
        String categoriaPadreNombre,
        List<CategoriaResponse> subcategorias
) {
}
