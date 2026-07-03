package com.comandago.api.producto.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoDisponibilidadRequest {

    @NotNull(message = "El campo disponible es obligatorio")
    private Boolean disponible;
}
