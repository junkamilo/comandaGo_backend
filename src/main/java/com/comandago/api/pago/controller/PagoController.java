package com.comandago.api.pago.controller;

import com.comandago.api.pago.dto.request.PagoCreateRequest;
import com.comandago.api.pago.dto.response.PagoResponse;
import com.comandago.api.pago.service.PagoService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
public class PagoController {

    private final PagoService pagoService;

    @PostMapping("/api/v1/pagos")
    public ResponseEntity<ApiResponse<PagoResponse>> crear(@Valid @RequestBody PagoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pago registrado", pagoService.crear(request)));
    }

    @GetMapping("/api/v1/pagos")
    public ResponseEntity<ApiResponse<PageResponse<PagoResponse>>> listar(
            @RequestParam(required = false) Long pedidoId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.listar(pedidoId,
                PageRequest.of(page, size, Sort.by("fechaPago").descending()))));
    }

    @GetMapping("/api/v1/pagos/{id}")
    public ResponseEntity<ApiResponse<PagoResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.obtenerPorId(id)));
    }

    @GetMapping("/api/v1/pedidos/{pedidoId}/pagos")
    public ResponseEntity<ApiResponse<List<PagoResponse>>> listarPorPedido(
            @PathVariable @Positive Long pedidoId) {
        return ResponseEntity.ok(ApiResponse.ok(pagoService.listarPorPedido(pedidoId)));
    }
}
