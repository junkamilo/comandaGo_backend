package com.comandago.api.usuario.controller;

import com.comandago.api.shared.config.CustomUserDetailsService;
import com.comandago.api.shared.config.JwtService;
import com.comandago.api.shared.config.LoginRateLimiter;
import com.comandago.api.shared.exception.GlobalExceptionHandler;
import com.comandago.api.shared.exception.ConflictException;
import com.comandago.api.shared.exception.UnauthorizedException;
import com.comandago.api.usuario.dto.response.AuthResponse;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.service.AuthService;
import com.comandago.api.usuario.service.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private LoginRateLimiter loginRateLimiter;

    @MockitoBean
    private TokenService tokenService;

    @Test
    void register_exitoso_retorna201() throws Exception {
        when(authService.register(any())).thenReturn(AuthResponse.builder()
                .token("access")
                .refreshToken("refresh")
                .expiresIn(3600L)
                .id(1L)
                .nombre("Admin")
                .email("admin@test.com")
                .rol(Rol.ADMIN)
                .build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Admin","email":"admin@test.com","password":"Password1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access"))
                .andExpect(jsonPath("$.data.rol").value("ADMIN"));
    }

    @Test
    void register_bodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"","email":"invalid","password":"weak"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_bootstrapCerrado_retorna409() throws Exception {
        doThrow(new ConflictException("El registro inicial ya fue realizado. Use login o solicite acceso al administrador."))
                .when(authService).register(any());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Admin","email":"admin@test.com","password":"Password1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_exitoso_retorna200() throws Exception {
        when(authService.login(any())).thenReturn(AuthResponse.builder()
                .token("access")
                .refreshToken("refresh")
                .expiresIn(3600L)
                .id(1L)
                .nombre("Admin")
                .email("admin@test.com")
                .rol(Rol.ADMIN)
                .build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"Password1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("access"));
    }

    @Test
    void login_credencialesInvalidas_retorna401() throws Exception {
        doThrow(new UnauthorizedException("Credenciales inválidas")).when(authService).login(any());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void login_bodyInvalido_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void me_retornaPerfil() throws Exception {
        when(authService.me()).thenReturn(UsuarioResponse.builder()
                .id(1L)
                .nombre("Admin")
                .email("admin@test.com")
                .rol(Rol.ADMIN)
                .activo(true)
                .build());

        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("admin@test.com"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void cambiarPassword_retorna200() throws Exception {
        mockMvc.perform(patch("/api/v1/auth/me/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"passwordActual":"Password1","passwordNueva":"Password2"}
                                """))
                .andExpect(status().isOk());
    }
}
