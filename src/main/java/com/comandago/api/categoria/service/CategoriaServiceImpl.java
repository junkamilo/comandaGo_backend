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

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> menu() {
        return categoriaRepository.findByActivoTrueAndCategoriaPadreIsNullOrderByOrdenAsc()
                .stream()
                .map(padre -> categoriaMapper.toResponseConHijas(
                        padre,
                        categoriaRepository.findByActivoTrueAndCategoriaPadreIdOrderByOrdenAsc(padre.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarTodas() {
        return categoriaRepository.findAllByOrderByOrdenAsc()
                .stream()
                .map(categoriaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoriaResponse crear(CategoriaCreateRequest request) {
        if (categoriaRepository.existsByNombreIgnoreCaseAndActivoTrue(request.getNombre())) {
            throw new ConflictException("Ya existe una categoría activa con ese nombre");
        }
        Categoria categoria = categoriaMapper.toEntity(request);
        categoria.setCategoriaPadre(resolverPadre(categoria, request.getCategoriaPadreId()));
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
        if (request.getCategoriaPadreId() != null) {
            categoria.setCategoriaPadre(resolverPadre(categoria, request.getCategoriaPadreId()));
        }
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
        if (categoriaRepository.existsByCategoriaPadreIdAndActivoTrue(id)) {
            throw new ConflictException(
                    "No puedes desactivar una categoría con subcategorías activas. Desactiva o reasigna las hijas primero.");
        }
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    private Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    private Categoria resolverPadre(Categoria hija, Long padreId) {
        if (padreId == null) {
            return null;
        }
        if (hija.getId() != null && hija.getId().equals(padreId)) {
            throw new BusinessException("Una categoría no puede ser su propio padre");
        }
        if (hija.getId() != null && categoriaRepository.existsByCategoriaPadreIdAndActivoTrue(hija.getId())) {
            throw new BusinessException("Una categoría con subcategorías no puede convertirse en subcategoría");
        }
        Categoria padre = categoriaRepository.findById(padreId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + padreId));
        if (!Boolean.TRUE.equals(padre.getActivo())) {
            throw new BusinessException("La categoría padre no está activa");
        }
        if (padre.getCategoriaPadre() != null) {
            throw new BusinessException("No se permiten subcategorías de más de 2 niveles");
        }
        return padre;
    }

    private void validarAlMenosUnCampo(CategoriaUpdateRequest request) {
        if (request.getNombre() == null && request.getDescripcion() == null
                && request.getImagenUrl() == null && request.getOrden() == null
                && request.getCategoriaPadreId() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
