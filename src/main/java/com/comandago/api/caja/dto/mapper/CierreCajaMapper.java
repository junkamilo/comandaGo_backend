package com.comandago.api.caja.dto.mapper;

import com.comandago.api.caja.dto.response.CierreCajaResponse;
import com.comandago.api.caja.entity.CierreCaja;
import org.springframework.stereotype.Component;

@Component
public class CierreCajaMapper {

    public CierreCajaResponse toResponse(CierreCaja cierre) {
        return new CierreCajaResponse(
                cierre.getId(),
                cierre.getUsuario() != null ? cierre.getUsuario().getNombre() : null,
                cierre.getFechaApertura(),
                cierre.getFechaCierre(),
                cierre.getTotalEfectivo(),
                cierre.getTotalTarjeta(),
                cierre.getTotalNequi(),
                cierre.getTotalDaviplata(),
                cierre.getTotalTransferencia(),
                cierre.getTotalOtros(),
                cierre.getTotalPropinas(),
                cierre.getTotalGeneral(),
                cierre.getEfectivoContado(),
                cierre.getDiferencia(),
                cierre.getPedidosAtendidos(),
                cierre.getPedidosCancelados(),
                cierre.getNotas()
        );
    }
}
