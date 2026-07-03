package com.comandago.api.pago.dto.mapper;

import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.pago.entity.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoResponse toResponse(Pago pago) {
        return PagoResponse.builder()
                .id(pago.getId())
                .pedidoId(pago.getPedido().getId())
                .usuarioId(pago.getUsuario() != null ? pago.getUsuario().getId() : null)
                .metodo(pago.getMetodo())
                .monto(pago.getMonto())
                .referencia(pago.getReferencia())
                .fechaPago(pago.getFechaPago())
                .build();
    }
}
