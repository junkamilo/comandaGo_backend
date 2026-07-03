package com.comandago.api.pedido.dto.request;

import com.comandago.api.pedido.enums.EstadoPedido;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoPedido estado;
}
