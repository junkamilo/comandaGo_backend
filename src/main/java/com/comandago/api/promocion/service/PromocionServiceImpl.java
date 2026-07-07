package com.comandago.api.promocion.service;

import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.promocion.dto.mapper.PromocionMapper;
import com.comandago.api.promocion.dto.request.PromocionRequest;
import com.comandago.api.promocion.dto.response.PromocionResponse;
import com.comandago.api.promocion.entity.Promocion;
import com.comandago.api.promocion.entity.TipoPromocion;
import com.comandago.api.promocion.repository.PromocionRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PromocionServiceImpl implements PromocionService {

    private final PromocionRepository promocionRepository;
    private final ProductoRepository productoRepository;
    private final PromocionMapper promocionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PromocionResponse> listar() {
        return promocionRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(promocionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PromocionResponse> vigentes() {
        return promocionRepository.findVigentes(OffsetDateTime.now()).stream()
                .map(promocionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PromocionResponse obtener(Long id) {
        return promocionMapper.toResponse(buscar(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PromocionResponse> findVigenteParaProducto(Long productoId) {
        return promocionRepository.findVigenteParaProducto(productoId, OffsetDateTime.now())
                .map(promocionMapper::toResponse);
    }

    @Override
    @Transactional
    public PromocionResponse crear(PromocionRequest request) {
        validarPorTipo(request);
        validarFechas(request);

        Promocion promocion = new Promocion();
        aplicar(promocion, request);
        promocion.setUsoActual(0);
        promocion.setProductos(resolverProductos(request.productoIds()));

        return promocionMapper.toResponse(promocionRepository.save(promocion));
    }

    @Override
    @Transactional
    public PromocionResponse actualizar(Long id, PromocionRequest request) {
        validarPorTipo(request);
        validarFechas(request);

        Promocion promocion = buscar(id);
        aplicar(promocion, request);
        promocion.setProductos(resolverProductos(request.productoIds()));

        return promocionMapper.toResponse(promocionRepository.save(promocion));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Promocion promocion = buscar(id);
        promocion.setActivo(false);
        promocionRepository.save(promocion);
    }

    @Override
    @Transactional
    public void incrementarUso(Long promoId) {
        Promocion promocion = buscar(promoId);
        promocion.setUsoActual(promocion.getUsoActual() + 1);
        promocionRepository.save(promocion);
    }

    private Promocion buscar(Long id) {
        return promocionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promoción no encontrada con id: " + id));
    }

    private Set<Producto> resolverProductos(Set<Long> ids) {
        List<Producto> productos = productoRepository.findAllById(ids);
        if (productos.size() != ids.size()) {
            throw new BusinessException("Uno o más productos no existen");
        }
        for (Producto producto : productos) {
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new BusinessException(
                        "El producto '" + producto.getNombre() + "' está inactivo. No se puede incluir en una promo.");
            }
        }
        return new HashSet<>(productos);
    }

    private void validarPorTipo(PromocionRequest request) {
        switch (request.tipo()) {
            case PORCENTAJE -> {
                if (request.valorPorcentaje() == null) {
                    throw new BusinessException("Una promo de porcentaje requiere valor_porcentaje");
                }
                double porcentaje = request.valorPorcentaje().doubleValue();
                if (porcentaje <= 0 || porcentaje > 100) {
                    throw new BusinessException("El porcentaje debe estar entre 1 y 100");
                }
            }
            case MONTO_FIJO -> {
                if (request.valorMonto() == null || request.valorMonto().doubleValue() <= 0) {
                    throw new BusinessException("Una promo de monto fijo requiere valor_monto mayor a 0");
                }
            }
            case PAGA_X_LLEVA_Y -> {
                if (request.pagaCantidad() == null || request.llevaCantidad() == null) {
                    throw new BusinessException("Una promo paga X lleva Y requiere ambas cantidades");
                }
                if (request.pagaCantidad() <= 0) {
                    throw new BusinessException("La cantidad a pagar debe ser mayor a 0");
                }
                if (request.llevaCantidad() <= request.pagaCantidad()) {
                    throw new BusinessException("La cantidad que lleva debe ser mayor a la que paga");
                }
            }
        }
    }

    private void validarFechas(PromocionRequest request) {
        if (request.fechaFin() != null && !request.fechaFin().isAfter(request.fechaInicio())) {
            throw new BusinessException("La fecha de fin debe ser posterior a la de inicio");
        }
    }

    private void aplicar(Promocion promocion, PromocionRequest request) {
        promocion.setNombre(request.nombre().trim());
        promocion.setDescripcion(request.descripcion());
        promocion.setTipo(request.tipo());
        promocion.setFechaInicio(request.fechaInicio());
        promocion.setFechaFin(request.fechaFin());
        promocion.setUsoMaximo(request.usoMaximo());
        promocion.setActivo(request.activo() != null ? request.activo() : true);

        promocion.setValorPorcentaje(null);
        promocion.setValorMonto(null);
        promocion.setPagaCantidad(null);
        promocion.setLlevaCantidad(null);

        switch (request.tipo()) {
            case PORCENTAJE -> promocion.setValorPorcentaje(request.valorPorcentaje());
            case MONTO_FIJO -> promocion.setValorMonto(request.valorMonto());
            case PAGA_X_LLEVA_Y -> {
                promocion.setPagaCantidad(request.pagaCantidad());
                promocion.setLlevaCantidad(request.llevaCantidad());
            }
        }
    }
}
