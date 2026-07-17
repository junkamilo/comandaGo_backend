package com.comandago.api.receta.service;

import com.comandago.api.receta.dto.request.RecetaRequest;
import com.comandago.api.receta.dto.response.RecetaResponse;

import java.util.List;

public interface RecetaService {

    List<RecetaResponse> listar();

    List<RecetaResponse> listarActivas();

    RecetaResponse obtener(Long id);

    RecetaResponse crear(RecetaRequest request);

    RecetaResponse actualizar(Long id, RecetaRequest request);

    void desactivar(Long id);
}
