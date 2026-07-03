package com.comandago.api.pedido.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PedidoUpdateRequest {

    @Size(max = 120, message = "El nombre del cliente no puede superar 120 caracteres")
    private String clienteNombre;

    @Size(max = 30, message = "El teléfono del cliente no puede superar 30 caracteres")
    private String clienteTelefono;

    @Size(max = 500, message = "La dirección no puede superar 500 caracteres")
    private String direccionEntrega;

    @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres")
    private String notas;
}
