package com.comandago.api.pedido.service;

import com.comandago.api.pedido.dto.mapper.PedidoMapper;
import com.comandago.api.pedido.dto.request.DetalleEstadoRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoUpdateRequest;
import com.comandago.api.pedido.dto.response.DetallePedidoResponse;
import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.DetallePedidoRepository;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DetallePedidoService {

    private final DetallePedidoRepository detallePedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoMapper pedidoMapper;
    private final PedidoTotalesCalculator totalesCalculator;
    private final EntityManager entityManager;

    @Transactional
    public DetallePedidoResponse agregar(Long pedidoId, DetallePedidoItemRequest request) {
        Pedido pedido = buscarPedidoEditable(pedidoId);
        Producto producto = buscarProductoDisponible(request.getProductoId());
        DetallePedido detalle = DetallePedido.builder()
                .pedido(pedido)
                .producto(producto)
                .nombreProducto(producto.getNombre())
                .cantidad(request.getCantidad())
                .precioUnitario(producto.getPrecioFinal())
                .notasPreparacion(request.getNotasPreparacion())
                .build();
        pedido.getDetalles().add(detalle);
        pedidoRepository.save(pedido);
        entityManager.flush();
        entityManager.refresh(pedido);
        totalesCalculator.aplicarImpuestos(pedido);
        return pedidoMapper.toDetalleResponse(detalle);
    }

    @Transactional(readOnly = true)
    public List<DetallePedidoResponse> listar(Long pedidoId) {
        validarPedidoExiste(pedidoId);
        return detallePedidoRepository.findByPedidoIdWithProducto(pedidoId).stream()
                .map(pedidoMapper::toDetalleResponse)
                .toList();
    }

    @Transactional
    public DetallePedidoResponse actualizar(Long pedidoId, Long detalleId, DetallePedidoUpdateRequest request) {
        validarAlMenosUnCampo(request);
        DetallePedido detalle = buscarDetalle(pedidoId, detalleId);
        validarPedidoEditable(detalle.getPedido());
        if (request.getCantidad() != null) {
            detalle.setCantidad(request.getCantidad());
        }
        if (request.getNotasPreparacion() != null) {
            detalle.setNotasPreparacion(request.getNotasPreparacion());
        }
        DetallePedido guardado = detallePedidoRepository.save(detalle);
        entityManager.flush();
        entityManager.refresh(detalle.getPedido());
        totalesCalculator.aplicarImpuestos(detalle.getPedido());
        return pedidoMapper.toDetalleResponse(guardado);
    }

    @Transactional
    public DetallePedidoResponse actualizarEstado(Long pedidoId, Long detalleId, DetalleEstadoRequest request) {
        DetallePedido detalle = buscarDetalle(pedidoId, detalleId);
        detalle.setEstado(request.getEstado());
        detallePedidoRepository.save(detalle);

        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        if (PedidoDetalleEstadoRules.debePromoverPedidoAListo(pedido)) {
            pedido.setEstado(EstadoPedido.LISTO);
            pedidoRepository.save(pedido);
        }

        return pedidoMapper.toDetalleResponse(detalle);
    }

    @Transactional
    public void eliminar(Long pedidoId, Long detalleId) {
        DetallePedido detalle = buscarDetalle(pedidoId, detalleId);
        validarPedidoEditable(detalle.getPedido());
        detalle.getPedido().getDetalles().remove(detalle);
        detallePedidoRepository.delete(detalle);
        entityManager.flush();
        entityManager.refresh(detalle.getPedido());
        totalesCalculator.aplicarImpuestos(detalle.getPedido());
    }

    private Pedido buscarPedidoEditable(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        validarPedidoEditable(pedido);
        return pedido;
    }

    private void validarPedidoEditable(Pedido pedido) {
        if (pedido.getEstado() != EstadoPedido.POR_CONFIRMAR) {
            throw new BusinessException("Solo se pueden modificar detalles de pedidos en estado POR_CONFIRMAR");
        }
    }

    private void validarPedidoExiste(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId);
        }
    }

    private DetallePedido buscarDetalle(Long pedidoId, Long detalleId) {
        return detallePedidoRepository.findByIdAndPedidoId(detalleId, pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Detalle no encontrado"));
    }

    private Producto buscarProductoDisponible(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        if (!Boolean.TRUE.equals(producto.getActivo()) || !Boolean.TRUE.equals(producto.getDisponible())) {
            throw new BusinessException("El producto no está disponible");
        }
        return producto;
    }

    private void validarAlMenosUnCampo(DetallePedidoUpdateRequest request) {
        if (request.getCantidad() == null && request.getNotasPreparacion() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
