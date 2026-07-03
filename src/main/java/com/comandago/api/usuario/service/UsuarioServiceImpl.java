package com.comandago.api.usuario.service;

import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.shared.exception.ConflictException;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.shared.response.PageResponse;
import com.comandago.api.shared.security.SecurityUtils;
import com.comandago.api.shared.security.UsuarioPrincipal;
import com.comandago.api.shared.util.PaginationUtils;
import com.comandago.api.usuario.dto.mapper.UsuarioMapper;
import com.comandago.api.usuario.dto.request.UsuarioCreateRequest;
import com.comandago.api.usuario.dto.request.UsuarioPasswordUpdateRequest;
import com.comandago.api.usuario.dto.request.UsuarioUpdateRequest;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final Set<Rol> ROLES_STAFF =
            EnumSet.of(Rol.MESERO, Rol.COCINERO, Rol.CAJERO, Rol.RECEPCIONISTA);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse crear(UsuarioCreateRequest request) {
        validarRolStaff(request.getRol());
        if (usuarioRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario con ese email");
        }
        String hash = passwordEncoder.encode(request.getPassword());
        Usuario usuario = usuarioMapper.toEntity(request, hash);
        aplicarAuditoriaCreacion(usuario);
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse obtenerPorId(Long id) {
        return usuarioMapper.toResponse(buscarPorId(id));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UsuarioResponse> listar(Rol rol, Boolean activo, Pageable pageable) {
        Page<Usuario> page;
        if (rol != null && activo != null) {
            page = usuarioRepository.findByRolAndActivo(rol, activo, pageable);
        } else if (rol != null) {
            page = usuarioRepository.findByRol(rol, pageable);
        } else if (activo != null) {
            page = usuarioRepository.findByActivo(activo, pageable);
        } else {
            page = usuarioRepository.findAll(pageable);
        }
        return PaginationUtils.toPageResponse(page.map(usuarioMapper::toResponse));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse actualizar(Long id, UsuarioUpdateRequest request) {
        validarAlMenosUnCampo(request);
        Usuario usuario = buscarPorId(id);
        if (request.getEmail() != null && usuarioRepository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id)) {
            throw new ConflictException("Ya existe otro usuario con ese email");
        }
        Rol rolAnterior = usuario.getRol();
        if (request.getRol() != null) {
            validarRolStaff(request.getRol());
            if (rolAnterior == Rol.ADMIN
                    && request.getRol() != Rol.ADMIN
                    && Boolean.TRUE.equals(usuario.getActivo())) {
                validarNoUltimoAdmin(usuario);
            }
        }
        usuarioMapper.updateEntity(usuario, request);
        aplicarAuditoriaModificacion(usuario);
        if (request.getRol() != null && request.getRol() != rolAnterior) {
            tokenService.revokeAllRefreshForUser(usuario.getId());
        }
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse actualizarPassword(Long id, UsuarioPasswordUpdateRequest request) {
        Usuario usuario = buscarPorId(id);
        usuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        aplicarAuditoriaModificacion(usuario);
        tokenService.revokeAllRefreshForUser(usuario.getId());
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse actualizarActivo(Long id, Boolean activo) {
        Usuario usuario = buscarPorId(id);
        if (Boolean.FALSE.equals(activo)) {
            validarNoAutoDesactivacion(id);
            validarNoUltimoAdmin(usuario);
        }
        usuario.setActivo(activo);
        aplicarAuditoriaModificacion(usuario);
        if (Boolean.FALSE.equals(activo)) {
            tokenService.revokeAllRefreshForUser(usuario.getId());
        }
        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(Long id) {
        validarNoAutoDesactivacion(id);
        Usuario usuario = buscarPorId(id);
        validarNoUltimoAdmin(usuario);
        usuario.setActivo(false);
        aplicarAuditoriaModificacion(usuario);
        tokenService.revokeAllRefreshForUser(usuario.getId());
        usuarioRepository.save(usuario);
    }

    private Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private void validarAlMenosUnCampo(UsuarioUpdateRequest request) {
        if (request.getNombre() == null && request.getEmail() == null
                && request.getTelefono() == null && request.getRol() == null) {
            throw new BusinessException("Debe enviar al menos un campo para actualizar");
        }
        if (request.getNombre() != null && !StringUtils.hasText(request.getNombre())) {
            throw new BusinessException("El nombre no puede estar vacío");
        }
        if (request.getEmail() != null && !StringUtils.hasText(request.getEmail())) {
            throw new BusinessException("El email no puede estar vacío");
        }
    }

    private void validarRolStaff(Rol rol) {
        if (rol == null || !ROLES_STAFF.contains(rol)) {
            throw new BusinessException(
                    "Un ADMIN solo puede gestionar personal (MESERO, COCINERO, CAJERO, RECEPCIONISTA)");
        }
    }

    private void validarNoAutoDesactivacion(Long id) {
        UsuarioPrincipal actor = SecurityUtils.currentUserOrNull();
        if (actor != null && actor.getId().equals(id)) {
            throw new BusinessException("No puedes desactivarte a ti mismo");
        }
    }

    private void validarNoUltimoAdmin(Usuario usuario) {
        if (usuario.getRol() == Rol.ADMIN && Boolean.TRUE.equals(usuario.getActivo())) {
            long activos = usuarioRepository.countByRolAndActivo(Rol.ADMIN, true);
            if (activos <= 1) {
                throw new BusinessException("No se puede desactivar el último administrador activo");
            }
        }
    }

    private void aplicarAuditoriaCreacion(Usuario usuario) {
        UsuarioPrincipal actor = SecurityUtils.currentUserOrNull();
        if (actor != null) {
            usuario.setCreadoPor(usuarioRepository.getReferenceById(actor.getId()));
            usuario.setModificadoPor(usuario.getCreadoPor());
        }
    }

    private void aplicarAuditoriaModificacion(Usuario usuario) {
        UsuarioPrincipal actor = SecurityUtils.currentUserOrNull();
        if (actor != null) {
            usuario.setModificadoPor(usuarioRepository.getReferenceById(actor.getId()));
        }
    }
}
