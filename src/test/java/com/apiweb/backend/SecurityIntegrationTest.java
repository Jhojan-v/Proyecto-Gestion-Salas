package com.apiweb.backend;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

import com.apiweb.backend.Model.EstadoReserva;
import com.apiweb.backend.Model.ReservaModel;
import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Repository.ReservaRepository;
import com.apiweb.backend.Repository.SalaRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private Environment environment;

    private UsuarioModel secretaria;
    private UsuarioModel docente;
    private SalaModel salaIngenieria;

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        salaRepository.deleteAll();
        usuarioRepository.deleteAll();

        secretaria = usuarioRepository.save(new UsuarioModel(
                null,
                "Secretaria Ingenieria",
                "secretaria.ingenieria@uao.edu.co",
                "ClaveSegura1!",
                "SECRETARIA",
                10));

        docente = usuarioRepository.save(new UsuarioModel(
                null,
                "Docente Ingenieria",
                "docente.ingenieria@uao.edu.co",
                "ClaveSegura1!",
                "DOCENTE",
                10));

        salaIngenieria = salaRepository.save(new SalaModel(null, "Sala Magna", "Bloque A", 20, 10, true));
    }

    @Test
    void debeExponerHealthSinAutenticacion() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void debeResponderPreflightDesdeVite() throws Exception {
        mockMvc.perform(options("/api/usuarios/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void debeResponderJsonCuandoApiNoEstaAutenticada() throws Exception {
        mockMvc.perform(get("/api/salas")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticacion requerida"));
    }

    @Test
    void debePermitirAutenticarConEncabezadoUsuarioSinSesion() throws Exception {
        mockMvc.perform(get("/api/salas")
                        .header("X-Usuario-Id", secretaria.getIdUsuario())
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Sala Magna"));
    }

    @Test
    void debeDesactivarCookieSecurePorDefectoEnDesarrollo() {
        assertFalse(Boolean.parseBoolean(environment.getProperty("server.servlet.session.cookie.secure")));
    }

    @Test
    void docenteDebePoderVerSusReservasSinSesionFormulario() throws Exception {
        reservaRepository.save(new ReservaModel(
                null,
                salaIngenieria,
                docente.getIdUsuario(),
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                EstadoReserva.CONFIRMADA));

        mockMvc.perform(get("/api/reservas/mis-reservas")
                        .header("X-Usuario-Id", docente.getIdUsuario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSala").value(salaIngenieria.getIdSala()))
                .andExpect(jsonPath("$[0].estado").value("ACTIVA"));
    }

    @Test
    void docenteDebePoderCancelarSuReservaSinSesionFormulario() throws Exception {
        ReservaModel reserva = reservaRepository.save(new ReservaModel(
                null,
                salaIngenieria,
                docente.getIdUsuario(),
                LocalDate.now().plusDays(1),
                LocalTime.of(14, 0),
                LocalTime.of(15, 0),
                EstadoReserva.CONFIRMADA));

        mockMvc.perform(delete("/api/reservas/{idReserva}", reserva.getIdReserva())
                        .header("X-Usuario-Id", docente.getIdUsuario())
                        .header("X-Rol", "DOCENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.canceladoPor").value("USUARIO"));
    }
}
