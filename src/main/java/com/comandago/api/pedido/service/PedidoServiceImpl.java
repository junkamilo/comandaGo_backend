package com.comandago.api.pedido.service;

import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.dto.mapper.PedidoMapper;
import com.comandago.api.pedido.dto.request.AgregarDetallesRequest;
import com.comandago.api.pedido.dto.request.CancelarDetallesRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.PedidoCreateRequest;
import com.comandago.api.pedido.dto.request.PedidoEstadoRequest;
import com.comandago.api.pedido.dto.request.PedidoUpdateRequest;
import com.comandago.api.pedido.dto.response.PedidoResponse;
import com.comandago.api.pedido.entity.DetallePedido;
import com.comandago.api.pedido.entity.Pedido;
import com.comandago.api.pedido.enums.EstadoDetalle;
import com.comandago.api.pedido.enums.EstadoPago;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.enums.OrigenPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.promocion.service.PromocionService;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.promocion.PrecioProductoResolver;
import com.comandago.api.shared.promocion.PrecioProductoResolver.ResultadoPrecioLinea;
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

import java.time.OffsetDateTime;
import java.util.List;

import static com.comandago.api.pedido.service.PedidoDetalleEdicionRules.buscarDetalle;
import static com.comandago.api.pedido.service.PedidoDetalleEdicionRules.todosDetallesCancelados;
import static com.comandago.api.pedido.service.PedidoDetalleEdicionRules.validarCancelable;
import static com.comandago.api.pedido.service.PedidoDetalleEdicionRules.validarPedidoCancelableCompleto;
import static com.comandago.api.pedido.service.PedidoDetalleEdicionRules.validarPedidoEditable;

@Service
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final PedidoNumeroGenerator numeroGenerator;
    private final PedidoMapper pedidoMapper;
    private final PedidoTotalesCalculator totalesCalculator;
    private final PedidoMesaCoordinator mesaCoordinator;
    private final PrecioProductoResolver precioProductoResolver;
    private final PromocionService promocionService;
    private final DetalleComposicionHelper detalleComposicionHelper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public PedidoResponse crear(PedidoCreateRequest request) {
        validarDomicilio(request);
        validarMeseroAutenticado(request);

        Pedido pedido = Pedido.builder()
                .numeroPedido(numeroGenerator.generar())
                .origen(request.getOrigen())
                .notas(request.getNotas())
                .clienteNombre(request.getClienteNombre())
                .clienteTelefono(request.getClienteTelefono())
                .direccionEntrega(request.getDireccionEntrega())
                .build();

        if (request.getOrigen() == OrigenPedido.MESA_MESERO) {
            pedido.setUsuario(resolverMeseroAutenticado());
        }

        if (request.getOrigen() == OrigenPedido.MESA_MESERO || request.getOrigen() == OrigenPedido.MESA_QR) {
            Mesa mesa = buscarMesaActiva(request.getMesaId());
            pedido.setMesa(mesa);
            mesaCoordinator.ocuparMesa(mesa);
        }

        for (DetallePedidoItemRequest item : request.getDetalles()) {
            pedido.getDetalles().add(crearDetalle(pedido, item));
        }

        Pedido guardado = pedidoRepository.save(pedido);
        entityManager.flush();
        entityManager.refresh(guardado);
        totalesCalculator.aplicarImpuestos(guardado);
        return pedidoMapper.toResponse(recargarConDetalles(guardado.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse obtenerPorId(Long id) {
        return pedidoMapper.toResponse(recargarConDetalles(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PedidoResponse> listar(EstadoPedido estado, Long mesaId, OrigenPedido origen,
                                               OffsetDateTime desde, OffsetDateTime hasta, Pageable pageable) {
        Page<Pedido> page;
        if (desde != null && hasta != null) {
            page = pedidoRepository.findByFechaPedidoBetween(desde, hasta, pageable);
        } else if (estado != null) {
            page = pedidoRepository.findByEstado(estado, pageable);
        } else if (mesaId != null) {
            page = pedidoRepository.findByMesaId(mesaId, pageable);
        } else if (origen != null) {
            page = pedidoRepository.findByOrigen(origen, pageable);
        } else {
            page = pedidoRepository.findAll(pageable);
        }
        return PaginationUtils.toPageResponse(page.map(pedidoMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarActivos() {
        return pedidoRepository.findByEstadoInOrderByFechaPedidoAsc(PedidoMesaCoordinator.ESTADOS_ACTIVOS)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorMesa(Long mesaId) {
        return pedidoRepository.findCuentasPendientesPorMesa(
                        mesaId, EstadoPedido.CANCELADO, EstadoPago.PAGADO)
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PedidoResponse> listarCocina() {
        return pedidoRepository.findByEstadoInOrderByFechaPedidoAsc(
                        List.of(EstadoPedido.EN_PREPARACION, EstadoPedido.POR_CONFIRMAR))
                .stream()
                .map(pedidoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PedidoResponse actualizar(Long id, PedidoUpdateRequest request) {
        validarAlMenosUnCampo(request);
        Pedido pedido = buscarPorId(id);
        if (request.getClienteNombre() != null) {
            pedido.setClienteNombre(request.getClienteNombre());
        }
        if (request.getClienteTelefono() != null) {
            pedido.setClienteTelefono(request.getClienteTelefono());
        }
        if (request.getDireccionEntrega() != null) {
            pedido.setDireccionEntrega(request.getDireccionEntrega());
        }
        if (request.getNotas() != null) {
            pedido.setNotas(request.getNotas());
        }
        return pedidoMapper.toResponse(pedidoRepository.save(pedido));
    }

    @Override
    @Transactional
    public PedidoResponse actualizarEstado(Long id, PedidoEstadoRequest request) {
        if (request.getEstado() == EstadoPedido.ENTREGADO) {
            return entregarCompleto(id);
        }
        Pedido pedido = buscarPorId(id);
        PedidoEstadoTransition.validar(pedido.getEstado(), request.getEstado());
        pedido.setEstado(request.getEstado());
        Pedido guardado = pedidoRepository.save(pedido);
        if (request.getEstado() == EstadoPedido.CANCELADO) {
            mesaCoordinator.liberarMesaSiCorresponde(guardado);
        }
        return pedidoMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public PedidoResponse entregarCompleto(Long pedidoId) {
        Pedido pedido = recargarConDetalles(pedidoId);
        validarPedidoEditable(pedido);
        if (!PedidoDetalleEstadoRules.todosActivosListosParaEntregar(pedido)) {
            throw new BusinessException(
                    "No se puede entregar el pedido completo: todos los platos activos deben estar listos");
        }
        pedido.getDetalles().stream()
                .filter(d -> d.getEstado() == EstadoDetalle.LISTO)
                .forEach(d -> d.setEstado(EstadoDetalle.ENTREGADO));
        PedidoEstadoTransition.validar(pedido.getEstado(), EstadoPedido.ENTREGADO);
        pedido.setEstado(EstadoPedido.ENTREGADO);
        Pedido guardado = pedidoRepository.save(pedido);
        return pedidoMapper.toResponse(recargarConDetalles(guardado.getId()));
    }

    @Override
    @Transactional
    public PedidoResponse cancelar(Long id) {
        Pedido pedido = recargarConDetalles(id);
        if (!PedidoEstadoTransition.puedeCancelar(pedido.getEstado())) {
            throw new BusinessException("No se puede cancelar un pedido entregado o ya cancelado");
        }
        validarPedidoCancelableCompleto(pedido);
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido.getDetalles().stream()
                .filter(d -> d.getEstado() == EstadoDetalle.PENDIENTE)
                .forEach(d -> d.setEstado(EstadoDetalle.CANCELADO));
        Pedido guardado = pedidoRepository.save(pedido);
        entityManager.flush();
        entityManager.refresh(guardado);
        totalesCalculator.aplicarImpuestos(guardado);
        mesaCoordinator.liberarMesaSiCorresponde(guardado);
        return pedidoMapper.toResponse(recargarConDetalles(guardado.getId()));
    }

    @Override
    @Transactional
    public PedidoResponse cancelarDetalles(Long pedidoId, CancelarDetallesRequest request) {
        Pedido pedido = recargarConDetalles(pedidoId);
        validarPedidoEditable(pedido);

        for (Long detalleId : request.getDetalleIds()) {
            DetallePedido detalle = buscarDetalle(pedido, detalleId);
            validarCancelable(detalle);
            detalle.setEstado(EstadoDetalle.CANCELADO);
        }

        if (todosDetallesCancelados(pedido)) {
            pedido.setEstado(EstadoPedido.CANCELADO);
            mesaCoordinator.liberarMesaSiCorresponde(pedido);
        }

        Pedido guardado = pedidoRepository.save(pedido);
        entityManager.flush();
        entityManager.refresh(guardado);
        totalesCalculator.aplicarImpuestos(guardado);
        return pedidoMapper.toResponse(recargarConDetalles(guardado.getId()));
    }

    @Override
    @Transactional
    public PedidoResponse agregarDetalles(Long pedidoId, AgregarDetallesRequest request) {
        Pedido pedido = recargarConDetalles(pedidoId);
        validarPedidoEditable(pedido);

        for (DetallePedidoItemRequest detalleRequest : request.getDetalles()) {
            pedido.getDetalles().add(crearDetalle(pedido, detalleRequest));
        }

        Pedido guardado = pedidoRepository.save(pedido);
        entityManager.flush();
        entityManager.refresh(guardado);
        totalesCalculator.aplicarImpuestos(guardado);
        return pedidoMapper.toResponse(recargarConDetalles(guardado.getId()));
    }

    private DetallePedido crearDetalle(Pedido pedido, DetallePedidoItemRequest item) {
        Producto producto = detalleComposicionHelper.cargarProductoParaPedido(item.getProductoId());
        ResultadoPrecioLinea precio = precioProductoResolver.resolver(
                producto, item.getCantidad(), OffsetDateTime.now());
        precio.promocionId().ifPresent(promocionService::incrementarUso);

        DetalleComposicionHelper.ResultadoLineaCompuesta linea =
                detalleComposicionHelper.resolverLinea(producto, item, precio.precioUnitario());

        return DetallePedido.builder()
                .pedido(pedido)
                .producto(producto)
                .nombreProducto(producto.getNombre())
                .cantidad(item.getCantidad())
                .precioUnitario(linea.precioUnitario())
                .notasPreparacion(linea.notasPreparacion())
                .build();
    }

    private Pedido recargarConDetalles(Long id) {
        return pedidoRepository.findByIdWithDetalles(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));
    }

    private Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));
    }

    private Mesa buscarMesaActiva(Long id) {
        Mesa mesa = mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada con id: " + id));
        if (!Boolean.TRUE.equals(mesa.getActivo())) {
            throw new BusinessException("La mesa no está activa");
        }
        return mesa;
    }

    private Usuario resolverMeseroAutenticado() {
        UsuarioPrincipal principal = SecurityUtils.currentUserOrNull();
        if (principal == null) {
            throw new BusinessException("Se requiere autenticación para pedidos del mesero");
        }
        return usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + principal.getId()));
    }

    private void validarMeseroAutenticado(PedidoCreateRequest request) {
        if (request.getOrigen() == OrigenPedido.MESA_MESERO && SecurityUtils.currentUserOrNull() == null) {
            throw new BusinessException("Se requiere autenticación para pedidos del mesero");
        }
    }

    private void validarDomicilio(PedidoCreateRequest request) {
        if (request.getOrigen() == OrigenPedido.WEB_DOMICILIO) {
            if (request.getClienteNombre() == null || request.getClienteNombre().isBlank()) {
                throw new BusinessException("El nombre del cliente es obligatorio para domicilio");
            }
            if (request.getClienteTelefono() == null || request.getClienteTelefono().isBlank()) {
                throw new BusinessException("El teléfono del cliente es obligatorio para domicilio");
            }
        }
    }

    private void validarAlMenosUnCampo(PedidoUpdateRequest request) {
        if (request.getClienteNombre() == null && request.getClienteTelefono() == null
                && request.getDireccionEntrega() == null && request.getNotas() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
