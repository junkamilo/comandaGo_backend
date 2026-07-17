package com.comandago.api.producto.controller;

import com.comandago.api.producto.dto.request.ProductoCreateRequest;
import com.comandago.api.producto.dto.request.ProductoDisponibilidadRequest;
import com.comandago.api.producto.dto.request.ProductoReordenarRequest;
import com.comandago.api.producto.dto.request.ProductoUpdateRequest;
import com.comandago.api.producto.dto.response.PersonalizacionProductoResponse;
import com.comandago.api.producto.dto.response.ProductoResponse;
import com.comandago.api.producto.enums.TipoProducto;
import com.comandago.api.producto.service.ProductoService;
import com.comandago.api.shared.response.ApiResponse;
import com.comandago.api.shared.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Validated
public class ProductoController {

    private final ProductoService productoService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductoResponse>> crear(@Valid @RequestBody ProductoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Producto creado", productoService.crear(request)));
    }

    @GetMapping("/menu")
    public ResponseEntity<ApiResponse<List<ProductoResponse>>> menu(
            @RequestParam(required = false) Long categoriaId) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.listarMenu(categoriaId)));
    }

    @GetMapping("/insumos")
    public ResponseEntity<ApiResponse<List<ProductoResponse>>> listarInsumos() {
        return ResponseEntity.ok(ApiResponse.ok(productoService.listarInsumos()));
    }

    @GetMapping("/promociones")
    public ResponseEntity<ApiResponse<List<ProductoResponse>>> promociones() {
        return ResponseEntity.ok(ApiResponse.ok(productoService.listarPromociones()));
    }

    @GetMapping("/menu-del-dia")
    public ResponseEntity<ApiResponse<List<ProductoResponse>>> menuDelDia() {
        return ResponseEntity.ok(ApiResponse.ok(productoService.menuDelDia()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProductoResponse>>> listar(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean disponible,
            @RequestParam(required = false) Boolean esPromocion,
            @RequestParam(required = false) TipoProducto tipo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.listar(categoriaId, activo, disponible, esPromocion, tipo,
                PageRequest.of(page, size, Sort.by("orden").ascending()))));
    }

    @PutMapping("/reordenar")
    public ResponseEntity<ApiResponse<Void>> reordenar(@Valid @RequestBody ProductoReordenarRequest request) {
        productoService.reordenar(request);
        return ResponseEntity.ok(ApiResponse.ok("Orden actualizado", null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductoResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtenerPorId(id)));
    }

    @GetMapping("/{id}/personalizacion")
    public ResponseEntity<ApiResponse<PersonalizacionProductoResponse>> personalizacion(
            @PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtenerPersonalizacion(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductoResponse>> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProductoUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<ApiResponse<ProductoResponse>> actualizarDisponibilidad(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ProductoDisponibilidadRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.actualizarDisponibilidad(id, request.getDisponible())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable @Positive Long id) {
        productoService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Producto eliminado", null));
    }
}
