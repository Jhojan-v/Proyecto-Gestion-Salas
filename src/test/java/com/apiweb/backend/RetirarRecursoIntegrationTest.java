package com.apiweb.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.apiweb.backend.Model.RecursoSalaModel;
import com.apiweb.backend.Model.RecursoTecnologicoModel;
import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Repository.AuditoriaRepository;
import com.apiweb.backend.Repository.RecursoSalaRepository;
import com.apiweb.backend.Repository.RecursoTecnologicoRepository;
import com.apiweb.backend.Repository.ReservaRepository;
import com.apiweb.backend.Repository.SalaRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class RetirarRecursoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private RecursoSalaRepository recursoSalaRepository;

    @Autowired
    private RecursoTecnologicoRepository recursoTecnologicoRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    private SalaModel salaIngenieria;
    private RecursoSalaModel recursoAsignado;

    @BeforeEach
    void setUp() {
        recursoSalaRepository.deleteAll();
        recursoTecnologicoRepository.deleteAll();
        reservaRepository.deleteAll();
        auditoriaRepository.deleteAll();
        salaRepository.deleteAll();

        salaIngenieria = salaRepository.save(new SalaModel(null, "Sala Magna", "Bloque A", 20, 10, true));

        RecursoTecnologicoModel recurso = recursoTecnologicoRepository.save(
                new RecursoTecnologicoModel(null, "TV-01", "Televisor", "Televisor 4K", true));

        recursoAsignado = recursoSalaRepository.save(
                new RecursoSalaModel(null, salaIngenieria, recurso, 2));
    }

    @Test
    void debeRetirarRecursoCorrectamente() throws Exception {
        mockMvc.perform(delete("/api/salas/{idSala}/recursos", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoRecurso": "TV-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoRecurso").value("TV-01"))
                .andExpect(jsonPath("$.mensaje").value("El recurso fue retirado correctamente de la sala"));
    }

    @Test
    void debeRetirarRecursoPorIdComoLoHaceElFrontend() throws Exception {
        mockMvc.perform(delete("/api/salas/{idSala}/recursos/{idRecursoSala}",
                        salaIngenieria.getIdSala(), recursoAsignado.getIdRecursoSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idRecursoSala").value(recursoAsignado.getIdRecursoSala()))
                .andExpect(jsonPath("$.codigoRecurso").value("TV-01"));
    }

    @Test
    void debeRechazarRetiroDeRecursoInexistente() throws Exception {
        mockMvc.perform(delete("/api/salas/{idSala}/recursos", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoRecurso": "RECURSO-INEXISTENTE"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El recurso con codigo 'RECURSO-INEXISTENTE' no esta asociado a esta sala"));
    }

    @Test
    void debeImpedirRetiroEnSalaDeshabilitada() throws Exception {
        salaIngenieria.setHabilitada(false);
        salaRepository.save(salaIngenieria);

        mockMvc.perform(delete("/api/salas/{idSala}/recursos", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoRecurso": "TV-01"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La sala no esta disponible para retirar recursos porque se encuentra deshabilitada"));
    }

    @Test
    void debeImpedirRetiroSinAutenticacionAdecuada() throws Exception {
        mockMvc.perform(delete("/api/salas/{idSala}/recursos", salaIngenieria.getIdSala())
                        .header("X-Usuario-Id", "1")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "DOCENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoRecurso": "TV-01"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
