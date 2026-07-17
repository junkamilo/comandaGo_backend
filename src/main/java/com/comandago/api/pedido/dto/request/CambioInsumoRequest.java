package com.comandago.api.pedido.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CambioInsumoRequest {

    @NotNull(message = "El producto de origen es obligatorio")
    private Long desdeProductoId;

    @NotNull(message = "El producto de destino es obligatorio")
    private Long haciaProductoId;
}
