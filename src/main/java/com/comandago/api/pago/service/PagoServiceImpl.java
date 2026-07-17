package com.comandago.api.pago.service;

import com.comandago.api.pago.dto.mapper.PagoMapper;
import com.comandago.api.pago.dto.request.RegistrarPagoRequest;
import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.pago.dto.response.ResumenPagoPedidoResponse;
import com.comandago.api.pago.entity.Pago;
import com.comandago.api.pago.enums.EstadoTransaccionPago;
import com.comandago.api.pago.enums.MetodoPago;
import com.comandago.api.pago.repository.PagoRepository;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.pedido.service.PedidoMesaCoordinator;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.response.PageResponse;
import com.comandago.api.shared.security.SecurityUtils;
import com.comandago.api.shared.security.UsuarioPrincipal;
import com.comandago.api.shared.util.PaginationUtils;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.comandago.api.pago.service.PagoReglas.validarEfectivo;
import static com.comandago.api.pago.service.PagoReglas.validarPedidoCobrable;
import static com.comandago.api.pago.service.PagoReglas.validarSobrepago;

@Service
@RequiredArgsConstructor
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PagoMapper pagoMapper;
    private final PedidoMesaCoordinator mesaCoordinator;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public ResumenPagoPedidoResponse resumenPorPedido(Long pedidoId) {
        Pedido pedido = buscarPedido(pedidoId);
        return construirResumen(pedido);
    }

    @Override
    @Transactional
    public PagoResponse registrar(RegistrarPagoRequest request) {
        Pedido pedido = buscarPedido(request.getPedidoId());
        validarPedidoCobrable(pedido);
        validarEfectivo(request);

        BigDecimal totalPagado = pagoRepository.totalPagadoPorPedido(pedido.getId());
        validarSobrepago(pedido, totalPagado, request.getMonto());

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setUsuario(resolverCajero());
        pago.setMetodo(request.getMetodo());
        pago.setMonto(request.getMonto());
        pago.setPropina(request.getPropina() != null ? request.getPropina() : BigDecimal.ZERO);
        pago.setMontoRecibido(request.getMontoRecibido());
        pago.setReferencia(request.getReferencia());
        pago.setProveedorId(request.getProveedorId());
        pago.setNotas(request.getNotas());

        if (request.getMetodo() == MetodoPago.NEQUI || request.getMetodo() == MetodoPago.DAVIPLATA) {
            pago.setEstado(EstadoTransaccionPago.PENDIENTE);
        } else {
            pago.setEstado(EstadoTransaccionPago.COMPLETADO);
        }

        Pago guardado = pagoRepository.save(pago);
        actualizarEstadoPagoPedido(pedido);
        entityManager.flush();
        entityManager.refresh(guardado);
        return pagoMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public PagoResponse confirmar(Long pagoId) {
        Pago pago = buscarPago(pagoId);
        if (pago.getEstado() != EstadoTransaccionPago.PENDIENTE) {
            throw new BusinessException("Solo se pueden confirmar pagos pendientes");
        }
        pago.setEstado(EstadoTransaccionPago.COMPLETADO);
        pagoRepository.save(pago);
        actualizarEstadoPagoPedido(pago.getPedido());
        return pagoMapper.toResponse(pago);
    }

    @Override
    @Transactional
    public PagoResponse rechazar(Long pagoId) {
        Pago pago = buscarPago(pagoId);
        if (pago.getEstado() != EstadoTransaccionPago.PENDIENTE) {
            throw new BusinessException("Solo se pueden rechazar pagos pendientes");
        }
        pago.setEstado(EstadoTransaccionPago.RECHAZADO);
        pagoRepository.save(pago);
        actualizarEstadoPagoPedido(pago.getPedido());
        return pagoMapper.toResponse(pago);
    }

    @Override
    @Transactional
    public PagoResponse reembolsar(Long pagoId, String notas) {
        Pago pago = buscarPago(pagoId);
        if (pago.getEstado() != EstadoTransaccionPago.COMPLETADO) {
            throw new BusinessException("Solo se pueden reembolsar pagos completados");
        }
        pago.setEstado(EstadoTransaccionPago.REEMBOLSADO);
        if (notas != null) {
            pago.setNotas(notas);
        }
        pagoRepository.save(pago);
        actualizarEstadoPagoPedido(pago.getPedido());
        return pagoMapper.toResponse(pago);
    }

    @Override
    @Transactional(readOnly = true)
    public PagoResponse obtenerPorId(Long id) {
        return pagoMapper.toResponse(buscarPago(id));
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

    private void actualizarEstadoPagoPedido(Pedido pedido) {
        BigDecimal totalPagado = pagoRepository.totalPagadoPorPedido(pedido.getId());
        EstadoPago nuevoEstado;
        if (totalPagado.compareTo(BigDecimal.ZERO) == 0) {
            nuevoEstado = EstadoPago.PENDIENTE;
        } else if (totalPagado.compareTo(pedido.getTotal()) >= 0) {
            nuevoEstado = EstadoPago.PAGADO;
        } else {
            nuevoEstado = EstadoPago.PARCIAL;
        }
        pedido.setEstadoPago(nuevoEstado);
        pedidoRepository.save(pedido);
        if (nuevoEstado == EstadoPago.PAGADO) {
            mesaCoordinator.liberarMesaSiCorresponde(pedido);
        }
    }

    private ResumenPagoPedidoResponse construirResumen(Pedido pedido) {
        BigDecimal totalPagado = pagoRepository.totalPagadoPorPedido(pedido.getId());
        BigDecimal totalPropinas = pagoRepository.totalPropinasPorPedido(pedido.getId());
        BigDecimal saldo = pedido.getTotal().subtract(totalPagado).max(BigDecimal.ZERO);
        List<PagoResponse> pagos = pagoRepository.findByPedidoIdOrderByFechaPagoAsc(pedido.getId())
                .stream()
                .map(pagoMapper::toResponse)
                .toList();
        return new ResumenPagoPedidoResponse(
                pedido.getId(),
                pedido.getNumeroPedido(),
                pedido.getTotal(),
                totalPagado,
                totalPropinas,
                saldo,
                pedido.getEstadoPago().name(),
                pagos
        );
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));
    }

    private Pago buscarPago(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con id: " + id));
    }

    private Usuario resolverCajero() {
        UsuarioPrincipal principal = SecurityUtils.currentUserOrNull();
        if (principal == null) {
            return null;
        }
        return usuarioRepository.findById(principal.getId()).orElse(null);
    }
}
