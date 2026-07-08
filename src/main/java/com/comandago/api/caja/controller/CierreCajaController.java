package com.comandago.api.caja.controller;

import com.comandago.api.caja.dto.request.CerrarCajaRequest;
import com.comandago.api.caja.dto.response.CierreCajaResponse;
import com.comandago.api.caja.dto.response.PreviewCierreResponse;
import com.comandago.api.caja.service.CierreCajaService;
import com.comandago.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/caja")
@RequiredArgsConstructor
@Validated
public class CierreCajaController {

    private final CierreCajaService cierreCajaService;

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<PreviewCierreResponse>> preview() {
        return ResponseEntity.ok(ApiResponse.ok(cierreCajaService.preview()));
    }

    @PostMapping("/cerrar")
    public ResponseEntity<ApiResponse<CierreCajaResponse>> cerrar(@Valid @RequestBody CerrarCajaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Caja cerrada", cierreCajaService.cerrar(request)));
    }

    @GetMapping("/cierres")
    public ResponseEntity<ApiResponse<List<CierreCajaResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(cierreCajaService.listar()));
    }

    @GetMapping("/cierres/{id}")
    public ResponseEntity<ApiResponse<CierreCajaResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(cierreCajaService.obtener(id)));
    }
}
