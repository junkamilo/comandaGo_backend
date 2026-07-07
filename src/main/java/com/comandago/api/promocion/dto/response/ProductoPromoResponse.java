package com.comandago.api.promocion.dto.response;

import java.math.BigDecimal;

public record ProductoPromoResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        String imagenUrl
) {
}
