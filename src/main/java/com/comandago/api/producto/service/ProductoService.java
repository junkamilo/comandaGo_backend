package com.comandago.api.producto.service;

import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductoService {

    ProductoResponse crear(ProductoCreateRequest request);

    ProductoResponse obtenerPorId(Long id);

    PageResponse<ProductoResponse> listar(Long categoriaId, Boolean activo, Boolean disponible,
                                          Boolean esPromocion, Pageable pageable);

    List<ProductoResponse> listarMenu();

    ProductoResponse actualizar(Long id, ProductoUpdateRequest request);

    ProductoResponse actualizarDisponibilidad(Long id, Boolean disponible);

    void eliminar(Long id);
}
