package com.comandago.api.pedido.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetallePedidoUpdateRequest {

    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    private String notasPreparacion;
}
