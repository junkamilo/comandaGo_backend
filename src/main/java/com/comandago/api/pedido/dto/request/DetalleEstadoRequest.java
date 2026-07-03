package com.comandago.api.pedido.dto.request;

import com.comandago.api.pedido.enums.EstadoDetalle;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetalleEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoDetalle estado;
}
