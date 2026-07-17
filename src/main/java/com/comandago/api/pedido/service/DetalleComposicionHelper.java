package com.comandago.api.pedido.service;

import com.comandago.api.pedido.dto.request.CambioInsumoRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.entity.ProductoInsumo;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.receta.entity.RecetaIngrediente;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DetalleComposicionHelper {

    private final ProductoRepository productoRepository;

    public Producto cargarProductoParaPedido(Long productoId) {
        Producto producto = productoRepository.findByIdWithComposicion(productoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Producto no encontrado con id: " + productoId));
        if (!Boolean.TRUE.equals(producto.getActivo()) || !Boolean.TRUE.equals(producto.getDisponible())) {
            throw new BusinessException("El producto " + producto.getNombre() + " no está disponible");
        }
        if (producto.getTipo() == TipoProducto.INSUMO && producto.getCategoria() == null) {
            throw new BusinessException(
                    "Los insumos sin categoría son de uso interno y no se pueden vender en un pedido");
        }
        return producto;
    }

    public ResultadoLineaCompuesta resolverLinea(Producto producto, DetallePedidoItemRequest item, BigDecimal precioBase) {
        if (producto.getTipo() != TipoProducto.COMPUESTO) {
            return new ResultadoLineaCompuesta(precioBase, item.getNotasPreparacion());
        }

        if (producto.getReceta() != null
                && producto.getReceta().getIngredientes() != null
                && !producto.getReceta().getIngredientes().isEmpty()) {
            return resolverLineaReceta(producto, item, precioBase);
        }

        return resolverLineaComposicionLegacy(producto, item, precioBase);
    }

    private ResultadoLineaCompuesta resolverLineaReceta(
            Producto producto, DetallePedidoItemRequest item, BigDecimal precioBase) {
        Map<Long, RecetaIngrediente> porProductoId = producto.getReceta().getIngredientes().stream()
                .collect(Collectors.toMap(ri -> ri.getProducto().getId(), ri -> ri, (a, b) -> a));

        Set<Long> extrasIds = item.getExtrasIds() != null ? new HashSet<>(item.getExtrasIds()) : Set.of();
        if (!extrasIds.isEmpty()) {
            throw new BusinessException("Este producto no admite extras; personalízalo con removibles o cambios");
        }

        Set<Long> removidosIds = item.getRemovidosIds() != null
                ? new HashSet<>(item.getRemovidosIds())
                : Set.of();

        List<String> sinNombres = new ArrayList<>();
        for (Long removidoId : removidosIds) {
            RecetaIngrediente ri = porProductoId.get(removidoId);
            if (ri == null || !Boolean.TRUE.equals(ri.getEsRemovible())) {
                throw new BusinessException("El insumo removido no es válido para este producto");
            }
            sinNombres.add(ri.getProducto().getNombre());
        }

        List<CambioInsumoRequest> cambios = item.getCambios() != null ? item.getCambios() : List.of();
        Set<Long> desdeVistos = new HashSet<>();
        List<String> cambioNombres = new ArrayList<>();

        for (CambioInsumoRequest cambio : cambios) {
            if (cambio.getDesdeProductoId() == null || cambio.getHaciaProductoId() == null) {
                throw new BusinessException("Cada cambio debe indicar producto de origen y destino");
            }
            if (!desdeVistos.add(cambio.getDesdeProductoId())) {
                throw new BusinessException("No se puede cambiar el mismo insumo más de una vez");
            }
            if (removidosIds.contains(cambio.getDesdeProductoId())) {
                throw new BusinessException("No se puede remover y cambiar el mismo insumo a la vez");
            }
            if (Objects.equals(cambio.getDesdeProductoId(), cambio.getHaciaProductoId())) {
                throw new BusinessException("El cambio debe ser hacia un insumo distinto");
            }

            RecetaIngrediente origen = porProductoId.get(cambio.getDesdeProductoId());
            if (origen == null || !Boolean.TRUE.equals(origen.getEsRemovible())) {
                throw new BusinessException("El insumo a cambiar no es válido o no es removible");
            }
            if (origen.getProducto().getCategoria() == null) {
                throw new BusinessException(
                        "El insumo '" + origen.getProducto().getNombre() + "' no tiene categoría para cambiar");
            }

            Producto destino = productoRepository.findById(cambio.getHaciaProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + cambio.getHaciaProductoId()));

            if (destino.getTipo() != TipoProducto.INSUMO) {
                throw new BusinessException("Solo se puede cambiar por un insumo");
            }
            if (!Boolean.TRUE.equals(destino.getActivo()) || !Boolean.TRUE.equals(destino.getDisponible())) {
                throw new BusinessException("El insumo '" + destino.getNombre() + "' no está disponible hoy");
            }
            if (destino.getCategoria() == null
                    || !Objects.equals(destino.getCategoria().getId(), origen.getProducto().getCategoria().getId())) {
                throw new BusinessException(
                        "El insumo '" + destino.getNombre() + "' no pertenece a la misma categoría");
            }

            cambioNombres.add(origen.getProducto().getNombre() + " → " + destino.getNombre());
        }

        BigDecimal esperado = precioBase.setScale(2, RoundingMode.HALF_UP);
        validarPrecioUnitario(item, esperado);

        String notas = construirNotas(sinNombres, List.of(), cambioNombres, item.getNotasPreparacion());
        return new ResultadoLineaCompuesta(esperado, notas);
    }

    private ResultadoLineaCompuesta resolverLineaComposicionLegacy(
            Producto producto, DetallePedidoItemRequest item, BigDecimal precioBase) {
        List<ProductoInsumo> composicion = producto.getComposicion() != null
                ? producto.getComposicion()
                : List.of();
        Map<Long, ProductoInsumo> porInsumoId = composicion.stream()
                .collect(Collectors.toMap(pi -> pi.getProductoInsumo().getId(), pi -> pi, (a, b) -> a));

        Set<Long> extrasIds = item.getExtrasIds() != null
                ? new HashSet<>(item.getExtrasIds())
                : Set.of();
        Set<Long> removidosIds = item.getRemovidosIds() != null
                ? new HashSet<>(item.getRemovidosIds())
                : Set.of();

        if (item.getCambios() != null && !item.getCambios().isEmpty()) {
            throw new BusinessException("Este producto legacy no admite cambios de insumos");
        }

        BigDecimal extrasTotal = BigDecimal.ZERO;
        List<String> extrasNombres = new ArrayList<>();
        for (Long extraId : extrasIds) {
            ProductoInsumo pi = porInsumoId.get(extraId);
            if (pi == null || !Boolean.TRUE.equals(pi.getEsExtra())) {
                throw new BusinessException("El extra seleccionado no pertenece a la receta del producto");
            }
            if (pi.getPrecioExtra() == null) {
                throw new BusinessException("El extra '" + pi.getProductoInsumo().getNombre() + "' no tiene precio");
            }
            extrasTotal = extrasTotal.add(pi.getPrecioExtra());
            extrasNombres.add(pi.getProductoInsumo().getNombre() + " (+$"
                    + pi.getPrecioExtra().setScale(0, RoundingMode.HALF_UP).toPlainString() + ")");
        }

        List<String> sinNombres = new ArrayList<>();
        for (Long removidoId : removidosIds) {
            ProductoInsumo pi = porInsumoId.get(removidoId);
            if (pi == null || !Boolean.TRUE.equals(pi.getEsRemovible()) || Boolean.TRUE.equals(pi.getEsExtra())) {
                throw new BusinessException("El insumo removido no es válido para este producto");
            }
            sinNombres.add(pi.getProductoInsumo().getNombre());
        }

        BigDecimal esperado = precioBase.add(extrasTotal).setScale(2, RoundingMode.HALF_UP);
        validarPrecioUnitario(item, esperado);

        String notas = construirNotas(sinNombres, extrasNombres, List.of(), item.getNotasPreparacion());
        return new ResultadoLineaCompuesta(esperado, notas);
    }

    private void validarPrecioUnitario(DetallePedidoItemRequest item, BigDecimal esperado) {
        if (item.getPrecioUnitario() != null) {
            BigDecimal enviado = item.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP);
            if (enviado.compareTo(esperado) != 0) {
                throw new BusinessException(
                        "El precio unitario no coincide con la base más extras. Esperado: $" + esperado.toPlainString());
            }
        }
    }

    private String construirNotas(
            List<String> sin, List<String> extras, List<String> cambios, String notasCliente) {
        List<String> partes = new ArrayList<>();
        if (!cambios.isEmpty()) {
            partes.add("CAMBIO: " + String.join(", ", cambios));
        }
        if (!sin.isEmpty()) {
            partes.add("SIN: " + String.join(", ", sin));
        }
        if (!extras.isEmpty()) {
            partes.add("EXTRA: " + String.join(", ", extras));
        }
        String generado = String.join(" | ", partes);
        if (generado.isBlank()) {
            return notasCliente;
        }
        if (notasCliente == null || notasCliente.isBlank()) {
            return generado;
        }
        if (Objects.equals(notasCliente.trim(), generado)) {
            return generado;
        }
        return generado + " | " + notasCliente.trim();
    }

    public record ResultadoLineaCompuesta(BigDecimal precioUnitario, String notasPreparacion) {
    }
}
