package com.comandago.api.pago.dto.mapper;

import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.pago.entity.Pago;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoResponse toResponse(Pago pago) {
        return new PagoResponse(
                pago.getId(),
                pago.getPedido().getId(),
                pago.getPedido().getNumeroPedido(),
                pago.getUsuario() != null ? pago.getUsuario().getNombre() : null,
                pago.getMetodo(),
                pago.getEstado(),
                pago.getMonto(),
                pago.getPropina(),
                pago.getMontoRecibido(),
                pago.getVuelto(),
                pago.getReferencia(),
                pago.getProveedorId(),
                pago.getNotas(),
                pago.getFechaPago()
        );
    }
}
