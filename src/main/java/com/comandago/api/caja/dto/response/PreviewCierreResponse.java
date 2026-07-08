package com.comandago.api.caja.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PreviewCierreResponse(
        OffsetDateTime fechaApertura,
        OffsetDateTime fechaActual,
        BigDecimal totalEfectivo,
        BigDecimal totalTarjeta,
        BigDecimal totalNequi,
        BigDecimal totalDaviplata,
        BigDecimal totalTransferencia,
        BigDecimal totalOtros,
        BigDecimal totalPropinas,
        BigDecimal totalGeneral,
        Integer pedidosAtendidos,
        Integer pedidosCancelados
) {}
