package com.comandago.api.producto.service;

import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.shared.exception.BusinessException;

public final class ProductoCategoriaRules {

    private ProductoCategoriaRules() {
    }

    public static void validarCategoriaPorTipo(TipoProducto tipo, Long categoriaId) {
        TipoProducto efectivo = tipo != null ? tipo : TipoProducto.NORMAL;
        if (efectivo != TipoProducto.INSUMO && categoriaId == null) {
            throw new BusinessException(
                    "Los productos tipo " + efectivo + " requieren categoría");
        }
    }
}
