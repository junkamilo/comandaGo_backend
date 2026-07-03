package com.comandago.api.usuario.dto.response;

import com.comandago.api.usuario.enums.Rol;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        Rol rol,
        Boolean activo,
        OffsetDateTime ultimoAcceso,
        OffsetDateTime fechaCreacion,
        OffsetDateTime fechaActualizacion
) {
}
