package com.comandago.api.shared.promocion;

import com.comandago.api.promocion.entity.Promocion;
import com.comandago.api.promocion.entity.TipoPromocion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadorPromocionTest {

    @Test
    void precioConDescuento_porcentaje20_aplicaDescuento() {
        Promocion promo = Promocion.builder()
                .tipo(TipoPromocion.PORCENTAJE)
                .valorPorcentaje(new BigDecimal("20"))
                .build();

        BigDecimal resultado = CalculadorPromocion.precioConDescuento(new BigDecimal("10000"), promo);

        assertThat(resultado).isEqualByComparingTo("8000");
    }

    @Test
    void precioConDescuento_montoFijo_aplicaDescuento() {
        Promocion promo = Promocion.builder()
                .tipo(TipoPromocion.MONTO_FIJO)
                .valorMonto(new BigDecimal("5000"))
                .build();

        BigDecimal resultado = CalculadorPromocion.precioConDescuento(new BigDecimal("8000"), promo);

        assertThat(resultado).isEqualByComparingTo("3000");
    }

    @Test
    void precioConDescuento_precioFijo_usaPrecioDirecto() {
        Promocion promo = Promocion.builder()
                .tipo(TipoPromocion.PRECIO_FIJO)
                .valorPrecio(new BigDecimal("7000"))
                .build();

        BigDecimal resultado = CalculadorPromocion.precioConDescuento(new BigDecimal("10000"), promo);

        assertThat(resultado).isEqualByComparingTo("7000");
    }

    @Test
    void totalPagaXLlevaY_seisUnidades_cobraCuatro() {
        Promocion promo = Promocion.builder()
                .tipo(TipoPromocion.PAGA_X_LLEVA_Y)
                .pagaCantidad(2)
                .llevaCantidad(3)
                .build();

        BigDecimal total = CalculadorPromocion.totalPagaXLlevaY(new BigDecimal("8000"), 6, promo);

        assertThat(total).isEqualByComparingTo("32000");
    }

    @Test
    void precioUnitarioEfectivo_pagaXLlevaY_devuelvePromedio() {
        Promocion promo = Promocion.builder()
                .tipo(TipoPromocion.PAGA_X_LLEVA_Y)
                .pagaCantidad(2)
                .llevaCantidad(3)
                .build();

        BigDecimal unitario = CalculadorPromocion.precioUnitarioEfectivo(new BigDecimal("8000"), 6, promo);

        assertThat(unitario).isEqualByComparingTo("5333.33");
        assertThat(CalculadorPromocion.totalPagaXLlevaY(new BigDecimal("8000"), 6, promo))
                .isEqualByComparingTo("32000");
    }
}
