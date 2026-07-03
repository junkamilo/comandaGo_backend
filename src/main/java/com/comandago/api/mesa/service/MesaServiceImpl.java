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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        return conGrupo(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public MesaResponse obtenerPorQrToken(String token) {
        Mesa mesa = mesaRepository.findByQrToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada para el token QR"));
        return conGrupo(mesa);
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
    @Transactional(readOnly = true)
    public List<MesaResponse> listarPiso() {
        List<Mesa> mesas = mesaRepository.findByActivoTrueOrderByNumeroAsc();
        return conGrupoBatch(mesas);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MesaResponse> listarLibres() {
        return mesaRepository.findByActivoTrueAndEstadoOrderByNumeroAsc(EstadoMesa.LIBRE)
                .stream()
                .map(mesaMapper::toResponse)
                .toList();
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
    public List<MesaResponse> agrupar(List<Long> mesaIds) {
        if (mesaIds == null || mesaIds.size() < 2) {
            throw new BusinessException("Se necesitan al menos 2 mesas para agrupar");
        }

        Set<Long> idsUnicos = new HashSet<>(mesaIds);
        if (idsUnicos.size() != mesaIds.size()) {
            throw new BusinessException("No puedes repetir la misma mesa en el grupo");
        }

        List<Mesa> mesas = mesaRepository.findAllById(mesaIds);
        if (mesas.size() != mesaIds.size()) {
            throw new ResourceNotFoundException("Una o más mesas no existen");
        }

        for (Mesa mesa : mesas) {
            if (!Boolean.TRUE.equals(mesa.getActivo())) {
                throw new BusinessException("La mesa " + mesa.getNumero() + " está inactiva");
            }
            if (mesa.getGrupoId() != null) {
                throw new BusinessException(
                        "La mesa " + mesa.getNumero() + " ya está en un grupo. Sepárala primero.");
            }
        }

        String grupoId = UUID.randomUUID().toString();
        for (Mesa mesa : mesas) {
            mesa.setGrupoId(grupoId);
            mesa.setEstado(EstadoMesa.OCUPADA);
        }
        List<Mesa> guardadas = mesaRepository.saveAll(mesas);
        return conGrupoBatch(guardadas);
    }

    @Override
    @Transactional
    public void desagrupar(String grupoId) {
        List<Mesa> mesas = mesaRepository.findByGrupoId(grupoId);
        if (mesas.isEmpty()) {
            throw new ResourceNotFoundException("Grupo no encontrado: " + grupoId);
        }
        for (Mesa mesa : mesas) {
            mesa.setGrupoId(null);
            mesa.setEstado(EstadoMesa.LIBRE);
        }
        mesaRepository.saveAll(mesas);
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Mesa mesa = buscarPorId(id);
        if (mesa.getEstado() == EstadoMesa.OCUPADA) {
            throw new BusinessException("No puedes desactivar una mesa que está ocupada");
        }
        if (mesa.getGrupoId() != null) {
            throw new BusinessException("Separa el grupo antes de desactivar esta mesa");
        }
        if (pedidoRepository.existsByMesaIdAndEstadoIn(id, ESTADOS_PEDIDO_ACTIVOS)) {
            throw new ConflictException("No se puede inactivar la mesa porque tiene pedidos activos");
        }
        mesa.setActivo(false);
        mesa.setEstado(EstadoMesa.INACTIVA);
        mesaRepository.save(mesa);
    }

    private Mesa buscarPorId(Long id) {
        return mesaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mesa no encontrada con id: " + id));
    }

    private MesaResponse conGrupo(Mesa mesa) {
        if (mesa.getGrupoId() == null) {
            return mesaMapper.toResponse(mesa);
        }

        List<String> otrasDelGrupo = mesaRepository.findByGrupoId(mesa.getGrupoId())
                .stream()
                .filter(otra -> !otra.getId().equals(mesa.getId()))
                .map(Mesa::getNumero)
                .toList();
        return mesaMapper.toResponse(mesa, otrasDelGrupo);
    }

    private List<MesaResponse> conGrupoBatch(List<Mesa> mesas) {
        Set<String> grupoIds = mesas.stream()
                .map(Mesa::getGrupoId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toSet());

        Map<String, List<Mesa>> mesasPorGrupo = new HashMap<>();
        if (!grupoIds.isEmpty()) {
            for (Mesa mesaGrupo : mesaRepository.findByGrupoIdIn(grupoIds)) {
                mesasPorGrupo.computeIfAbsent(mesaGrupo.getGrupoId(), ignored -> new ArrayList<>())
                        .add(mesaGrupo);
            }
        }

        return mesas.stream()
                .map(mesa -> {
                    if (mesa.getGrupoId() == null) {
                        return mesaMapper.toResponse(mesa);
                    }
                    List<String> otrasDelGrupo = mesasPorGrupo.getOrDefault(mesa.getGrupoId(), List.of())
                            .stream()
                            .filter(otra -> !otra.getId().equals(mesa.getId()))
                            .map(Mesa::getNumero)
                            .toList();
                    return mesaMapper.toResponse(mesa, otrasDelGrupo);
                })
                .toList();
    }

    private void validarAlMenosUnCampo(MesaUpdateRequest request) {
        if (request.getNumero() == null && request.getNombre() == null && request.getCapacidad() == null
                && request.getQrToken() == null && request.getEstado() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
