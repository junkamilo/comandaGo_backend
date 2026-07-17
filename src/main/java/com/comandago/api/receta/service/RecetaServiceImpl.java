package com.comandago.api.receta.service;

import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.producto.enums.UnidadInsumo;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.receta.dto.mapper.RecetaMapper;
import com.comandago.api.receta.dto.request.IngredienteRequest;
import com.comandago.api.receta.dto.request.RecetaRequest;
import com.comandago.api.receta.dto.response.RecetaResponse;
import com.comandago.api.receta.entity.Receta;
import com.comandago.api.receta.entity.RecetaIngrediente;
import com.comandago.api.receta.repository.RecetaRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RecetaServiceImpl implements RecetaService {

    private final RecetaRepository recetaRepository;
    private final ProductoRepository productoRepository;
    private final RecetaMapper recetaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RecetaResponse> listar() {
        return recetaRepository.findAllWithIngredientesOrderByNombreAsc().stream()
                .map(recetaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecetaResponse> listarActivas() {
        return recetaRepository.findActivasWithIngredientesOrderByNombreAsc().stream()
                .map(recetaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecetaResponse obtener(Long id) {
        return recetaMapper.toResponse(buscarConIngredientes(id));
    }

    @Override
    @Transactional
    public RecetaResponse crear(RecetaRequest request) {
        if (recetaRepository.existsByNombreIgnoreCase(request.getNombre().trim())) {
            throw new BusinessException("Ya existe una receta con ese nombre");
        }

        Receta receta = new Receta();
        aplicar(receta, request);
        aplicarIngredientes(receta, request.getIngredientes());

        Receta guardada = recetaRepository.save(receta);
        return recetaMapper.toResponse(buscarConIngredientes(guardada.getId()));
    }

    @Override
    @Transactional
    public RecetaResponse actualizar(Long id, RecetaRequest request) {
        Receta receta = buscarConIngredientes(id);
        String nuevoNombre = request.getNombre().trim();

        if (!receta.getNombre().equalsIgnoreCase(nuevoNombre)
                && recetaRepository.existsByNombreIgnoreCase(nuevoNombre)) {
            throw new BusinessException("Ya existe una receta con ese nombre");
        }

        aplicar(receta, request);
        // clear + flush evita 409 por uq_receta_ingrediente: Hibernate no debe
        // INSERT de los mismos (receta_id, producto_id) antes de DELETE de huérfanos.
        receta.limpiarIngredientes();
        recetaRepository.saveAndFlush(receta);
        aplicarIngredientes(receta, request.getIngredientes());

        recetaRepository.save(receta);
        return recetaMapper.toResponse(buscarConIngredientes(id));
    }

    @Override
    @Transactional
    public void desactivar(Long id) {
        Receta receta = buscar(id);
        receta.setActivo(false);
        recetaRepository.save(receta);
    }

    private Receta buscar(Long id) {
        return recetaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada con id: " + id));
    }

    private Receta buscarConIngredientes(Long id) {
        return recetaRepository.findByIdWithIngredientes(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada con id: " + id));
    }

    private void aplicar(Receta receta, RecetaRequest request) {
        receta.setNombre(request.getNombre().trim());
        receta.setDescripcion(request.getDescripcion());
        receta.setPreparacion(request.getPreparacion());
        receta.setTiempoTotalMin(request.getTiempoTotalMin());
        receta.setPorciones(request.getPorciones());
        receta.setActivo(request.getActivo() != null ? request.getActivo() : true);
    }

    private void aplicarIngredientes(Receta receta, List<IngredienteRequest> ingredientes) {
        Set<Long> vistos = new HashSet<>();
        int index = 0;
        for (IngredienteRequest req : ingredientes) {
            if (!vistos.add(req.getProductoId())) {
                throw new BusinessException("No se puede repetir el mismo insumo en la receta");
            }

            Producto insumo = productoRepository.findById(req.getProductoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + req.getProductoId()));

            if (insumo.getTipo() != TipoProducto.INSUMO) {
                throw new BusinessException(
                        "El producto '" + insumo.getNombre() + "' no es de tipo INSUMO. "
                                + "Solo se pueden agregar insumos a una receta.");
            }
            if (!Boolean.TRUE.equals(insumo.getActivo())) {
                throw new BusinessException("El insumo '" + insumo.getNombre() + "' está inactivo");
            }
            RecetaIngrediente ri = new RecetaIngrediente();
            ri.setProducto(insumo);
            ri.setCantidad(req.getCantidad() != null ? req.getCantidad() : BigDecimal.ONE);
            ri.setUnidad(req.getUnidad() != null ? req.getUnidad() : UnidadInsumo.UND);
            ri.setEsRemovible(req.getEsRemovible() != null ? req.getEsRemovible() : false);
            ri.setOrden(req.getOrden() != null ? req.getOrden() : index);
            receta.agregarIngrediente(ri);
            index++;
        }
    }
}
