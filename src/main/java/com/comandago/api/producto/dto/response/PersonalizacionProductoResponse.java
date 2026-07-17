package com.comandago.api.producto.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PersonalizacionProductoResponse(
        Long productoId,
        String nombre,
        BigDecimal precioBase,
        List<IngredientePersonalizacionResponse> ingredientes
) {
}
