package com.comandago.api.pedido.controller;

import com.comandago.api.pedido.dto.request.PedidoCreateRequest;
import com.comandago.api.pedido.dto.request.PedidoEstadoRequest;
import com.comandago.api.pedido.dto.request.PedidoUpdateRequest;
import com.comandago.api.pedido.dto.response.PedidoResponse;
import com.comandago.api.pedido.enums.EstadoPedido;
import com.comandago.api.pedido.enums.OrigenPedido;
import com.comandago.api.pedido.service.PedidoService;
import com.comandago.api.shared.response.ApiResponse;
import com.comandago.api.shared.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Validated
public class PedidoController {

    private final PedidoService pedidoService;

    @PostMapping
    public ResponseEntity<ApiResponse<PedidoResponse>> crear(@Valid @RequestBody PedidoCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pedido creado", pedidoService.crear(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PedidoResponse>>> listar(
            @RequestParam(required = false) EstadoPedido estado,
            @RequestParam(required = false) Long mesaId,
            @RequestParam(required = false) OrigenPedido origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime hasta,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                pedidoService.listar(estado, mesaId, origen, desde, hasta,
                        PageRequest.of(page, size, Sort.by("fechaPedido").descending()))));
    }

    @GetMapping("/cocina")
    public ResponseEntity<ApiResponse<List<PedidoResponse>>> cocina() {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.listarCocina()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PedidoResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PedidoResponse>> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PedidoUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<PedidoResponse>> actualizarEstado(
            @PathVariable @Positive Long id,
            @Valid @RequestBody PedidoEstadoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.actualizarEstado(id, request)));
    }

    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<PedidoResponse>> cancelar(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(pedidoService.cancelar(id)));
    }
}
