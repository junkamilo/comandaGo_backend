package com.comandago.api.usuario.dto.mapper;

import com.comandago.api.usuario.dto.request.RegisterRequest;
import com.comandago.api.usuario.dto.request.UsuarioCreateRequest;
import com.comandago.api.usuario.dto.request.UsuarioUpdateRequest;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class UsuarioMapper {

    public Usuario toEntity(UsuarioCreateRequest request, String passwordHash) {
        return Usuario.builder()
                .nombre(trimToNull(request.getNombre()))
                .email(normalizeEmail(request.getEmail()))
                .passwordHash(passwordHash)
                .telefono(trimToNull(request.getTelefono()))
                .rol(request.getRol())
                .activo(true)
                .build();
    }

    public Usuario toBootstrapAdmin(RegisterRequest request, String passwordHash) {
        return Usuario.builder()
                .nombre(trimToNull(request.getNombre()))
                .email(normalizeEmail(request.getEmail()))
                .passwordHash(passwordHash)
                .telefono(trimToNull(request.getTelefono()))
                .rol(Rol.ADMIN)
                .activo(true)
                .build();
    }

    public void updateEntity(Usuario usuario, UsuarioUpdateRequest request) {
        if (request.getNombre() != null) {
            usuario.setNombre(trimToNull(request.getNombre()));
        }
        if (request.getEmail() != null) {
            usuario.setEmail(normalizeEmail(request.getEmail()));
        }
        if (request.getTelefono() != null) {
            usuario.setTelefono(trimToNull(request.getTelefono()));
        }
        if (request.getRol() != null) {
            usuario.setRol(request.getRol());
        }
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol())
                .activo(usuario.getActivo())
                .ultimoAcceso(usuario.getUltimoAcceso())
                .fechaCreacion(usuario.getFechaCreacion())
                .fechaActualizacion(usuario.getFechaActualizacion())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
