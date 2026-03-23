package com.apiweb.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.apiweb.backend.Model.EstadoReserva;
import com.apiweb.backend.Model.ReservaModel;
import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Repository.AuditoriaRepository;
import com.apiweb.backend.Repository.RecursoSalaRepository;
import com.apiweb.backend.Repository.RecursoTecnologicoRepository;
import com.apiweb.backend.Repository.ReservaRepository;
import com.apiweb.backend.Repository.SalaRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SalaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private RecursoSalaRepository recursoSalaRepository;

    @Autowired
    private RecursoTecnologicoRepository recursoTecnologicoRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private SalaModel salaIngenieria;
    private SalaModel salaOtraFacultad;

    @BeforeEach
    void setUp() {
        recursoSalaRepository.deleteAll();
        recursoTecnologicoRepository.deleteAll();
        reservaRepository.deleteAll();
        auditoriaRepository.deleteAll();
        salaRepository.deleteAll();

        salaIngenieria = salaRepository.save(new SalaModel(null, "Sala Magna", "Bloque A", 20, 10, true));
        salaRepository.save(new SalaModel(null, "Sala Norte", "Bloque B", 12, 10, true));
        salaOtraFacultad = salaRepository.save(new SalaModel(null, "Sala Externa", "Bloque C", 15, 99, true));
    }

    @Test
    void debeEditarSalaCorrectamente() throws Exception {
        mockMvc.perform(put("/api/salas/{idSala}", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Sala Innovacion",
                                  "ubicacion": "Bloque A - Piso 2",
                                  "capacidad": 30
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sala Innovacion"))
                .andExpect(jsonPath("$.capacidad").value(30));
    }

    @Test
    void debeRechazarNombreDuplicadoEnLaMismaFacultad() throws Exception {
        mockMvc.perform(put("/api/salas/{idSala}", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Sala Norte",
                                  "ubicacion": "Bloque A",
                                  "capacidad": 30
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una sala con ese nombre en la facultad"));
    }

    @Test
    void debeCancelarReservaConfirmadaAlDeshabilitarSala() throws Exception {
        reservaRepository.save(new ReservaModel(
                null,
                salaIngenieria,
                2,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                EstadoReserva.CONFIRMADA));

        mockMvc.perform(patch("/api/salas/{idSala}/estado", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "habilitada": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitada").value(false))
                .andExpect(jsonPath("$.reservaCanceladaId").isNumber());
    }

    @Test
    void debeAgregarRecursoYActualizarCantidadSiYaExiste() throws Exception {
        mockMvc.perform(post("/api/salas/{idSala}/recursos", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoRecurso": "TV-01",
                                  "nombreRecurso": "Televisor",
                                  "cantidad": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(2));

        mockMvc.perform(post("/api/salas/{idSala}/recursos", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoRecurso": "TV-01",
                                  "nombreRecurso": "Televisor Smart",
                                  "cantidad": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidad").value(3))
                .andExpect(jsonPath("$.mensaje").value("El recurso ya estaba asignado y se actualizo la cantidad"));

        mockMvc.perform(get("/api/salas/{idSala}", salaIngenieria.getIdSala())
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recursos[0].codigoRecurso").value("TV-01"))
                .andExpect(jsonPath("$.recursos[0].cantidad").value(3));
    }

    @Test
    void debeImpedirGestionarSalaDeOtraFacultad() throws Exception {
        mockMvc.perform(get("/api/salas/{idSala}", salaOtraFacultad.getIdSala())
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isForbidden());
    }
}
