package com.comandago.api.shared.promocion;

import com.comandago.api.producto.entity.Producto;
import com.comandago.api.promocion.entity.Promocion;
import com.comandago.api.promocion.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PrecioProductoResolver {

    private final PromocionRepository promocionRepository;

    public ResultadoPrecioLinea resolver(Producto producto, int cantidad, OffsetDateTime ahora) {
        Optional<Promocion> promoOpt = promocionRepository.findVigenteParaProducto(producto.getId(), ahora);

        if (promoOpt.isPresent()) {
            Promocion promo = promoOpt.get();
            BigDecimal precioUnitario = CalculadorPromocion.precioUnitarioEfectivo(
                    producto.getPrecio(), cantidad, promo);
            return new ResultadoPrecioLinea(precioUnitario, Optional.of(promo.getId()));
        }

        return new ResultadoPrecioLinea(producto.getPrecioFinal(), Optional.empty());
    }

    public record ResultadoPrecioLinea(
            BigDecimal precioUnitario,
            Optional<Long> promocionId
    ) {
    }
}
