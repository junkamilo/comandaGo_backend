package com.comandago.api.producto.dto.response;

import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.receta.dto.response.RecetaResponse;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

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
        /** Derivado de la receta (tiempoTotalMin); null si el producto no tiene receta. */
        Integer tiempoPreparacionMin,
        Boolean esPromocion,
        Boolean disponible,
        Boolean activo,
        Integer orden,
        TipoProducto tipo,
        List<ProductoInsumoResponse> composicion,
        Long recetaId,
        String recetaNombre,
        RecetaResponse receta,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
