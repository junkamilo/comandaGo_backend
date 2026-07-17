package com.comandago.api.producto.service;

import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoInsumoRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.shared.exception.BusinessException;

import java.util.List;

public final class ProductoRecetaRules {

    private ProductoRecetaRules() {
    }

    public static void validarRecetaAlCrear(ProductoCreateRequest request) {
        TipoProducto tipo = request.getTipo() != null ? request.getTipo() : TipoProducto.NORMAL;
        validarRecetaPorTipo(tipo, request.getRecetaId());
        validarNoMezclarComposicion(tipo, request.getRecetaId(), request.getComposicion());
    }

    public static void validarRecetaAlActualizar(TipoProducto tipoResultante, ProductoUpdateRequest request,
                                                 Long recetaIdActual) {
        Long recetaIdResultante = request.getRecetaId() != null ? request.getRecetaId() : recetaIdActual;
        if (request.getRecetaId() != null || request.getTipo() != null) {
            if (tipoResultante == TipoProducto.COMPUESTO) {
                if (recetaIdResultante == null) {
                    throw new BusinessException("Un producto compuesto debe tener una receta asignada");
                }
            } else if (request.getRecetaId() != null) {
                throw new BusinessException("Solo los productos de tipo COMPUESTO pueden tener receta");
            }
        }
        if (tipoResultante == TipoProducto.COMPUESTO && recetaIdResultante == null) {
            throw new BusinessException("Un producto compuesto debe tener una receta asignada");
        }
        if (request.getComposicion() != null && !request.getComposicion().isEmpty()) {
            throw new BusinessException(
                    "La composición embebida ya no se usa. Asigna una receta al producto compuesto");
        }
    }

    public static void validarRecetaPorTipo(TipoProducto tipo, Long recetaId) {
        TipoProducto efectivo = tipo != null ? tipo : TipoProducto.NORMAL;
        if (efectivo == TipoProducto.COMPUESTO) {
            if (recetaId == null) {
                throw new BusinessException("Un producto compuesto debe tener una receta asignada");
            }
        } else if (recetaId != null) {
            throw new BusinessException("Solo los productos de tipo COMPUESTO pueden tener receta");
        }
    }

    private static void validarNoMezclarComposicion(TipoProducto tipo, Long recetaId,
                                                    List<ProductoInsumoRequest> composicion) {
        boolean tieneComposicion = composicion != null && !composicion.isEmpty();
        if (tieneComposicion) {
            throw new BusinessException(
                    "La composición embebida ya no se usa. Asigna una receta al producto compuesto");
        }
        if (tipo == TipoProducto.COMPUESTO && recetaId == null) {
            throw new BusinessException("Un producto compuesto debe tener una receta asignada");
        }
    }
}
