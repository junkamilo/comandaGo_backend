package com.comandago.api.usuario.controller;

import com.comandago.api.shared.response.ApiResponse;
import com.comandago.api.shared.response.PageResponse;
import com.comandago.api.usuario.dto.request.UsuarioActivoRequest;
import com.comandago.api.usuario.dto.request.UsuarioCreateRequest;
import com.comandago.api.usuario.dto.request.UsuarioPasswordUpdateRequest;
import com.comandago.api.usuario.dto.request.UsuarioUpdateRequest;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.service.UsuarioService;
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

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Validated
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario creado", usuarioService.crear(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UsuarioResponse>>> listar(
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.listar(rol, activo,
                PageRequest.of(page, size, Sort.by("nombre").ascending()))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtener(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarPassword(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UsuarioPasswordUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.actualizarPassword(id, request)));
    }

    @PatchMapping("/{id}/activo")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizarActivo(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UsuarioActivoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioService.actualizarActivo(id, request.getActivo())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable @Positive Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.ok(ApiResponse.ok("Usuario eliminado", null));
    }
}
