package com.comandago.api.categoria.service;

import com.comandago.api.categoria.dto.mapper.CategoriaMapper;
import com.comandago.api.categoria.dto.request.CategoriaCreateRequest;
import com.comandago.api.categoria.dto.request.CategoriaReordenarRequest;
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
import com.comandago.api.storage.StorageBucket;
import com.comandago.api.storage.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaMapper categoriaMapper;
    private final SupabaseStorageService supabaseStorageService;

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
        categoria.setImagenUrl(resolverImagenUrlParaCreacion(request.getImagenUrl()));
        categoria.setCategoriaPadre(resolverPadre(categoria, request.getCategoriaPadreId()));
        categoria.setOrden(categoriaRepository.findMaxOrdenEnNivel(request.getCategoriaPadreId()) + 1);
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
        if (request.getImagenUrl() != null) {
            aplicarImagenUrlEnActualizacion(categoria, request.getImagenUrl());
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

    @Override
    @Transactional
    public void reordenar(CategoriaReordenarRequest request) {
        Long padreId = request.categoriaPadreId();
        long activasEnNivel = padreId == null
                ? categoriaRepository.countByActivoTrueAndCategoriaPadreIsNull()
                : categoriaRepository.countByActivoTrueAndCategoriaPadreId(padreId);

        if (request.ids().size() != activasEnNivel) {
            throw new BusinessException(
                    "Debe incluir todas las categorías activas del nivel para reordenar");
        }

        List<Categoria> categorias = categoriaRepository.findAllById(request.ids());
        if (categorias.size() != request.ids().size()) {
            throw new ResourceNotFoundException("Una o más categorías no existen");
        }

        Map<Long, Categoria> porId = categorias.stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));

        for (int i = 0; i < request.ids().size(); i++) {
            Long id = request.ids().get(i);
            Categoria categoria = porId.get(id);
            if (categoria == null) {
                throw new ResourceNotFoundException("Categoría no encontrada con id: " + id);
            }
            if (!Boolean.TRUE.equals(categoria.getActivo())) {
                throw new BusinessException("Solo se pueden reordenar categorías activas");
            }
            Long padreActual = categoria.getCategoriaPadre() != null
                    ? categoria.getCategoriaPadre().getId()
                    : null;
            if (!Objects.equals(padreId, padreActual)) {
                throw new BusinessException("Todas las categorías deben pertenecer al mismo nivel");
            }
            categoria.setOrden(i);
        }

        categoriaRepository.saveAll(categorias);
    }

    private String resolverImagenUrlParaCreacion(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return null;
        }
        String normalizada = imagenUrl.trim();
        supabaseStorageService.validarUrlDelBucket(StorageBucket.CATEGORIAS, normalizada);
        return normalizada;
    }

    private void aplicarImagenUrlEnActualizacion(Categoria categoria, String imagenUrl) {
        String anterior = categoria.getImagenUrl();

        if (imagenUrl.isBlank()) {
            eliminarImagenAnteriorSiCorresponde(anterior);
            categoria.setImagenUrl(null);
            return;
        }

        String nueva = imagenUrl.trim();
        supabaseStorageService.validarUrlDelBucket(StorageBucket.CATEGORIAS, nueva);

        if (!Objects.equals(anterior, nueva)) {
            eliminarImagenAnteriorSiCorresponde(anterior);
            categoria.setImagenUrl(nueva);
        }
    }

    private void eliminarImagenAnteriorSiCorresponde(String imagenUrl) {
        if (imagenUrl != null && supabaseStorageService.esUrlDelBucket(StorageBucket.CATEGORIAS, imagenUrl)) {
            supabaseStorageService.eliminarPorPublicUrl(StorageBucket.CATEGORIAS, imagenUrl);
        }
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
                && request.getImagenUrl() == null
                && request.getCategoriaPadreId() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
