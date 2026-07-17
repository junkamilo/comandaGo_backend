package com.comandago.api.receta.dto.request;

import com.comandago.api.producto.enums.UnidadInsumo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class IngredienteRequest {

    @NotNull(message = "El producto es obligatorio")
    private Long productoId;

    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 6, fraction = 2, message = "La cantidad tiene formato inválido")
    private BigDecimal cantidad;

    private UnidadInsumo unidad;

    private Boolean esRemovible;

    private Integer orden;
}
