package com.comandago.api.mesa.controller;

import com.comandago.api.mesa.dto.request.MesaCreateRequest;
import com.comandago.api.mesa.dto.request.MesaEstadoRequest;
import com.comandago.api.mesa.dto.request.MesaUpdateRequest;
import com.comandago.api.mesa.dto.response.MesaResponse;
import com.comandago.api.mesa.enums.EstadoMesa;
import com.comandago.api.mesa.service.MesaService;
import com.comandago.api.shared.response.ApiResponse;
import com.comandago.api.shared.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

@RestController
@RequestMapping("/api/v1/mesas")
@RequiredArgsConstructor
@Validated
public class MesaController {

    private final MesaService mesaService;

    @PostMapping
    public ResponseEntity<ApiResponse<MesaResponse>> crear(@Valid @RequestBody MesaCreateRequest request) {
        MesaResponse response = mesaService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Mesa creada", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<MesaResponse>>> listar(
            @RequestParam(required = false) EstadoMesa estado,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(mesaService.listar(estado, activo,
                PageRequest.of(page, size, Sort.by("numero").ascending()))));
    }

    @GetMapping("/qr/{token}")
    public ResponseEntity<ApiResponse<MesaResponse>> obtenerPorQr(@PathVariable @NotBlank String token) {
        return ResponseEntity.ok(ApiResponse.ok(mesaService.obtenerPorQrToken(token)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MesaResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mesaService.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MesaResponse>> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody MesaUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(mesaService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<MesaResponse>> actualizarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody MesaEstadoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(mesaService.actualizarEstado(id, request.getEstado())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable @Positive Long id) {
        mesaService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Mesa eliminada", null));
    }
}
