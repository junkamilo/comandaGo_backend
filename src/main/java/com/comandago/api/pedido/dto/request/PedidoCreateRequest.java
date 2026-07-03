package com.comandago.api.pedido.dto.request;

import com.comandago.api.pedido.dto.validation.OrigenDestinoValidator;
import com.comandago.api.pedido.enums.OrigenPedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@OrigenDestinoValidator
public class PedidoCreateRequest {

    @NotNull(message = "El origen es obligatorio")
    private OrigenPedido origen;

    private Long usuarioId;

    private Long mesaId;

    @Size(max = 120, message = "El nombre del cliente no puede superar 120 caracteres")
    private String clienteNombre;

    @Size(max = 30, message = "El teléfono del cliente no puede superar 30 caracteres")
    private String clienteTelefono;

    @Size(max = 500, message = "La dirección no puede superar 500 caracteres")
    private String direccionEntrega;

    @Size(max = 1000, message = "Las notas no pueden superar 1000 caracteres")
    private String notas;

    @NotEmpty(message = "El pedido debe tener al menos un detalle")
    @Valid
    private List<DetallePedidoItemRequest> detalles;
}
