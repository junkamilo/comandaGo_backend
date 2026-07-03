package com.comandago.api.usuario.service;

import com.comandago.api.usuario.dto.request.UsuarioCreateRequest;
import com.comandago.api.usuario.dto.request.UsuarioPasswordUpdateRequest;
import com.comandago.api.usuario.dto.request.UsuarioUpdateRequest;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.shared.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface UsuarioService {

    UsuarioResponse crear(UsuarioCreateRequest request);

    UsuarioResponse obtenerPorId(Long id);

    PageResponse<UsuarioResponse> listar(Rol rol, Boolean activo, Pageable pageable);

    UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request);

    UsuarioResponse actualizarPassword(Long id, UsuarioPasswordUpdateRequest request);

    UsuarioResponse actualizarActivo(Long id, Boolean activo);

    void eliminar(Long id);
}
