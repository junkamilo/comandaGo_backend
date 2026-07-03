package com.comandago.api.usuario.controller;

import com.comandago.api.shared.response.ApiResponse;
import com.comandago.api.usuario.dto.request.AuthRequest;
import com.comandago.api.usuario.dto.request.RegisterRequest;
import com.comandago.api.usuario.dto.request.LogoutRequest;
import com.comandago.api.usuario.dto.request.RefreshTokenRequest;
import com.comandago.api.usuario.dto.request.UsuarioPasswordChangeRequest;
import com.comandago.api.usuario.dto.response.AuthResponse;
import com.comandago.api.usuario.dto.response.RolResponse;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registro exitoso", authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Token renovado", authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody(required = false) LogoutRequest request) {
        authService.logout(authorization, request);
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UsuarioResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(authService.me()));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(@Valid @RequestBody UsuarioPasswordChangeRequest request) {
        authService.cambiarPasswordPropia(request);
        return ResponseEntity.ok(ApiResponse.ok("Contraseña actualizada", null));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<RolResponse>>> roles() {
        return ResponseEntity.ok(ApiResponse.ok(authService.listarRoles()));
    }
}
