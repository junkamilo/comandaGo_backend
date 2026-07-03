package com.comandago.api.usuario.service;

import com.comandago.api.shared.config.LoginRateLimiter;
import com.comandago.api.shared.exception.ConflictException;
import com.comandago.api.usuario.dto.mapper.UsuarioMapper;
import com.comandago.api.usuario.dto.request.RegisterRequest;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private TokenService tokenService;

    @Mock
    private com.comandago.api.shared.config.JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_cuandoYaHayUsuarios_lanzaConflictException() {
        when(usuarioRepository.count()).thenReturn(1L);

        RegisterRequest request = new RegisterRequest();
        request.setNombre("Admin");
        request.setEmail("admin@test.com");
        request.setPassword("Password1");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("registro inicial ya fue realizado");

        verify(usuarioRepository, never()).save(any());
        verify(tokenService, never()).issueTokens(any());
    }

    @Test
    void register_bdVacia_creaAdminYEmiteTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setNombre("Admin");
        request.setEmail("admin@test.com");
        request.setPassword("Password1");

        Usuario usuario = Usuario.builder()
                .id(1L)
                .nombre("Admin")
                .email("admin@test.com")
                .rol(Rol.ADMIN)
                .activo(true)
                .build();

        when(usuarioRepository.count()).thenReturn(0L);
        when(usuarioRepository.existsByEmailIgnoreCase("admin@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hash");
        when(usuarioMapper.toBootstrapAdmin(request, "hash")).thenReturn(usuario);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(tokenService.issueTokens(usuario)).thenReturn(
                new TokenService.TokenPair("access", "refresh", 3600L));

        var response = authService.register(request);

        assertThat(response.token()).isEqualTo("access");
        assertThat(response.rol()).isEqualTo(Rol.ADMIN);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(Rol.ADMIN);
        verify(tokenService).issueTokens(usuario);
        verify(loginRateLimiter).reset(anyString());
    }
}
