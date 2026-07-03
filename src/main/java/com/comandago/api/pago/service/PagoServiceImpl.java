package com.comandago.api.pago.service;

import com.comandago.api.pago.dto.mapper.PagoMapper;
import com.comandago.api.pago.dto.request.PagoCreateRequest;
import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.pago.entity.Pago;
import com.comandago.api.pago.repository.PagoRepository;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.response.PageResponse;
import com.comandago.api.shared.util.PaginationUtils;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoMapper pagoMapper;

    @Override
    @Transactional
    public PagoResponse crear(PagoCreateRequest request) {
        Pedido pedido = pedidoRepository.findById(request.getPedidoId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido no encontrado con id: " + request.getPedidoId()));
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new BusinessException("No se puede registrar pago en un pedido cancelado");
        }

        BigDecimal pagado = pagoRepository.sumMontoByPedidoId(pedido.getId());
        BigDecimal saldo = pedido.getTotal().subtract(pagado);
        if (request.getMonto().compareTo(saldo) > 0) {
            throw new BusinessException("El monto excede el saldo pendiente del pedido");
        }

        Pago pago = Pago.builder()
                .pedido(pedido)
                .metodo(request.getMetodo())
                .monto(request.getMonto())
                .referencia(request.getReferencia())
                .build();

        if (request.getUsuarioId() != null) {
            Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Usuario no encontrado con id: " + request.getUsuarioId()));
            pago.setUsuario(usuario);
        }

        Pago guardado = pagoRepository.save(pago);
        actualizarEstadoPago(pedido);
        return pagoMapper.toResponse(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtenerPorId(Long id) {
        return pagoMapper.toResponse(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PagoResponse> listar(Long pedidoId, Pageable pageable) {
        Page<Pago> page = pedidoId != null
                ? pagoRepository.findByPedidoId(pedidoId, pageable)
                : pagoRepository.findAll(pageable);
        return PaginationUtils.toPageResponse(page.map(pagoMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> listarPorPedido(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId);
        }
        return pagoRepository.findByPedidoIdOrderByFechaPagoAsc(pedidoId).stream()
                .map(pagoMapper::toResponse)
                .toList();
    }

    private void actualizarEstadoPago(Pedido pedido) {
        BigDecimal pagado = pagoRepository.sumMontoByPedidoId(pedido.getId());
        if (pagado.compareTo(BigDecimal.ZERO) == 0) {
            pedido.setEstadoPago(EstadoPago.PENDIENTE);
        } else if (pagado.compareTo(pedido.getTotal()) < 0) {
            pedido.setEstadoPago(EstadoPago.PARCIAL);
        } else {
            pedido.setEstadoPago(EstadoPago.PAGADO);
        }
        pedidoRepository.save(pedido);
    }

    private Pago buscarPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con id: " + id));
    }
}
