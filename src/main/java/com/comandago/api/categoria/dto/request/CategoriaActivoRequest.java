package com.comandago.api.categoria.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoriaActivoRequest {

    @NotNull(message = "El campo activo es obligatorio")
    private Boolean activo;
}
