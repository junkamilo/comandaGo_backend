package com.comandago.api.pedido.controller;

import com.comandago.api.pedido.dto.request.DetalleEstadoRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoItemRequest;
import com.comandago.api.pedido.dto.request.DetallePedidoUpdateRequest;
import com.comandago.api.pedido.dto.response.DetallePedidoResponse;
import com.comandago.api.pedido.service.DetallePedidoService;
import com.comandago.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos/{pedidoId}/detalles")
@RequiredArgsConstructor
@Validated
public class DetallePedidoController {

    private final DetallePedidoService detallePedidoService;

    @PostMapping
    public ResponseEntity<ApiResponse<DetallePedidoResponse>> agregar(
            @PathVariable @Positive Long pedidoId,
            @Valid @RequestBody DetallePedidoItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Detalle agregado", detallePedidoService.agregar(pedidoId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DetallePedidoResponse>>> listar(@PathVariable @Positive Long pedidoId) {
        return ResponseEntity.ok(ApiResponse.ok(detallePedidoService.listar(pedidoId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DetallePedidoResponse>> actualizar(
            @PathVariable @Positive Long pedidoId,
            @PathVariable @Positive Long id,
            @Valid @RequestBody DetallePedidoUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(detallePedidoService.actualizar(pedidoId, id, request)));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ApiResponse<DetallePedidoResponse>> actualizarEstado(
            @PathVariable @Positive Long pedidoId,
            @PathVariable @Positive Long id,
            @Valid @RequestBody DetalleEstadoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(detallePedidoService.actualizarEstado(pedidoId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable @Positive Long pedidoId,
            @PathVariable @Positive Long id) {
        detallePedidoService.eliminar(pedidoId, id);
        return ResponseEntity.ok(ApiResponse.ok("Detalle eliminado", null));
    }
}
