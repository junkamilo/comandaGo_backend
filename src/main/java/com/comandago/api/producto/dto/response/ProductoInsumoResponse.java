package com.comandago.api.producto.dto.response;

import com.comandago.api.producto.enums.UnidadInsumo;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ProductoInsumoResponse(
        Long id,
        Long productoInsumoId,
        String nombre,
        BigDecimal cantidad,
        UnidadInsumo unidad,
        Boolean esRemovible,
        Boolean esExtra,
        BigDecimal precioExtra,
        Integer orden
) {
}
