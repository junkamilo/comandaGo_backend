package com.comandago.api.producto.dto.mapper;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.entity.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoResponse toResponse(Producto producto) {
        Categoria categoria = producto.getCategoria();
        Categoria padre = categoria.getCategoriaPadre();

        return ProductoResponse.builder()
                .id(producto.getId())
                .categoriaId(categoria.getId())
                .categoriaNombre(categoria.getNombre())
                .categoriaPadreNombre(padre != null ? padre.getNombre() : null)
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .precioPromocion(producto.getPrecioPromocion())
                .precioFinal(producto.getPrecioFinal())
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
        boolean promo = Boolean.TRUE.equals(request.getEsPromocion());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setPrecioPromocion(promo ? request.getPrecioPromocion() : null);
        producto.setTiempoPreparacionMin(request.getTiempoPreparacionMin());
        producto.setEsPromocion(promo);
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
        if (request.getTiempoPreparacionMin() != null) {
            producto.setTiempoPreparacionMin(request.getTiempoPreparacionMin());
        }
        if (request.getDisponible() != null) {
            producto.setDisponible(request.getDisponible());
        }
        if (request.getOrden() != null) {
            producto.setOrden(request.getOrden());
        }
        if (request.getEsPromocion() != null) {
            producto.setEsPromocion(request.getEsPromocion());
            if (!request.getEsPromocion()) {
                producto.setPrecioPromocion(null);
            }
        }
        if (request.getPrecioPromocion() != null) {
            producto.setPrecioPromocion(request.getPrecioPromocion());
        } else if (Boolean.FALSE.equals(request.getEsPromocion())) {
            producto.setPrecioPromocion(null);
        }
    }
}
