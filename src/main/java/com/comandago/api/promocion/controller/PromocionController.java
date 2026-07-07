package com.comandago.api.promocion.controller;

import com.comandago.api.promocion.dto.request.PromocionRequest;
import com.comandago.api.promocion.dto.response.PromocionResponse;
import com.comandago.api.promocion.service.PromocionService;
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
@RequestMapping("/api/v1/promociones")
@RequiredArgsConstructor
@Validated
public class PromocionController {

    private final PromocionService promocionService;

    @GetMapping("/vigentes")
    public ResponseEntity<ApiResponse<List<PromocionResponse>>> vigentes() {
        return ResponseEntity.ok(ApiResponse.ok(promocionService.vigentes()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromocionResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(promocionService.listar()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromocionResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(promocionService.obtener(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromocionResponse>> crear(@Valid @RequestBody PromocionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Promoción creada", promocionService.crear(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromocionResponse>> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PromocionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Promoción actualizada", promocionService.actualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable @Positive Long id) {
        promocionService.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok("Promoción desactivada", null));
    }
}
