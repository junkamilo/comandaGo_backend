package com.comandago.api.mesa.service;

import com.comandago.api.mesa.dto.mapper.MesaMapper;
import com.comandago.api.mesa.dto.request.MesaCreateRequest;
import com.comandago.api.mesa.dto.request.MesaUpdateRequest;
import com.comandago.api.mesa.dto.response.MesaResponse;
import com.comandago.api.mesa.entity.Mesa;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.repository.MesaRepository;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.repository.PedidoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ConflictException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.response.PageResponse;
import com.comandago.api.shared.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MesaServiceImpl implements MesaService {

    private static final List<EstadoPedido> ESTADOS_PEDIDO_ACTIVOS = List.of(
            EstadoPedido.POR_CONFIRMAR,
            EstadoPedido.EN_PREPARACION,
            EstadoPedido.LISTO,
            EstadoPedido.EN_CAMINO
    );

    private final MesaRepository mesaRepository;
    private final PedidoRepository pedidoRepository;
    private final MesaMapper mesaMapper;

    @Override
    @Transactional
    public MesaResponse crear(MesaCreateRequest request) {
        if (mesaRepository.existsByNumeroIgnoreCase(request.getNumero())) {
            throw new ConflictException("Ya existe una mesa con ese número");
        }
        String qrToken = request.getQrToken() != null ? request.getQrToken() : UUID.randomUUID().toString();
        if (mesaRepository.existsByQrToken(qrToken)) {
            throw new ConflictException("El token QR ya está en uso");
        }
        Mesa mesa = mesaMapper.toEntity(request, qrToken);
        return mesaMapper.toResponse(mesaRepository.save(mesa));
    }

    @Override
    @Transactional(readOnly = true)
    public MesaResponse obtenerPorId(Long id) {
        return mesaMapper.toResponse(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public MesaResponse obtenerPorQrToken(String token) {
        Mesa mesa = mesaRepository.findByQrToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada para el token QR"));
        return mesaMapper.toResponse(mesa);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MesaResponse> listar(EstadoMesa estado, Boolean activo, Pageable pageable) {
        Page<Mesa> page;
        if (estado != null && activo != null) {
            page = mesaRepository.findByEstadoAndActivo(estado, activo, pageable);
        } else if (estado != null) {
            page = mesaRepository.findByEstado(estado, pageable);
        } else if (activo != null) {
            page = mesaRepository.findByActivo(activo, pageable);
        } else {
            page = mesaRepository.findAll(pageable);
        }
        return PaginationUtils.toPageResponse(page.map(mesaMapper::toResponse));
    }

    @Override
    @Transactional
    public MesaResponse actualizar(Long id, MesaUpdateRequest request) {
        validarAlMenosUnCampo(request);
        Mesa mesa = buscarPorId(id);
        if (request.getNumero() != null && mesaRepository.existsByNumeroIgnoreCaseAndIdNot(request.getNumero(), id)) {
            throw new ConflictException("Ya existe otra mesa con ese número");
        }
        if (request.getQrToken() != null && mesaRepository.existsByQrToken(request.getQrToken())
                && !request.getQrToken().equals(mesa.getQrToken())) {
            throw new ConflictException("El token QR ya está en uso");
        }
        mesaMapper.updateEntity(mesa, request);
        return mesaMapper.toResponse(mesaRepository.save(mesa));
    }

    @Override
    @Transactional
    public MesaResponse actualizarEstado(Long id, EstadoMesa estado) {
        Mesa mesa = buscarPorId(id);
        mesa.setEstado(estado);
        return mesaMapper.toResponse(mesaRepository.save(mesa));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Mesa mesa = buscarPorId(id);
        if (pedidoRepository.existsByMesaIdAndEstadoIn(id, ESTADOS_PEDIDO_ACTIVOS)) {
            throw new ConflictException("No se puede inactivar la mesa porque tiene pedidos activos");
        }
        mesa.setActivo(false);
        mesaRepository.save(mesa);
    }

    private Mesa buscarPorId(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada con id: " + id));
    }

    private void validarAlMenosUnCampo(MesaUpdateRequest request) {
        if (request.getNumero() == null && request.getNombre() == null && request.getCapacidad() == null
                && request.getQrToken() == null && request.getEstado() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
