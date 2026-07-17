package com.comandago.api.producto.dto.request;

import com.comandago.api.producto.enums.UnidadInsumo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductoInsumoRequest {

    @NotNull(message = "El insumo es obligatorio")
    private Long productoInsumoId;

    @DecimalMin(value = "0.01", message = "La cantidad debe ser mayor a 0")
    @Digits(integer = 6, fraction = 2)
    private BigDecimal cantidad;

    private UnidadInsumo unidad;

    private Boolean esRemovible;

    private Boolean esExtra;

    @DecimalMin(value = "0.00", message = "El precio extra debe ser mayor o igual a 0")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal precioExtra;

    private Integer orden;
}
