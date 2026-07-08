package com.comandago.api.pedido.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReemplazarDetalleRequest {

    @NotNull(message = "El nuevo producto es obligatorio")
    private Long nuevoProductoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad mínima es 1")
    private Integer cantidad;

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    private String notasPreparacion;
}
