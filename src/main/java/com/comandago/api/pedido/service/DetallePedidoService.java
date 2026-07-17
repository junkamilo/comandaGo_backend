package com.comandago.api.pedido.service;

import com.comandago.api.pedido.dto.mapper.PedidoMapper;
import com.comandago.api.pedido.dto.request.DetalleEstadoRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoUpdateRequest;
import com.comandago.api.pedido.dto.request.ReemplazarDetalleRequest;
import com.comandago.api.pedido.dto.response.DetallePedidoResponse;
import com.comandago.api.pedido.dto.response.PedidoResponse;
import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.repository.DetallePedidoRepository;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.promocion.service.PromocionService;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.promocion.PrecioProductoResolver;
import com.comandago.api.shared.promocion.PrecioProductoResolver.ResultadoPrecioLinea;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

import static com.comandago.api.pedido.service.PedidoDetalleEdicionRules.validarCancelable;

@Service
@RequiredArgsConstructor
public class DetallePedidoService {

    private final DetallePedidoRepository detallePedidoRepository;
    private final PedidoRepository pedidoRepository;
    private final ProductoRepository productoRepository;
    private final PedidoMapper pedidoMapper;
    private final PedidoTotalesCalculator totalesCalculator;
    private final PrecioProductoResolver precioProductoResolver;
    private final PromocionService promocionService;
    private final DetalleComposicionHelper detalleComposicionHelper;
    private final EntityManager entityManager;
    private final PedidoDetalleEstadoPromoter estadoPromoter;

    @Transactional
    public DetallePedidoResponse entregar(Long pedidoId, Long detalleId) {
        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        PedidoDetalleEdicionRules.validarPedidoEditable(pedido);
        DetallePedido detalle = buscarDetalle(pedidoId, detalleId);
        DetalleEstadoTransition.validarEntrega(detalle.getEstado());
        detalle.setEstado(EstadoDetalle.ENTREGADO);
        detallePedidoRepository.save(detalle);

        Pedido pedidoActualizado = pedidoRepository.findByIdWithDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        estadoPromoter.aplicarPromocionPorEstadoDetalles(pedidoActualizado);

        return pedidoMapper.toDetalleResponse(detalle);
    }

    @Transactional
    public DetallePedidoResponse agregar(Long pedidoId, DetallePedidoItemRequest request) {
        Pedido pedido = buscarPedidoEditable(pedidoId);
        DetallePedido detalle = crearDetalleDesdeProducto(pedido, request);
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
        PedidoDetalleEdicionRules.validarPedidoEditable(detalle.getPedido());
        validarCancelable(detalle);
        if (request.getCantidad() != null) {
            detalle.setCantidad(request.getCantidad());
            ResultadoPrecioLinea precio = precioProductoResolver.resolver(
                    detalle.getProducto(), request.getCantidad(), OffsetDateTime.now());
            detalle.setPrecioUnitario(precio.precioUnitario());
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
        DetalleEstadoTransition.validarCocina(detalle.getEstado(), request.getEstado());
        detalle.setEstado(request.getEstado());
        detallePedidoRepository.save(detalle);

        Pedido pedido = pedidoRepository.findByIdWithDetalles(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        estadoPromoter.aplicarPromocionPorEstadoDetalles(pedido);

        return pedidoMapper.toDetalleResponse(detalle);
    }

    @Transactional
    public PedidoResponse reemplazar(Long pedidoId, Long detalleId, ReemplazarDetalleRequest request) {
        Pedido pedido = buscarPedidoEditable(pedidoId);
        DetallePedido detalleViejo = buscarDetalle(pedidoId, detalleId);
        validarCancelable(detalleViejo);
        detalleViejo.setEstado(EstadoDetalle.CANCELADO);

        DetallePedido detalleNuevo = crearDetalleDesdeProducto(
                pedido,
                mapearItemRequest(request));
        pedido.getDetalles().add(detalleNuevo);
        pedidoRepository.save(pedido);
        entityManager.flush();
        entityManager.refresh(pedido);
        totalesCalculator.aplicarImpuestos(pedido);
        return pedidoMapper.toResponse(pedido);
    }

    @Transactional
    public void eliminar(Long pedidoId, Long detalleId) {
        DetallePedido detalle = buscarDetalle(pedidoId, detalleId);
        PedidoDetalleEdicionRules.validarPedidoEditable(detalle.getPedido());
        validarCancelable(detalle);
        detalle.setEstado(EstadoDetalle.CANCELADO);
        detallePedidoRepository.save(detalle);
        entityManager.flush();
        entityManager.refresh(detalle.getPedido());
        totalesCalculator.aplicarImpuestos(detalle.getPedido());
    }

    private Pedido buscarPedidoEditable(Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + pedidoId));
        PedidoDetalleEdicionRules.validarPedidoEditable(pedido);
        return pedido;
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

    private DetallePedidoItemRequest mapearItemRequest(ReemplazarDetalleRequest request) {
        DetallePedidoItemRequest item = new DetallePedidoItemRequest();
        item.setProductoId(request.getNuevoProductoId());
        item.setCantidad(request.getCantidad());
        item.setNotasPreparacion(request.getNotasPreparacion());
        return item;
    }

    private DetallePedido crearDetalleDesdeProducto(Pedido pedido, DetallePedidoItemRequest request) {
        Producto producto = detalleComposicionHelper.cargarProductoParaPedido(request.getProductoId());
        ResultadoPrecioLinea precio = precioProductoResolver.resolver(
                producto, request.getCantidad(), OffsetDateTime.now());
        precio.promocionId().ifPresent(promocionService::incrementarUso);

        DetalleComposicionHelper.ResultadoLineaCompuesta linea =
                detalleComposicionHelper.resolverLinea(producto, request, precio.precioUnitario());

        return DetallePedido.builder()
                .pedido(pedido)
                .producto(producto)
                .nombreProducto(producto.getNombre())
                .cantidad(request.getCantidad())
                .precioUnitario(linea.precioUnitario())
                .notasPreparacion(linea.notasPreparacion())
                .build();
    }
}
