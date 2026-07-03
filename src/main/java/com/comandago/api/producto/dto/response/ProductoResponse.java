package com.comandago.api.producto.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record ProductoResponse(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal precioPromocion,
        BigDecimal precioEfectivo,
        String imagenUrl,
        Integer tiempoPreparacionMin,
        Boolean esPromocion,
        Boolean disponible,
        Boolean activo,
        Integer orden,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
