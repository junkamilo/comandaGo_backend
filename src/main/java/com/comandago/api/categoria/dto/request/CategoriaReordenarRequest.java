package com.comandago.api.categoria.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CategoriaReordenarRequest(
        @NotEmpty(message = "La lista de IDs no puede estar vacía")
        List<Long> ids,
        Long categoriaPadreId
) {
}
