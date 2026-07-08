package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;

public final class PedidoDetalleEdicionRules {

    private PedidoDetalleEdicionRules() {
    }

    public static void validarCancelable(DetallePedido detalle) {
        if (detalle.getEstado() != EstadoDetalle.PENDIENTE) {
            throw new BusinessException(
                    "El plato '" + detalle.getNombreProducto() + "' ya está en "
                            + detalle.getEstado() + " y no se puede cancelar ni cambiar. "
                            + "Si ya está en preparación, se puede pedir para llevar.");
        }
    }

    public static void validarPedidoEditable(Pedido pedido) {
        if (pedido.getEstado() == EstadoPedido.ENTREGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new BusinessException(
                    "No se puede modificar un pedido que ya fue "
                            + pedido.getEstado().name().toLowerCase().replace("_", " "));
        }
    }

    public static void validarPedidoCancelableCompleto(Pedido pedido) {
        boolean hayEnProceso = pedido.getDetalles().stream()
                .anyMatch(d -> d.getEstado() != EstadoDetalle.PENDIENTE
                        && d.getEstado() != EstadoDetalle.CANCELADO);

        if (hayEnProceso) {
            throw new BusinessException(
                    "No se puede cancelar el pedido completo porque hay platos en preparación. "
                            + "Cancela los ítems pendientes individualmente o pídelos para llevar.");
        }
    }

    public static boolean todosDetallesCancelados(Pedido pedido) {
        return pedido.getDetalles().stream().allMatch(d -> d.getEstado() == EstadoDetalle.CANCELADO);
    }

    public static DetallePedido buscarDetalle(Pedido pedido, Long detalleId) {
        return pedido.getDetalles().stream()
                .filter(d -> d.getId().equals(detalleId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Detalle no encontrado con id: " + detalleId));
    }
}
