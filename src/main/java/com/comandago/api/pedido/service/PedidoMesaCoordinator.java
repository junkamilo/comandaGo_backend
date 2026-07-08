package com.comandago.api.pedido.service;

import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PedidoMesaCoordinator {

    public static final List<EstadoPedido> ESTADOS_ACTIVOS = List.of(
            EstadoPedido.POR_CONFIRMAR,
            EstadoPedido.EN_PREPARACION,
            EstadoPedido.LISTO,
            EstadoPedido.EN_CAMINO
    );

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;

    public void ocuparMesa(Mesa mesa) {
        mesa.setEstado(EstadoMesa.OCUPADA);
        mesaRepository.save(mesa);
    }

    /**
     * Libera la mesa solo si no quedan pedidos con cuenta pendiente (no cancelados y sin pagar).
     */
    public void liberarMesaSiCorresponde(Pedido pedido) {
        if (pedido.getMesa() == null) {
            return;
        }
        Long mesaId = pedido.getMesa().getId();
        if (!mesaTieneCuentaPendiente(mesaId)) {
            Mesa mesa = mesaRepository.findById(mesaId).orElse(pedido.getMesa());
            mesa.setEstado(EstadoMesa.LIBRE);
            mesaRepository.save(mesa);
        }
    }

    public boolean mesaTieneCuentaPendiente(Long mesaId) {
        return pedidoRepository.existsByMesaIdAndEstadoNotAndEstadoPagoNot(
                mesaId, EstadoPedido.CANCELADO, EstadoPago.PAGADO);
    }

    public Collection<EstadoPedido> estadosActivos() {
        return ESTADOS_ACTIVOS;
    }
}
