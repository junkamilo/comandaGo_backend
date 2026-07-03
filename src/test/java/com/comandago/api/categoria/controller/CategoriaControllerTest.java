package com.comandago.api.categoria.controller;

import com.comandago.api.categoria.dto.response.CategoriaResponse;
import com.comandago.api.categoria.service.CategoriaService;
import com.comandago.api.usuario.service.TokenService;
import com.comandago.api.shared.config.CustomUserDetailsService;
import com.comandago.api.shared.config.JwtService;
import com.comandago.api.shared.exception.GlobalExceptionHandler;
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

@WebMvcTest(CategoriaController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoriaService categoriaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private TokenService tokenService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_retorna201() throws Exception {
        when(categoriaService.crear(any())).thenReturn(
                CategoriaResponse.builder().id(1L).nombre("Bebidas").orden(0).activo(true).build());

        mockMvc.perform(post("/api/v1/categorias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Bebidas","orden":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nombre").value("Bebidas"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crear_sinNombre_retorna400() throws Exception {
        mockMvc.perform(post("/api/v1/categorias")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orden":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void obtener_noExiste_retorna404() throws Exception {
        when(categoriaService.obtenerPorId(99L))
                .thenThrow(new com.comandago.api.shared.exception.ResourceNotFoundException("Categoría no encontrada"));

        mockMvc.perform(get("/api/v1/categorias/99"))
                .andExpect(status().isNotFound());
    }
}
