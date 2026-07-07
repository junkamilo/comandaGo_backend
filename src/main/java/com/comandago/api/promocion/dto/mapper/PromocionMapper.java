package com.comandago.api.promocion.dto.mapper;

import com.comandago.api.producto.entity.Producto;
import com.comandago.api.promocion.dto.response.ProductoPromoResponse;
import com.comandago.api.promocion.dto.response.PromocionResponse;
import com.comandago.api.promocion.entity.Promocion;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class PromocionMapper {

    public PromocionResponse toResponse(Promocion promocion) {
        return toResponse(promocion, OffsetDateTime.now());
    }

    public PromocionResponse toResponse(Promocion promocion, OffsetDateTime ahora) {
        boolean vigente = esVigente(promocion, ahora);
        List<ProductoPromoResponse> productos = promocion.getProductos().stream()
                .map(this::toProductoPromoResponse)
                .toList();

        return new PromocionResponse(
                promocion.getId(),
                promocion.getNombre(),
                promocion.getDescripcion(),
                promocion.getTipo(),
                promocion.getValorPorcentaje(),
                promocion.getValorMonto(),
                promocion.getPagaCantidad(),
                promocion.getLlevaCantidad(),
                promocion.getFechaInicio(),
                promocion.getFechaFin(),
                promocion.getUsoMaximo(),
                promocion.getUsoActual(),
                promocion.getActivo(),
                vigente,
                productos,
                promocion.getFechaCreacion()
        );
    }

    private ProductoPromoResponse toProductoPromoResponse(Producto producto) {
        return new ProductoPromoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getImagenUrl()
        );
    }

    private boolean esVigente(Promocion promocion, OffsetDateTime ahora) {
        return Boolean.TRUE.equals(promocion.getActivo())
                && !ahora.isBefore(promocion.getFechaInicio())
                && (promocion.getFechaFin() == null || ahora.isBefore(promocion.getFechaFin()))
                && (promocion.getUsoMaximo() == null || promocion.getUsoActual() < promocion.getUsoMaximo());
    }
}
