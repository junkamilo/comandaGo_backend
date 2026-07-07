package com.comandago.api.promocion.dto.response;

import com.comandago.api.promocion.entity.TipoPromocion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record PromocionResponse(
        Long id,
        String nombre,
        String descripcion,
        TipoPromocion tipo,
        BigDecimal valorPorcentaje,
        BigDecimal valorMonto,
        BigDecimal valorPrecio,
        Integer pagaCantidad,
        Integer llevaCantidad,
        OffsetDateTime fechaInicio,
        OffsetDateTime fechaFin,
        Integer usoMaximo,
        Integer usoActual,
        Boolean activo,
        Boolean vigente,
        List<ProductoPromoResponse> productos,
        OffsetDateTime fechaCreacion
) {
}
