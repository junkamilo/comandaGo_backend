package com.comandago.api.usuario.service;

import com.comandago.api.shared.exception.BusinessException;
import com.comandago.api.usuario.dto.mapper.UsuarioMapper;
import com.comandago.api.usuario.dto.request.UsuarioCreateRequest;
import com.comandago.api.usuario.dto.request.UsuarioUpdateRequest;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void crear_conRolAdmin_lanzaBusinessException() {
        UsuarioCreateRequest request = new UsuarioCreateRequest();
        request.setNombre("Otro Admin");
        request.setEmail("admin2@test.com");
        request.setPassword("Password1");
        request.setRol(Rol.ADMIN);

        assertThatThrownBy(() -> usuarioService.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("solo puede gestionar personal");
    }

    @Test
    void desactivarUltimoAdmin_lanzaBusinessException() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.countByRolAndActivo(Rol.ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.actualizarActivo(1L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("último administrador");
    }

    @Test
    void actualizar_cambiarRolAUltimoAdmin_lanzaBusinessException() {
        Usuario admin = Usuario.builder().id(1L).rol(Rol.ADMIN).activo(true).build();
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setRol(Rol.MESERO);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.countByRolAndActivo(Rol.ADMIN, true)).thenReturn(1L);

        assertThatThrownBy(() -> usuarioService.actualizar(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("último administrador");
    }

    @Test
    void actualizar_cambiarRol_revocaTokens() {
        Usuario mesero = Usuario.builder().id(2L).rol(Rol.MESERO).activo(true).build();
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setRol(Rol.CAJERO);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(mesero));
        when(usuarioRepository.save(mesero)).thenReturn(mesero);
        when(usuarioMapper.toResponse(mesero)).thenReturn(UsuarioResponse.builder()
                .id(2L)
                .rol(Rol.CAJERO)
                .activo(true)
                .build());

        usuarioService.actualizar(2L, request);

        verify(tokenService).revokeAllRefreshForUser(2L);
    }

    @Test
    void actualizar_sinCambioDeRol_noRevocaTokens() {
        Usuario mesero = Usuario.builder().id(2L).rol(Rol.MESERO).activo(true).build();
        UsuarioUpdateRequest request = new UsuarioUpdateRequest();
        request.setNombre("Mesero Actualizado");

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(mesero));
        when(usuarioRepository.save(mesero)).thenReturn(mesero);
        when(usuarioMapper.toResponse(mesero)).thenReturn(UsuarioResponse.builder()
                .id(2L)
                .nombre("Mesero Actualizado")
                .rol(Rol.MESERO)
                .activo(true)
                .build());

        usuarioService.actualizar(2L, request);

        verify(tokenService, never()).revokeAllRefreshForUser(any());
    }
}
