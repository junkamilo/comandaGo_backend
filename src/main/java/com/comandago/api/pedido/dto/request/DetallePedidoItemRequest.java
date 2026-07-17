package com.comandago.api.pedido.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class DetallePedidoItemRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    private Integer cantidad;

    @Size(max = 500, message = "Las notas no pueden superar 500 caracteres")
    private String notasPreparacion;

    @DecimalMin(value = "0.00", message = "El precio unitario debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal precioUnitario;

    private List<Long> extrasIds;

    private List<Long> removidosIds;

    private List<CambioInsumoRequest> cambios;
}
