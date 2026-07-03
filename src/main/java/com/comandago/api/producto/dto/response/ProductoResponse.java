package com.comandago.api.producto.dto.response;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record ProductoResponse(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        String categoriaPadreNombre,
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal precioPromocion,
        BigDecimal precioFinal,
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
