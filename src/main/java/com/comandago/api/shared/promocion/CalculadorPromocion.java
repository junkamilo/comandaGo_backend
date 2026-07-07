package com.comandago.api.shared.promocion;

import com.comandago.api.promocion.entity.Promocion;
import com.comandago.api.promocion.entity.TipoPromocion;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CalculadorPromocion {

    private CalculadorPromocion() {
    }

    public static BigDecimal precioConDescuento(BigDecimal precioOriginal, Promocion promo) {
        return switch (promo.getTipo()) {
            case PORCENTAJE -> {
                BigDecimal descuento = precioOriginal
                        .multiply(promo.getValorPorcentaje())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                yield precioOriginal.subtract(descuento).max(BigDecimal.ZERO);
            }
            case MONTO_FIJO -> precioOriginal.subtract(promo.getValorMonto()).max(BigDecimal.ZERO);
            case PAGA_X_LLEVA_Y -> precioOriginal;
        };
    }

    public static BigDecimal totalPagaXLlevaY(BigDecimal precioUnitario, int cantidad, Promocion promo) {
        int paga = promo.getPagaCantidad();
        int lleva = promo.getLlevaCantidad();

        int gruposCompletos = cantidad / lleva;
        int sobrantes = cantidad % lleva;
        int unidadesCobradas = (gruposCompletos * paga) + sobrantes;

        return precioUnitario.multiply(BigDecimal.valueOf(unidadesCobradas));
    }

    public static BigDecimal precioUnitarioEfectivo(BigDecimal precioOriginal, int cantidad, Promocion promo) {
        if (promo.getTipo() != TipoPromocion.PAGA_X_LLEVA_Y) {
            return precioConDescuento(precioOriginal, promo);
        }
        if (cantidad <= 0) {
            return precioOriginal;
        }
        BigDecimal total = totalPagaXLlevaY(precioOriginal, cantidad, promo);
        return total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }
}
