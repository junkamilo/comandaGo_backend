package com.comandago.api.pago.service;

import com.comandago.api.pago.dto.request.RegistrarPagoRequest;
import com.comandago.api.pago.enums.MetodoPago;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.shared.exception.BusinessException;

import java.math.BigDecimal;

public final class PagoReglas {

    private PagoReglas() {
    }

    public static void validarPedidoCobrable(Pedido pedido) {
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new BusinessException("No se puede cobrar un pedido cancelado");
        }
        if (pedido.getEstadoPago() == EstadoPago.PAGADO) {
            throw new BusinessException("Este pedido ya está completamente pagado");
        }
    }

    public static void validarEfectivo(RegistrarPagoRequest request) {
        if (request.getMetodo() != MetodoPago.EFECTIVO) {
            return;
        }
        if (request.getMontoRecibido() == null) {
            throw new BusinessException("Para pago en efectivo se requiere el monto recibido");
        }
        BigDecimal propina = request.getPropina() != null ? request.getPropina() : BigDecimal.ZERO;
        if (request.getMontoRecibido().compareTo(request.getMonto().add(propina)) < 0) {
            throw new BusinessException("El monto recibido no cubre la cuenta más la propina");
        }
    }

    public static void validarSobrepago(Pedido pedido, BigDecimal totalPagado, BigDecimal montoNuevo) {
        BigDecimal totalConNuevo = totalPagado.add(montoNuevo);
        if (totalConNuevo.compareTo(pedido.getTotal()) > 0) {
            BigDecimal saldo = pedido.getTotal().subtract(totalPagado);
            throw new BusinessException(
                    "El monto excede el saldo pendiente. El saldo es $" + saldo.toPlainString());
        }
    }
}
