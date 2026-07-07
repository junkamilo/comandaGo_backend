package com.comandago.api.producto.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProductoReordenarRequest(
        @NotEmpty(message = "La lista de IDs no puede estar vacía")
        List<Long> ids,
        @NotNull(message = "La categoría es obligatoria")
        Long categoriaId
) {
}
