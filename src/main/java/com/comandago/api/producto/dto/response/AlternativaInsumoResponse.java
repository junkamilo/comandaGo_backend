package com.comandago.api.producto.dto.response;

import lombok.Builder;

@Builder
public record AlternativaInsumoResponse(
        Long productoId,
        String nombre
) {
}
