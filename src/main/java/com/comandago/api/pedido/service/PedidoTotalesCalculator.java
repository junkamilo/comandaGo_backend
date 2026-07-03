package com.comandago.api.pedido.service;

import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class PedidoTotalesCalculator {

    public static final BigDecimal IMPOCONSUMO = new BigDecimal("0.08");

    private final PedidoRepository pedidoRepository;
    private final EntityManager entityManager;

    public void aplicarImpuestos(Pedido pedido) {
        Long pedidoId = pedido.getId();
        if (pedidoId == null) {
            throw new IllegalStateException("El pedido debe estar persistido antes de calcular impuestos");
        }

        BigDecimal subtotal = resolverSubtotal(pedido);
        BigDecimal impuestos = subtotal.multiply(IMPOCONSUMO).setScale(2, RoundingMode.HALF_UP);
        pedidoRepository.actualizarImpuestosYTotal(pedidoId, impuestos);
        sincronizarTotalesEnMemoria(pedido, subtotal, impuestos);
    }

    public void aplicarImpuestos(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId).orElseThrow();
        aplicarImpuestos(pedido);
    }

    private BigDecimal resolverSubtotal(Pedido pedido) {
        BigDecimal subtotal = pedido.getSubtotal();
        if (subtotal != null) {
            return subtotal;
        }

        Pedido managed = pedidoRepository.findById(pedido.getId()).orElseThrow();
        subtotal = managed.getSubtotal();
        if (subtotal == null) {
            entityManager.refresh(managed);
            subtotal = managed.getSubtotal();
        }
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        pedido.setSubtotal(subtotal);
        return subtotal;
    }

    private void sincronizarTotalesEnMemoria(Pedido pedido, BigDecimal subtotal, BigDecimal impuestos) {
        BigDecimal descuento = pedido.getDescuento() != null ? pedido.getDescuento() : BigDecimal.ZERO;
        BigDecimal costoEnvio = pedido.getCostoEnvio() != null ? pedido.getCostoEnvio() : BigDecimal.ZERO;

        pedido.setSubtotal(subtotal);
        pedido.setImpuestos(impuestos);
        pedido.setTotal(subtotal.subtract(descuento).add(impuestos).add(costoEnvio));
    }
}
