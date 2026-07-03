package com.comandago.api.usuario.dto.response;

import com.comandago.api.usuario.enums.Rol;
import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        String refreshToken,
        Long expiresIn,
        Long id,
        String nombre,
        String email,
        Rol rol
) {
}
