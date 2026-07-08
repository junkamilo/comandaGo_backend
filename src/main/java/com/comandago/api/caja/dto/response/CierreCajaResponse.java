package com.comandago.api.caja.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CierreCajaResponse(
        Long id,
        String cajeroNombre,
        OffsetDateTime fechaApertura,
        OffsetDateTime fechaCierre,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta,
        BigDecimal totalNequi,
        BigDecimal totalDaviplata,
        BigDecimal totalTransferencia,
        BigDecimal totalOtros,
        BigDecimal totalPropinas,
        BigDecimal totalGeneral,
        BigDecimal efectivoContado,
        BigDecimal diferencia,
        Integer pedidosAtendidos,
        Integer pedidosCancelados,
        String notas
) {}
