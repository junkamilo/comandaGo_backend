package com.comandago.api.producto.dto.mapper;

import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.entity.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoResponse toResponse(Producto producto) {
        return ProductoResponse.builder()
                .id(producto.getId())
                .categoriaId(producto.getCategoria().getId())
                .categoriaNombre(producto.getCategoria().getNombre())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .precioPromocion(producto.getPrecioPromocion())
                .precioEfectivo(producto.getPrecioEfectivo())
                .imagenUrl(producto.getImagenUrl())
                .tiempoPreparacionMin(producto.getTiempoPreparacionMin())
                .esPromocion(producto.getEsPromocion())
                .disponible(producto.getDisponible())
                .activo(producto.getActivo())
                .orden(producto.getOrden())
                .fechaCreacion(producto.getFechaCreacion())
                .fechaActualizacion(producto.getFechaActualizacion())
                .build();
    }

    public void applyCreate(Producto producto, ProductoCreateRequest request) {
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setPrecioPromocion(request.getPrecioPromocion());
        producto.setImagenUrl(request.getImagenUrl());
        producto.setTiempoPreparacionMin(request.getTiempoPreparacionMin());
        producto.setEsPromocion(request.getEsPromocion() != null ? request.getEsPromocion() : false);
        producto.setOrden(request.getOrden() != null ? request.getOrden() : 0);
        producto.setDisponible(true);
        producto.setActivo(true);
    }

    public void applyUpdate(Producto producto, ProductoUpdateRequest request) {
        if (request.getNombre() != null) {
            producto.setNombre(request.getNombre());
        }
        if (request.getDescripcion() != null) {
            producto.setDescripcion(request.getDescripcion());
        }
        if (request.getPrecio() != null) {
            producto.setPrecio(request.getPrecio());
        }
        if (request.getPrecioPromocion() != null) {
            producto.setPrecioPromocion(request.getPrecioPromocion());
        }
        if (request.getImagenUrl() != null) {
            producto.setImagenUrl(request.getImagenUrl());
        }
        if (request.getTiempoPreparacionMin() != null) {
            producto.setTiempoPreparacionMin(request.getTiempoPreparacionMin());
        }
        if (request.getEsPromocion() != null) {
            producto.setEsPromocion(request.getEsPromocion());
        }
        if (request.getDisponible() != null) {
            producto.setDisponible(request.getDisponible());
        }
        if (request.getOrden() != null) {
            producto.setOrden(request.getOrden());
        }
    }
}
