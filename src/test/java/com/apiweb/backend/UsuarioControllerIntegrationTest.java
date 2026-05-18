package com.apiweb.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @BeforeEach
    void setUp() {
        usuarioRepository.deleteAll();
        usuarioRepository.save(new UsuarioModel(
                null,
                "Secretaria Ingenieria",
                "secretaria.ingenieria@uao.edu.co",
                "ClaveSegura1!",
                "SECRETARIA",
                1));
    }

    @Test
    void debePermitirLoginConJson() throws Exception {
        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "secretaria.ingenieria@uao.edu.co",
                                  "password": "ClaveSegura1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("secretaria.ingenieria@uao.edu.co"))
                .andExpect(jsonPath("$.rol").value("SECRETARIA"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void debeNormalizarCorreoEnLogin() throws Exception {
        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "  SECRETARIA.INGENIERIA@UAO.EDU.CO  ",
                                  "password": "ClaveSegura1!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("secretaria.ingenieria@uao.edu.co"));
    }

    @Test
    void debePermitirLoginConRequestParamsPorCompatibilidad() throws Exception {
        mockMvc.perform(post("/api/usuarios/login")
                        .param("correo", "secretaria.ingenieria@uao.edu.co")
                        .param("password", "ClaveSegura1!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("SECRETARIA"));
    }

    @Test
    void debeResponderUnauthorizedCuandoCredencialesSonInvalidas() throws Exception {
        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "correo": "secretaria.ingenieria@uao.edu.co",
                                  "password": "incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Correo o contrasena incorrectos"));
    }
}
