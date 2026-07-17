package com.comandago.api.producto.service;

import com.comandago.api.categoria.entity.Categoria;
import com.comandago.api.categoria.repository.CategoriaRepository;
import com.comandago.api.producto.dto.mapper.ProductoMapper;
import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoInsumoRequest;
import com.comandago.api.producto.dto.request.ProductoReordenarRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.dto.response.AlternativaInsumoResponse;
import com.comandago.api.producto.dto.response.IngredientePersonalizacionResponse;
import com.comandago.api.producto.dto.response.PersonalizacionProductoResponse;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.entity.Producto;
import com.comandago.api.producto.entity.ProductoInsumo;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.producto.enums.UnidadInsumo;
import com.comandago.api.producto.repository.ProductoRepository;
import com.comandago.api.receta.entity.Receta;
import com.comandago.api.receta.entity.RecetaIngrediente;
import com.comandago.api.receta.repository.RecetaRepository;
import com.comandago.api.shared.exception.BusinessException;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductoServiceImpl implements ProductoService {

    private static final List<TipoProducto> TIPOS_VENDIBLES =
            List.of(TipoProducto.COMPUESTO, TipoProducto.NORMAL, TipoProducto.INSUMO);

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final RecetaRepository recetaRepository;
    private final ProductoMapper productoMapper;
    private final SupabaseStorageService supabaseStorageService;

    @Override
    @Transactional
    public ProductoResponse crear(ProductoCreateRequest request) {
        validarPromocion(request.getEsPromocion(), request.getPrecio(), request.getPrecioPromocion());
        TipoProducto tipo = request.getTipo() != null ? request.getTipo() : TipoProducto.NORMAL;
        ProductoCategoriaRules.validarCategoriaPorTipo(tipo, request.getCategoriaId());
        ProductoComposicionRules.validarTipoComposicionAlCrear(request);
        ProductoRecetaRules.validarRecetaAlCrear(request);

        Producto producto = Producto.builder().build();
        if (request.getCategoriaId() != null) {
            Categoria categoria = resolverCategoriaHoja(request.getCategoriaId());
            producto.setCategoria(categoria);
            producto.setOrden(productoRepository.findMaxOrdenEnCategoria(categoria.getId()) + 1);
        } else {
            producto.setCategoria(null);
            producto.setOrden(0);
        }

        productoMapper.applyCreate(producto, request);
        producto.setImagenUrl(resolverImagenUrlParaCreacion(request.getImagenUrl()));
        producto.setReceta(resolverReceta(tipo, request.getRecetaId()));
        producto.getComposicion().clear();
        Producto guardado = productoRepository.save(producto);
        return productoMapper.toResponse(buscarConComposicion(guardado.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return productoMapper.toResponse(buscarConComposicion(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalizacionProductoResponse obtenerPersonalizacion(Long id) {
        Producto producto = buscarConComposicion(id);
        if (producto.getTipo() != TipoProducto.COMPUESTO) {
            throw new BusinessException("Solo los productos compuestos se pueden personalizar");
        }
        if (producto.getReceta() == null || producto.getReceta().getIngredientes() == null
                || producto.getReceta().getIngredientes().isEmpty()) {
            throw new BusinessException("El producto compuesto no tiene receta configurada");
        }

        List<RecetaIngrediente> ingredientes = producto.getReceta().getIngredientes().stream()
                .sorted(Comparator.comparing(ri -> ri.getOrden() != null ? ri.getOrden() : 0))
                .toList();

        Set<Long> categoriaIds = new HashSet<>();
        for (RecetaIngrediente ri : ingredientes) {
            if (Boolean.TRUE.equals(ri.getEsRemovible())
                    && ri.getProducto() != null
                    && ri.getProducto().getCategoria() != null) {
                categoriaIds.add(ri.getProducto().getCategoria().getId());
            }
        }

        Map<Long, List<Producto>> alternativasPorCategoria = new HashMap<>();
        for (Long categoriaId : categoriaIds) {
            List<Producto> disponibles = productoRepository
                    .findByActivoTrueAndDisponibleTrueAndCategoriaIdAndTipoInOrderByOrdenAsc(
                            categoriaId, List.of(TipoProducto.INSUMO));
            alternativasPorCategoria.put(categoriaId, disponibles);
        }

        List<IngredientePersonalizacionResponse> lineas = new ArrayList<>();
        for (RecetaIngrediente ri : ingredientes) {
            Producto insumo = ri.getProducto();
            Long categoriaId = insumo.getCategoria() != null ? insumo.getCategoria().getId() : null;
            String categoriaNombre = insumo.getCategoria() != null ? insumo.getCategoria().getNombre() : null;

            List<AlternativaInsumoResponse> alternativas = List.of();
            if (Boolean.TRUE.equals(ri.getEsRemovible()) && categoriaId != null) {
                alternativas = alternativasPorCategoria.getOrDefault(categoriaId, List.of()).stream()
                        .filter(alt -> !Objects.equals(alt.getId(), insumo.getId()))
                        .map(alt -> AlternativaInsumoResponse.builder()
                                .productoId(alt.getId())
                                .nombre(alt.getNombre())
                                .build())
                        .toList();
            }

            lineas.add(IngredientePersonalizacionResponse.builder()
                    .productoId(insumo.getId())
                    .nombre(insumo.getNombre())
                    .categoriaId(categoriaId)
                    .categoriaNombre(categoriaNombre)
                    .esRemovible(Boolean.TRUE.equals(ri.getEsRemovible()))
                    .orden(ri.getOrden() != null ? ri.getOrden() : 0)
                    .alternativas(alternativas)
                    .build());
        }

        return PersonalizacionProductoResponse.builder()
                .productoId(producto.getId())
                .nombre(producto.getNombre())
                .precioBase(producto.getPrecioFinal())
                .ingredientes(lineas)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductoResponse> listar(Long categoriaId, Boolean activo, Boolean disponible,
                                                 Boolean esPromocion, TipoProducto tipo, Pageable pageable) {
        Page<Producto> page = productoRepository.findAll(pageable);
        if (tipo != null) {
            page = productoRepository.findByTipo(tipo, pageable);
        } else if (categoriaId != null) {
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
                ? productoRepository.findByActivoTrueAndDisponibleTrueAndCategoriaIdAndTipoInOrderByOrdenAsc(
                        categoriaId, TIPOS_VENDIBLES)
                : productoRepository.findByActivoTrueAndDisponibleTrueAndTipoInAndCategoriaIsNotNullOrderByOrdenAsc(
                        TIPOS_VENDIBLES);
        return productos.stream().map(productoMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarInsumos() {
        return productoRepository.findByActivoTrueAndTipoOrderByNombreAsc(TipoProducto.INSUMO)
                .stream()
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarPromociones() {
        return productoRepository
                .findByEsPromocionTrueAndActivoTrueAndDisponibleTrueAndCategoriaIsNotNullOrderByOrdenAsc()
                .stream()
                .filter(p -> TIPOS_VENDIBLES.contains(p.getTipo()))
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductoResponse> menuDelDia() {
        return productoRepository.findByActivoTrueAndCategoriaIsNotNullOrderByCategoriaOrdenAscOrdenAsc()
                .stream()
                .filter(p -> TIPOS_VENDIBLES.contains(p.getTipo() != null ? p.getTipo() : TipoProducto.NORMAL))
                .map(productoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoUpdateRequest request) {
        validarAlMenosUnCampo(request);
        Producto producto = buscarConComposicion(id);
        TipoProducto tipoResultante = request.getTipo() != null ? request.getTipo() : producto.getTipo();
        ProductoComposicionRules.validarTipoComposicionAlActualizar(tipoResultante, request);
        Long recetaIdActual = producto.getReceta() != null ? producto.getReceta().getId() : null;
        ProductoRecetaRules.validarRecetaAlActualizar(tipoResultante, request, recetaIdActual);

        if (Boolean.TRUE.equals(request.getSinCategoria())) {
            if (tipoResultante != TipoProducto.INSUMO) {
                throw new BusinessException("Solo un producto tipo INSUMO puede quedar sin categoría");
            }
            producto.setCategoria(null);
            producto.setOrden(0);
        } else if (request.getCategoriaId() != null) {
            Long actualId = producto.getCategoria() != null ? producto.getCategoria().getId() : null;
            if (!request.getCategoriaId().equals(actualId)) {
                Categoria nueva = resolverCategoriaHoja(request.getCategoriaId());
                producto.setCategoria(nueva);
                producto.setOrden(productoRepository.findMaxOrdenEnCategoria(nueva.getId()) + 1);
            }
        }

        if (request.getImagenUrl() != null) {
            aplicarImagenUrlEnActualizacion(producto, request.getImagenUrl());
        }
        productoMapper.applyUpdate(producto, request);
        validarPromocion(producto.getEsPromocion(), producto.getPrecio(), producto.getPrecioPromocion());

        Long categoriaIdResultante = producto.getCategoria() != null ? producto.getCategoria().getId() : null;
        ProductoCategoriaRules.validarCategoriaPorTipo(producto.getTipo(), categoriaIdResultante);

        if (producto.getTipo() == TipoProducto.COMPUESTO) {
            if (request.getRecetaId() != null) {
                producto.setReceta(resolverReceta(TipoProducto.COMPUESTO, request.getRecetaId()));
            } else if (producto.getReceta() == null) {
                throw new BusinessException("Un producto compuesto debe tener una receta asignada");
            }
            producto.getComposicion().clear();
        } else {
            producto.setReceta(null);
            producto.getComposicion().clear();
        }

        productoRepository.save(producto);
        return productoMapper.toResponse(buscarConComposicion(id));
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

    @Override
    @Transactional
    public void reordenar(ProductoReordenarRequest request) {
        Long categoriaId = request.categoriaId();
        long activosEnCategoria = productoRepository.countByActivoTrueAndCategoriaId(categoriaId);

        if (request.ids().size() != activosEnCategoria) {
            throw new BusinessException(
                    "Debe incluir todos los productos activos de la categoría para reordenar");
        }

        List<Producto> productos = productoRepository.findAllById(request.ids());
        if (productos.size() != request.ids().size()) {
            throw new ResourceNotFoundException("Uno o más productos no existen");
        }

        Map<Long, Producto> porId = productos.stream()
                .collect(Collectors.toMap(Producto::getId, Function.identity()));

        for (int i = 0; i < request.ids().size(); i++) {
            Long prodId = request.ids().get(i);
            Producto producto = porId.get(prodId);
            if (producto == null) {
                throw new ResourceNotFoundException("Producto no encontrado con id: " + prodId);
            }
            if (!Boolean.TRUE.equals(producto.getActivo())) {
                throw new BusinessException("Solo se pueden reordenar productos activos");
            }
            if (producto.getCategoria() == null
                    || !Objects.equals(categoriaId, producto.getCategoria().getId())) {
                throw new BusinessException("Todos los productos deben pertenecer a la misma categoría");
            }
            producto.setOrden(i);
        }

        productoRepository.saveAll(productos);
    }

    private void aplicarComposicion(Producto compuesto, List<ProductoInsumoRequest> insumos) {
        for (ProductoInsumoRequest req : insumos) {
            Producto insumo = productoRepository.findById(req.getProductoInsumoId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Producto no encontrado con id: " + req.getProductoInsumoId()));

            if (insumo.getTipo() != TipoProducto.INSUMO) {
                throw new BusinessException(
                        "El producto '" + insumo.getNombre() + "' no es de tipo INSUMO");
            }
            if (Objects.equals(compuesto.getId(), insumo.getId())) {
                throw new BusinessException("Un producto no puede componerse a sí mismo");
            }
            if (Boolean.TRUE.equals(req.getEsExtra()) && req.getPrecioExtra() == null) {
                throw new BusinessException(
                        "El insumo extra '" + insumo.getNombre() + "' requiere precio extra");
            }

            ProductoInsumo pi = new ProductoInsumo();
            pi.setProductoCompuesto(compuesto);
            pi.setProductoInsumo(insumo);
            pi.setCantidad(req.getCantidad() != null ? req.getCantidad() : BigDecimal.ONE);
            pi.setUnidad(req.getUnidad() != null ? req.getUnidad() : UnidadInsumo.UND);
            pi.setEsRemovible(req.getEsRemovible() != null ? req.getEsRemovible() : true);
            pi.setEsExtra(req.getEsExtra() != null ? req.getEsExtra() : false);
            pi.setPrecioExtra(req.getPrecioExtra());
            pi.setOrden(req.getOrden() != null ? req.getOrden() : 0);
            compuesto.getComposicion().add(pi);
        }
    }

    private Receta resolverReceta(TipoProducto tipo, Long recetaId) {
        if (tipo != TipoProducto.COMPUESTO) {
            return null;
        }
        if (recetaId == null) {
            throw new BusinessException("Un producto compuesto debe tener una receta asignada");
        }
        Receta receta = recetaRepository.findById(recetaId)
                .orElseThrow(() -> new ResourceNotFoundException("Receta no encontrada con id: " + recetaId));
        if (!Boolean.TRUE.equals(receta.getActivo())) {
            throw new BusinessException("La receta '" + receta.getNombre() + "' está inactiva");
        }
        return receta;
    }

    private Producto buscarConComposicion(Long id) {
        return productoRepository.findByIdWithComposicion(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
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
        if (request.getCategoriaId() == null && request.getSinCategoria() == null
                && request.getNombre() == null && request.getDescripcion() == null
                && request.getPrecio() == null && request.getPrecioPromocion() == null
                && request.getImagenUrl() == null
                && request.getEsPromocion() == null && request.getDisponible() == null
                && request.getTipo() == null && request.getComposicion() == null
                && request.getRecetaId() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
    }

    private String resolverImagenUrlParaCreacion(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) {
            return null;
        }
        String normalizada = imagenUrl.trim();
        supabaseStorageService.validarUrlDelBucket(StorageBucket.PRODUCTOS, normalizada);
        return normalizada;
    }

    private void aplicarImagenUrlEnActualizacion(Producto producto, String imagenUrl) {
        String anterior = producto.getImagenUrl();

        if (imagenUrl.isBlank()) {
            eliminarImagenAnteriorSiCorresponde(anterior);
            producto.setImagenUrl(null);
            return;
        }

        String nueva = imagenUrl.trim();
        supabaseStorageService.validarUrlDelBucket(StorageBucket.PRODUCTOS, nueva);

        if (!Objects.equals(anterior, nueva)) {
            eliminarImagenAnteriorSiCorresponde(anterior);
            producto.setImagenUrl(nueva);
        }
    }

    private void eliminarImagenAnteriorSiCorresponde(String imagenUrl) {
        if (imagenUrl != null && supabaseStorageService.esUrlDelBucket(StorageBucket.PRODUCTOS, imagenUrl)) {
            supabaseStorageService.eliminarPorPublicUrl(StorageBucket.PRODUCTOS, imagenUrl);
        }
    }
}
