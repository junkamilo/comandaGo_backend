package com.comandago.api.receta.controller;

import com.comandago.api.receta.dto.request.RecetaRequest;
import com.comandago.api.receta.dto.response.RecetaResponse;
import com.comandago.api.receta.service.RecetaService;
import com.comandago.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recetas")
@RequiredArgsConstructor
@Validated
public class RecetaController {

    private final RecetaService recetaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RecetaResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(recetaService.listar()));
    }

    @GetMapping("/activas")
    public ResponseEntity<ApiResponse<List<RecetaResponse>>> listarActivas() {
        return ResponseEntity.ok(ApiResponse.ok(recetaService.listarActivas()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecetaResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(recetaService.obtener(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecetaResponse>> crear(@Valid @RequestBody RecetaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Receta creada", recetaService.crear(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecetaResponse>> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RecetaRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Receta actualizada", recetaService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable @Positive Long id) {
        recetaService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Receta desactivada", null));
    }
}
