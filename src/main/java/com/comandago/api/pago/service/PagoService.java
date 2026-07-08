package com.comandago.api.pago.service;

import com.comandago.api.pago.dto.request.RegistrarPagoRequest;
import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.pago.dto.response.ResumenPagoPedidoResponse;
import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PagoService {

    ResumenPagoPedidoResponse resumenPorPedido(Long pedidoId);

    PagoResponse registrar(RegistrarPagoRequest request);

    PagoResponse confirmar(Long pagoId);

    PagoResponse rechazar(Long pagoId);

    PagoResponse reembolsar(Long pagoId, String notas);

    PagoResponse obtenerPorId(Long id);

    PageResponse<PagoResponse> listar(Long pedidoId, Pageable pageable);

    List<PagoResponse> listarPorPedido(Long pedidoId);
}
