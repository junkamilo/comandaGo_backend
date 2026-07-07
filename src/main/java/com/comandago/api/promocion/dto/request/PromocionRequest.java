package com.comandago.api.promocion.dto.request;

import com.comandago.api.promocion.entity.TipoPromocion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;

public record PromocionRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        String descripcion,

        @NotNull(message = "El tipo es obligatorio")
        TipoPromocion tipo,

        BigDecimal valorPorcentaje,
        BigDecimal valorMonto,
        Integer pagaCantidad,
        Integer llevaCantidad,

        @NotNull(message = "La fecha de inicio es obligatoria")
        OffsetDateTime fechaInicio,

        OffsetDateTime fechaFin,
        Integer usoMaximo,
        Boolean activo,

        @NotEmpty(message = "Debe incluir al menos un producto")
        Set<Long> productoIds
) {
}
