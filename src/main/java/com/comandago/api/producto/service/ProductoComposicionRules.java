package com.comandago.api.producto.service;

import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoInsumoRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.shared.exception.BusinessException;

import java.util.List;

public final class ProductoComposicionRules {

    private ProductoComposicionRules() {
    }

    /**
     * Legacy: composición embebida ya no se usa en altas/actualizaciones.
     * Se conserva para lectura de productos antiguos sin receta.
     */
    public static void validarTipoComposicion(TipoProducto tipo, List<ProductoInsumoRequest> composicion) {
        boolean vacia = composicion == null || composicion.isEmpty();
        if (!vacia) {
            throw new BusinessException(
                    "La composición embebida ya no se usa. Asigna una receta al producto compuesto");
        }
    }

    public static void validarTipoComposicionAlCrear(ProductoCreateRequest request) {
        validarTipoComposicion(request.getTipo(), request.getComposicion());
    }

    public static void validarTipoComposicionAlActualizar(TipoProducto tipoResultante, ProductoUpdateRequest request) {
        if (request.getComposicion() != null) {
            validarTipoComposicion(tipoResultante, request.getComposicion());
        }
    }
}
