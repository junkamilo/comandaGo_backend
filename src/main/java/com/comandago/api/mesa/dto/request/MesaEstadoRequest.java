package com.comandago.api.mesa.dto.request;

import com.comandago.api.mesa.enums.EstadoMesa;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MesaEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoMesa estado;
}
