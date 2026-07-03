package com.comandago.api.categoria.service;

import com.comandago.api.categoria.dto.mapper.CategoriaMapper;
import com.comandago.api.categoria.dto.request.CategoriaCreateRequest;
import com.comandago.api.categoria.dto.request.CategoriaUpdateRequest;
import com.comandago.api.categoria.dto.response.CategoriaResponse;
import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.producto.repository.ProductoRepository;
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

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaCreateRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue(request.getNombre())) {
            throw new ConflictException("Ya existe una categoría activa con ese nombre");
        }
        Categoria categoria = categoriaMapper.toEntity(request);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoriaResponse obtenerPorId(Long id) {
        return categoriaMapper.toResponse(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategoriaResponse> listar(Boolean activo, String nombre, Pageable pageable) {
        Page<Categoria> page;
        if (activo != null && nombre != null && !nombre.isBlank()) {
            page = categoriaRepository.findByActivoAndNombreContainingIgnoreCase(activo, nombre, pageable);
        } else if (activo != null) {
            page = categoriaRepository.findByActivo(activo, pageable);
        } else if (nombre != null && !nombre.isBlank()) {
            page = categoriaRepository.findByNombreContainingIgnoreCase(nombre, pageable);
        } else {
            page = categoriaRepository.findAll(pageable);
        }
        return PaginationUtils.toPageResponse(page.map(categoriaMapper::toResponse));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaUpdateRequest request) {
        validarAlMenosUnCampo(request);
        Categoria categoria = buscarPorId(id);
        if (request.getNombre() != null
                && categoriaRepository.existsByNombreIgnoreCaseAndActivoTrueAndIdNot(request.getNombre(), id)) {
            throw new ConflictException("Ya existe otra categoría activa con ese nombre");
        }
        categoriaMapper.updateEntity(categoria, request);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse actualizarActivo(Long id, Boolean activo) {
        Categoria categoria = buscarPorId(id);
        categoria.setActivo(activo);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscarPorId(id);
        if (productoRepository.existsByCategoriaIdAndActivoTrue(id)) {
            throw new ConflictException("No se puede eliminar la categoría porque tiene productos activos");
        }
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    private Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    private void validarAlMenosUnCampo(CategoriaUpdateRequest request) {
        if (request.getNombre() == null && request.getDescripcion() == null
                && request.getImagenUrl() == null && request.getOrden() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
