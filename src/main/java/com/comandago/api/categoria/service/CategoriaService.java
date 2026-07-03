package com.comandago.api.categoria.service;

import com.comandago.api.categoria.dto.request.CategoriaCreateRequest;
import com.comandago.api.categoria.dto.request.CategoriaUpdateRequest;
import com.comandago.api.categoria.dto.response.CategoriaResponse;
import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface CategoriaService {

    CategoriaResponse crear(CategoriaCreateRequest request);

    CategoriaResponse obtenerPorId(Long id);

    PageResponse<CategoriaResponse> listar(Boolean activo, String nombre, Pageable pageable);

    CategoriaResponse actualizar(Long id, CategoriaUpdateRequest request);

    CategoriaResponse actualizarActivo(Long id, Boolean activo);

    void eliminar(Long id);
}
