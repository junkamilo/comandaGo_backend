package com.comandago.api.usuario.controller;

import com.comandago.api.shared.config.CustomUserDetailsService;
import com.comandago.api.shared.config.JwtService;
import com.comandago.api.shared.exception.GlobalExceptionHandler;
import com.comandago.api.shared.exception.ResourceNotFoundException;
import com.comandago.api.usuario.dto.response.UsuarioResponse;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.service.UsuarioService;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_retorna201() throws Exception {
        when(usuarioService.crear(any())).thenReturn(UsuarioResponse.builder()
                .id(1L)
                .nombre("Mesero")
                .email("mesero@test.com")
                .rol(Rol.MESERO)
                .activo(true)
                .build());

        mockMvc.perform(post("/api/v1/usuarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mesero","email":"mesero@test.com","password":"Password1","rol":"MESERO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("mesero@test.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_passwordDebil_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Mesero","email":"mesero@test.com","password":"weak","rol":"MESERO"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtener_noExiste_retorna404() throws Exception {
        when(usuarioService.obtenerPorId(99L))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        mockMvc.perform(get("/api/v1/usuarios/99"))
                .andExpect(status().isNotFound());
    }
}
