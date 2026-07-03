package com.comandago.api.usuario.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioActivoRequest {

    @NotNull(message = "El campo activo es obligatorio")
    private Boolean activo;
}
