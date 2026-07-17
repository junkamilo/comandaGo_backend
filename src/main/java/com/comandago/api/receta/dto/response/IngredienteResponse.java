package com.comandago.api.receta.dto.response;

import com.comandago.api.producto.enums.UnidadInsumo;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record IngredienteResponse(
        Long id,
        Long productoId,
        String productoNombre,
        String productoImagenUrl,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal cantidad,
        UnidadInsumo unidad,
        Boolean esRemovible,
        Integer orden
) {
}
