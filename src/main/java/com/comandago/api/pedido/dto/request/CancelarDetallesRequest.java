package com.comandago.api.pedido.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class CancelarDetallesRequest {

    @NotEmpty(message = "Debe indicar al menos un ítem a cancelar")
    private Set<Long> detalleIds;
}
