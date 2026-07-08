package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PedidoDetalleEstadoPromoter {

    private final PedidoRepository pedidoRepository;

    public void aplicarPromocionPorEstadoDetalles(Pedido pedido) {
        if (PedidoDetalleEstadoRules.debePromoverPedidoAListo(pedido)) {
            pedido.setEstado(EstadoPedido.LISTO);
            pedidoRepository.save(pedido);
            return;
        }
        if (PedidoDetalleEstadoRules.debePromoverPedidoAEntregado(pedido)) {
            PedidoEstadoTransition.validar(pedido.getEstado(), EstadoPedido.ENTREGADO);
            pedido.setEstado(EstadoPedido.ENTREGADO);
            pedidoRepository.save(pedido);
        }
    }
}
