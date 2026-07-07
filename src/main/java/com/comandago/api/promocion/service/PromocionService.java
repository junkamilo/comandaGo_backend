package com.comandago.api.promocion.service;

import com.comandago.api.promocion.dto.request.PromocionRequest;
import com.comandago.api.promocion.dto.response.PromocionResponse;

import java.util.List;
import java.util.Optional;

public interface PromocionService {

    List<PromocionResponse> listar();

    List<PromocionResponse> vigentes();

    PromocionResponse obtener(Long id);

    PromocionResponse crear(PromocionRequest request);

    PromocionResponse actualizar(Long id, PromocionRequest request);

    void desactivar(Long id);

    void incrementarUso(Long promoId);

    Optional<PromocionResponse> findVigenteParaProducto(Long productoId);
}
