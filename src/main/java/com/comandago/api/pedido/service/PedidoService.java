package com.comandago.api.pedido.service;

import com.comandago.api.pedido.dto.request.PedidoCreateRequest;
import com.comandago.api.pedido.dto.request.PedidoEstadoRequest;
import com.comandago.api.pedido.dto.request.PedidoUpdateRequest;
import com.comandago.api.pedido.dto.response.PedidoResponse;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.enums.OrigenPedido;
import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

public interface PedidoService {

    PedidoResponse crear(PedidoCreateRequest request);

    PedidoResponse obtenerPorId(Long id);

    PageResponse<PedidoResponse> listar(EstadoPedido estado, Long mesaId, OrigenPedido origen,
                                        OffsetDateTime desde, OffsetDateTime hasta, Pageable pageable);

    List<PedidoResponse> listarCocina();

    List<PedidoResponse> listarActivos();

    List<PedidoResponse> listarPorMesa(Long mesaId);

    PedidoResponse actualizar(Long id, PedidoUpdateRequest request);

    PedidoResponse actualizarEstado(Long id, PedidoEstadoRequest request);

    PedidoResponse cancelar(Long id);
}
