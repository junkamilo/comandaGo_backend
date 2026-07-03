package com.comandago.api.producto.service;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.producto.dto.mapper.ProductoMapper;
import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.response.PageResponse;
import com.comandago.api.shared.util.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoMapper productoMapper;

    @Override
    @Transactional
    public ProductoResponse crear(ProductoCreateRequest request) {
        validarPromocion(request.getEsPromocion(), request.getPrecio(), request.getPrecioPromocion());
        Categoria categoria = resolverCategoriaHoja(request.getCategoriaId());
        Producto producto = Producto.builder().categoria(categoria).build();
        productoMapper.applyCreate(producto, request);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return productoMapper.toResponse(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listar(Long categoriaId, Boolean activo, Boolean disponible,
                                                 Boolean esPromocion, Pageable pageable) {
        Page<Producto> page = productoRepository.findAll(pageable);
        if (categoriaId != null) {
            page = productoRepository.findByCategoriaId(categoriaId, pageable);
        } else if (activo != null) {
            page = productoRepository.findByActivo(activo, pageable);
        } else if (disponible != null) {
            page = productoRepository.findByDisponible(disponible, pageable);
        } else if (esPromocion != null) {
            page = productoRepository.findByEsPromocion(esPromocion, pageable);
        }
        return PaginationUtils.toPageResponse(page.map(productoMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarMenu(Long categoriaId) {
        List<Producto> productos = categoriaId != null
                ? productoRepository.findByActivoTrueAndDisponibleTrueAndCategoriaIdOrderByOrdenAsc(categoriaId)
                : productoRepository.findByActivoTrueAndDisponibleTrueOrderByOrdenAsc();
        return productos.stream().map(productoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPromociones() {
        return productoRepository.findByEsPromocionTrueAndActivoTrueAndDisponibleTrueOrderByOrdenAsc()
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> menuDelDia() {
        return productoRepository.findByActivoTrueOrderByCategoriaOrdenAscOrdenAsc()
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoUpdateRequest request) {
        validarAlMenosUnCampo(request);
        Producto producto = buscarPorId(id);
        if (request.getCategoriaId() != null) {
            producto.setCategoria(resolverCategoriaHoja(request.getCategoriaId()));
        }
        productoMapper.applyUpdate(producto, request);
        validarPromocion(producto.getEsPromocion(), producto.getPrecio(), producto.getPrecioPromocion());
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse actualizarDisponibilidad(Long id, Boolean disponible) {
        Producto producto = buscarPorId(id);
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new BusinessException("No puedes cambiar la disponibilidad de un producto inactivo");
        }
        producto.setDisponible(disponible);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        producto.setActivo(false);
        productoRepository.save(producto);
    }

    private Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    private Categoria resolverCategoriaHoja(Long categoriaId) {
        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + categoriaId));
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new BusinessException("No puedes asignar el producto a una categoría inactiva");
        }
        if (categoriaRepository.existsByCategoriaPadreId(categoriaId)) {
            throw new BusinessException(
                    "Esta categoría tiene subcategorías. Asigna el producto a una subcategoría, no a la categoría padre.");
        }
        return categoria;
    }

    private void validarPromocion(Boolean esPromocion, BigDecimal precio, BigDecimal precioPromocion) {
        if (!Boolean.TRUE.equals(esPromocion)) {
            return;
        }
        if (precioPromocion == null) {
            throw new BusinessException("Un producto en promoción requiere precio de promoción");
        }
        if (precioPromocion.compareTo(precio) >= 0) {
            throw new BusinessException("El precio de promoción debe ser menor al precio normal");
        }
    }

    private void validarAlMenosUnCampo(ProductoUpdateRequest request) {
        if (request.getCategoriaId() == null && request.getNombre() == null && request.getDescripcion() == null
                && request.getPrecio() == null && request.getPrecioPromocion() == null
                && request.getImagenUrl() == null && request.getTiempoPreparacionMin() == null
                && request.getEsPromocion() == null && request.getDisponible() == null && request.getOrden() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }
}
