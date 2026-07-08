package com.comandago.api.pedido.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AgregarDetallesRequest {

    @NotEmpty(message = "Debe incluir al menos un producto")
    @Valid
    private List<DetallePedidoItemRequest> detalles;
}
