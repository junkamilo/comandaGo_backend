package com.comandago.api.categoria.controller;

import com.comandago.api.categoria.dto.request.CategoriaActivoRequest;
import com.comandago.api.categoria.dto.request.CategoriaCreateRequest;
import com.comandago.api.categoria.dto.request.CategoriaReordenarRequest;
import com.comandago.api.categoria.dto.request.CategoriaUpdateRequest;
import com.comandago.api.categoria.dto.response.CategoriaResponse;
import com.comandago.api.categoria.service.CategoriaService;
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
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Validated
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping("/menu")
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> menu() {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.menu()));
    }

    @GetMapping("/todas")
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> listarTodas() {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.listarTodas()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoriaResponse>> crear(@Valid @RequestBody CategoriaCreateRequest request) {
        CategoriaResponse response = categoriaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Categoría creada", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CategoriaResponse>>> listar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) String nombre,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        PageResponse<CategoriaResponse> response = categoriaService.listar(
                activo, nombre, PageRequest.of(page, size, Sort.by("orden").ascending()));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.obtenerPorId(id)));
    }

    @PutMapping("/reordenar")
    public ResponseEntity<ApiResponse<Void>> reordenar(@Valid @RequestBody CategoriaReordenarRequest request) {
        categoriaService.reordenar(request);
        return ResponseEntity.ok(ApiResponse.ok("Orden actualizado", null));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoriaResponse>> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CategoriaUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<ApiResponse<CategoriaResponse>> actualizarActivo(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CategoriaActivoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.actualizarActivo(id, request.getActivo())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable @Positive Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Categoría eliminada", null));
    }
}
