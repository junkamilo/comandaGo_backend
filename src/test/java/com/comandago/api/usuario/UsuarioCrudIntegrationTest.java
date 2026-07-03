package com.comandago.api.usuario;

import com.comandago.api.AbstractIntegrationTest;
import com.comandago.api.usuario.entity.Usuario;
import com.comandago.api.usuario.enums.Rol;
import com.comandago.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
class UsuarioCrudIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        usuarioRepository.deleteAll();
    }

    @Test
    void crudPersonal_comoAdmin_flujoCompleto() throws Exception {
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nombre("Admin Test")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("Password1"))
                .rol(Rol.ADMIN)
                .activo(true)
                .build());

        String token = loginComoAdmin();

        String createResponse = mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"María Mesera","email":"mesero@test.com","password":"Password1","rol":"MESERO"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rol").value("MESERO"))
                .andExpect(jsonPath("$.data.activo").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long meseroId = Long.parseLong(createResponse.split("\"id\":")[1].split(",")[0]);

        mockMvc.perform(post("/api/v1/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Otro Admin","email":"admin2@test.com","password":"Password1","rol":"ADMIN"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("personal")));

        mockMvc.perform(get("/api/v1/usuarios?activo=true")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(put("/api/v1/usuarios/" + meseroId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"María López"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nombre").value("María López"));

        mockMvc.perform(delete("/api/v1/usuarios/" + meseroId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/usuarios/" + meseroId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activo").value(false));

        mockMvc.perform(delete("/api/v1/usuarios/" + admin.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("desactivarte")));

        assertThat(usuarioRepository.findById(admin.getId())).isPresent();
        assertThat(usuarioRepository.findById(admin.getId()).orElseThrow().getActivo()).isTrue();
    }

    private String loginComoAdmin() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@test.com","password":"Password1"}
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return loginResponse.split("\"token\":\"")[1].split("\"")[0];
    }
}
