package com.comandago.api.usuario.dto.response;

import com.comandago.api.usuario.enums.Rol;
import lombok.Builder;

@Builder
public record RolResponse(String codigo, String etiqueta) {

    public static RolResponse from(Rol rol) {
        return new RolResponse(rol.name(), formatLabel(rol));
    }

    private static String formatLabel(Rol rol) {
        return switch (rol) {
            case ADMIN -> "Administrador";
            case MESERO -> "Mesero";
            case COCINERO -> "Cocinero";
            case RECEPCIONISTA -> "Recepcionista";
            case CAJERO -> "Cajero";
        };
    }
}
