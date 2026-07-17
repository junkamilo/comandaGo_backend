package com.comandago.api.producto.dto.mapper;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.dto.response.ProductoInsumoResponse;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.entity.ProductoInsumo;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.receta.dto.mapper.RecetaMapper;
import com.comandago.api.receta.dto.response.RecetaResponse;
import com.comandago.api.receta.entity.Receta;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductoMapper {

    private final RecetaMapper recetaMapper;

    public ProductoResponse toResponse(Producto producto) {
        Categoria categoria = producto.getCategoria();
        Categoria padre = categoria != null ? categoria.getCategoriaPadre() : null;
        Receta receta = producto.getReceta();

        return ProductoResponse.builder()
                .id(producto.getId())
                .categoriaId(categoria != null ? categoria.getId() : null)
                .categoriaNombre(categoria != null ? categoria.getNombre() : null)
                .categoriaPadreNombre(padre != null ? padre.getNombre() : null)
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .precio(producto.getPrecio())
                .precioPromocion(producto.getPrecioPromocion())
                .precioFinal(producto.getPrecioFinal())
                .imagenUrl(producto.getImagenUrl())
                // Derivado de la receta (COMPUESTO); null si no hay receta.
                .tiempoPreparacionMin(receta != null ? receta.getTiempoTotalMin() : null)
                .esPromocion(producto.getEsPromocion())
                .disponible(producto.getDisponible())
                .activo(producto.getActivo())
                .orden(producto.getOrden() != null ? producto.getOrden() : 0)
                .tipo(producto.getTipo() != null ? producto.getTipo() : TipoProducto.NORMAL)
                .composicion(mapComposicion(producto))
                .recetaId(receta != null ? receta.getId() : null)
                .recetaNombre(receta != null ? receta.getNombre() : null)
                .receta(mapRecetaSiCargada(receta))
                .fechaCreacion(producto.getFechaCreacion())
                .fechaActualizacion(producto.getFechaActualizacion())
                .build();
    }

    private RecetaResponse mapRecetaSiCargada(Receta receta) {
        if (receta == null) {
            return null;
        }
        if (!Hibernate.isInitialized(receta.getIngredientes())) {
            return null;
        }
        return recetaMapper.toResponse(receta);
    }

    public void applyCreate(Producto producto, ProductoCreateRequest request) {
        boolean promo = Boolean.TRUE.equals(request.getEsPromocion());
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecio(request.getPrecio());
        producto.setPrecioPromocion(promo ? request.getPrecioPromocion() : null);
        producto.setEsPromocion(promo);
        producto.setDisponible(true);
        producto.setActivo(true);
        producto.setTipo(request.getTipo() != null ? request.getTipo() : TipoProducto.NORMAL);
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
        if (request.getDisponible() != null) {
            producto.setDisponible(request.getDisponible());
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
        if (request.getTipo() != null) {
            producto.setTipo(request.getTipo());
        }
    }

    private List<ProductoInsumoResponse> mapComposicion(Producto producto) {
        if (producto.getComposicion() == null || producto.getComposicion().isEmpty()) {
            return Collections.emptyList();
        }
        return producto.getComposicion().stream()
                .map(this::toInsumoResponse)
                .toList();
    }

    private ProductoInsumoResponse toInsumoResponse(ProductoInsumo pi) {
        Producto insumo = pi.getProductoInsumo();
        return ProductoInsumoResponse.builder()
                .id(pi.getId())
                .productoInsumoId(insumo != null ? insumo.getId() : null)
                .nombre(insumo != null ? insumo.getNombre() : null)
                .cantidad(pi.getCantidad())
                .unidad(pi.getUnidad())
                .esRemovible(pi.getEsRemovible())
                .esExtra(pi.getEsExtra())
                .precioExtra(pi.getPrecioExtra())
                .orden(pi.getOrden())
                .build();
    }
}
