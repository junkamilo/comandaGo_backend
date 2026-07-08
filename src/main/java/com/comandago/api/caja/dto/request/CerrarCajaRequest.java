package com.comandago.api.caja.dto.request;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record CerrarCajaRequest(
        @DecimalMin(value = "0.0", message = "El efectivo contado no puede ser negativo")
        BigDecimal efectivoContado,
        String notas
) {}
