package com.comandago.api.receta.dto.mapper;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.receta.dto.response.IngredienteResponse;
import com.comandago.api.receta.dto.response.RecetaResponse;
import com.comandago.api.receta.entity.Receta;
import com.comandago.api.receta.entity.RecetaIngrediente;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RecetaMapper {

    public RecetaResponse toResponse(Receta receta) {
        List<IngredienteResponse> ingredientes = receta.getIngredientes() == null
                ? Collections.emptyList()
                : receta.getIngredientes().stream()
                        .sorted(Comparator.comparing(ri -> ri.getOrden() != null ? ri.getOrden() : 0))
                        .map(this::toIngredienteResponse)
                        .toList();

        Map<String, List<IngredienteResponse>> porCategoria = ingredientes.stream()
                .collect(Collectors.groupingBy(
                        i -> i.categoriaNombre() != null && !i.categoriaNombre().isBlank()
                                ? i.categoriaNombre()
                                : "Sin categoría",
                        LinkedHashMap::new,
                        Collectors.toList()));

        return RecetaResponse.builder()
                .id(receta.getId())
                .nombre(receta.getNombre())
                .descripcion(receta.getDescripcion())
                .preparacion(receta.getPreparacion())
                .tiempoTotalMin(receta.getTiempoTotalMin())
                .porciones(receta.getPorciones())
                .activo(receta.getActivo())
                .ingredientes(ingredientes)
                .ingredientesPorCategoria(porCategoria)
                .totalIngredientes(ingredientes.size())
                .fechaCreacion(receta.getFechaCreacion())
                .fechaActualizacion(receta.getFechaActualizacion())
                .build();
    }

    private IngredienteResponse toIngredienteResponse(RecetaIngrediente ri) {
        Producto producto = ri.getProducto();
        Categoria categoria = producto != null ? producto.getCategoria() : null;
        return IngredienteResponse.builder()
                .id(ri.getId())
                .productoId(producto != null ? producto.getId() : null)
                .productoNombre(producto != null ? producto.getNombre() : null)
                .productoImagenUrl(producto != null ? producto.getImagenUrl() : null)
                .categoriaId(categoria != null ? categoria.getId() : null)
                .categoriaNombre(categoria != null ? categoria.getNombre() : null)
                .cantidad(ri.getCantidad())
                .unidad(ri.getUnidad())
                .esRemovible(ri.getEsRemovible())
                .orden(ri.getOrden())
                .build();
    }
}
