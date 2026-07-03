package com.comandago.api.pago.service;

import com.comandago.api.pago.dto.request.PagoCreateRequest;
import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PagoService {

    PagoResponse crear(PagoCreateRequest request);

    PagoResponse obtenerPorId(Long id);

    PageResponse<PagoResponse> listar(Long pedidoId, Pageable pageable);

    List<PagoResponse> listarPorPedido(Long pedidoId);
}
