package com.apiweb.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.apiweb.backend.Model.EstadoReserva;
import com.apiweb.backend.Model.ReservaModel;
import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.AuditoriaRepository;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Repository.ReservaRepository;
import com.apiweb.backend.Repository.SalaRepository;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ReservaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SalaRepository salaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    private SalaModel salaIngenieria;
    private SalaModel salaDeshabilitada;
    private LocalDate fechaManana;
    private String secretariaId;
    private String docenteId;
    private Integer otroUsuarioId;
    private String docenteCorreo;

    @BeforeEach
    void setUp() {
        reservaRepository.deleteAll();
        auditoriaRepository.deleteAll();
        salaRepository.deleteAll();
        usuarioRepository.deleteAll();

        UsuarioModel secretaria = usuarioRepository.save(
                new UsuarioModel(null, "Secretaria", "secretaria.test@uao.edu.co", "ClaveSegura1!", "SECRETARIA", 10));
        UsuarioModel docente = usuarioRepository.save(
                new UsuarioModel(null, "Docente", "docente.test@uao.edu.co", "ClaveSegura1!", "DOCENTE", 10));
        UsuarioModel otroUsuario = usuarioRepository.save(
                new UsuarioModel(null, "Docente Dos", "docente2.test@uao.edu.co", "ClaveSegura1!", "DOCENTE", 10));

        secretariaId = String.valueOf(secretaria.getIdUsuario());
        docenteId = String.valueOf(docente.getIdUsuario());
        otroUsuarioId = otroUsuario.getIdUsuario();
        docenteCorreo = docente.getCorreo();

        salaIngenieria = salaRepository.save(new SalaModel(null, "Sala Magna", "Bloque A", 20, 10, true));
        salaDeshabilitada = salaRepository.save(new SalaModel(null, "Sala Sur", "Bloque D", 18, 10, false));
        fechaManana = LocalDate.now().plusDays(1);
    }

    @Test
    void debeCrearReservaCorrectamente() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .header("X-Usuario-Id", docenteId)
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "DOCENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idSala": %d,
                                  "fecha": "%s",
                                  "horaInicio": "10:00:00",
                                  "horaFin": "11:30:00"
                                }
                                """.formatted(salaIngenieria.getIdSala(), fechaManana)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.fechaHoraInicio").value(fechaManana + "T10:00"))
                .andExpect(jsonPath("$.mensaje").value("Reserva creada exitosamente"));
    }

    @Test
    void debeCrearReservaConPayloadDelFrontend() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .header("X-Usuario-Id", docenteCorreo)
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "DOCENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idSala": %d,
                                  "fechaHoraInicio": "%sT15:00:00",
                                  "fechaHoraFin": "%sT16:30:00"
                                }
                                """.formatted(salaIngenieria.getIdSala(), fechaManana, fechaManana)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ACTIVA"))
                .andExpect(jsonPath("$.fechaHoraInicio").value(fechaManana + "T15:00"))
                .andExpect(jsonPath("$.fechaHoraFin").value(fechaManana + "T16:30"));
    }

    @Test
    void debeRechazarReservaFueraDeHorario() throws Exception {
        mockMvc.perform(post("/api/reservas")
                        .header("X-Usuario-Id", docenteId)
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "DOCENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idSala": %d,
                                  "fecha": "%s",
                                  "horaInicio": "06:00:00",
                                  "horaFin": "08:00:00"
                                }
                                """.formatted(salaIngenieria.getIdSala(), fechaManana)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El horario de reserva debe estar entre 07:00 y 21:30"));
    }

    @Test
    void debeRechazarReservaConConflictoHorario() throws Exception {
        reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana,
                LocalTime.of(10, 0), LocalTime.of(11, 30), EstadoReserva.CONFIRMADA));

        mockMvc.perform(post("/api/reservas")
                        .header("X-Usuario-Id", docenteId)
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "DOCENTE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "idSala": %d,
                                  "fecha": "%s",
                                  "horaInicio": "11:00:00",
                                  "horaFin": "12:30:00"
                                }
                                """.formatted(salaIngenieria.getIdSala(), fechaManana)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("La sala ya est")));
    }

    @Test
    void debeCancelarReservaCorrectamente() throws Exception {
        ReservaModel reserva = reservaRepository.save(new ReservaModel(null, salaIngenieria, Integer.valueOf(docenteId), fechaManana,
                LocalTime.of(14, 0), LocalTime.of(15, 30), EstadoReserva.CONFIRMADA));

        mockMvc.perform(delete("/api/reservas/{idReserva}", reserva.getIdReserva())
                        .header("X-Usuario-Id", docenteId)
                        .header("X-Rol", "DOCENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelada").value(true))
                .andExpect(jsonPath("$.mensaje").value("Reserva cancelada exitosamente"))
                .andExpect(jsonPath("$.canceladoPor").value("USUARIO"));
    }

    @Test
    void secretariaPuedeCancelarReservaDeOtroUsuario() throws Exception {
        ReservaModel reserva = reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana,
                LocalTime.of(14, 0), LocalTime.of(15, 30), EstadoReserva.CONFIRMADA));

        mockMvc.perform(delete("/api/reservas/{idReserva}", reserva.getIdReserva())
                        .header("X-Usuario-Id", secretariaId)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.canceladoPor").value("SECRETARIA"));
    }

    @Test
    void usuarioNoPuedeCancelarReservaDeOtro() throws Exception {
        ReservaModel reserva = reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana,
                LocalTime.of(14, 0), LocalTime.of(15, 30), EstadoReserva.CONFIRMADA));

        mockMvc.perform(delete("/api/reservas/{idReserva}", reserva.getIdReserva())
                        .header("X-Usuario-Id", docenteId)
                        .header("X-Rol", "DOCENTE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tiene permiso para cancelar esta reserva"));
    }

    @Test
    void debeRechazarCancelacionDeReservaInexistente() throws Exception {
        mockMvc.perform(delete("/api/reservas/{idReserva}", 9999)
                        .header("X-Usuario-Id", docenteId)
                        .header("X-Rol", "DOCENTE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La reserva no existe"));
    }

    @Test
    void debeConsultarDisponibilidadCorrectamente() throws Exception {
        reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana,
                LocalTime.of(10, 0), LocalTime.of(11, 0), EstadoReserva.CONFIRMADA));

        mockMvc.perform(get("/api/reservas/disponibilidad")
                        .param("fecha", fechaManana.toString())
                        .header("X-Facultad-Id", 10)
                        .header("X-Usuario-Id", docenteCorreo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSala").value(salaIngenieria.getIdSala()))
                .andExpect(jsonPath("$[0].horariosDisponibles[0].disponible").value(true))
                .andExpect(jsonPath("$[0].horariosDisponibles[6].disponible").value(false))
                .andExpect(jsonPath("$[0].franjaHorariaPermitida").value("07:00 - 21:30"));
    }

    @Test
    void debeExcluirSalasDeshabilitadasDeLaDisponibilidad() throws Exception {
        mockMvc.perform(get("/api/reservas/disponibilidad")
                        .param("fecha", fechaManana.toString())
                        .header("X-Facultad-Id", 10)
                        .header("X-Usuario-Id", docenteCorreo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idSala").value(salaIngenieria.getIdSala()))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void debeRechazarConsultaDeDisponibilidadParaSalaDeshabilitada() throws Exception {
        mockMvc.perform(get("/api/reservas/disponibilidad/sala/{idSala}", salaDeshabilitada.getIdSala())
                        .param("fecha", fechaManana.toString())
                        .header("X-Facultad-Id", 10)
                        .header("X-Usuario-Id", docenteCorreo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("La sala no esta habilitada para reservas"));
    }

    @Test
    void secretariaDebeConsultarTodoElHistorialDeLaFacultad() throws Exception {
        SalaModel salaAuditorio = salaRepository.save(new SalaModel(null, "Auditorio Central", "Bloque B", 80, 10, true));

        reservaRepository.save(new ReservaModel(null, salaIngenieria, Integer.valueOf(docenteId), fechaManana.minusDays(2),
                LocalTime.of(8, 0), LocalTime.of(9, 0), EstadoReserva.FINALIZADA));
        reservaRepository.save(new ReservaModel(null, salaAuditorio, otroUsuarioId, fechaManana.minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), EstadoReserva.CANCELADA));
        reservaRepository.save(new ReservaModel(null, salaIngenieria, Integer.valueOf(docenteId), fechaManana,
                LocalTime.of(14, 0), LocalTime.of(15, 0), EstadoReserva.CONFIRMADA));

        mockMvc.perform(get("/api/reservas/historial/facultad")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Historial de reservas consultado exitosamente"))
                .andExpect(jsonPath("$.historial.length()").value(3))
                .andExpect(jsonPath("$.historial[0].estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.historial[1].estado").value("CANCELADA"))
                .andExpect(jsonPath("$.historial[2].estado").value("FINALIZADA"));
    }

    @Test
    void secretariaDebeRecibirMensajeCuandoHistorialEstaVacio() throws Exception {
        mockMvc.perform(get("/api/reservas/historial/facultad")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("No existe un registro de reservas para los criterios seleccionados"))
                .andExpect(jsonPath("$.historial").isEmpty());
    }

    @Test
    void secretariaDebeFiltrarHistorialPorProfesorOSala() throws Exception {
        SalaModel salaAuditorio = salaRepository.save(new SalaModel(null, "Auditorio Central", "Bloque B", 80, 10, true));

        reservaRepository.save(new ReservaModel(null, salaIngenieria, Integer.valueOf(docenteId), fechaManana.minusDays(1),
                LocalTime.of(8, 0), LocalTime.of(9, 0), EstadoReserva.FINALIZADA));
        reservaRepository.save(new ReservaModel(null, salaAuditorio, otroUsuarioId, fechaManana.minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), EstadoReserva.CANCELADA));

        mockMvc.perform(get("/api/reservas/historial/facultad")
                        .param("profesor", "docente2")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial.length()").value(1))
                .andExpect(jsonPath("$.historial[0].correoProfesor").value("docente2.test@uao.edu.co"))
                .andExpect(jsonPath("$.historial[0].nombreSala").value("Auditorio Central"));

        mockMvc.perform(get("/api/reservas/historial/facultad")
                        .param("sala", "Magna")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial.length()").value(1))
                .andExpect(jsonPath("$.historial[0].nombreProfesor").value("Docente"))
                .andExpect(jsonPath("$.historial[0].nombreSala").value("Sala Magna"));
    }

    @Test
    void secretariaDebeFiltrarHistorialPorDiaORangoDeDias() throws Exception {
        SalaModel salaAuditorio = salaRepository.save(new SalaModel(null, "Auditorio Central", "Bloque B", 80, 10, true));
        LocalDate ayer = fechaManana.minusDays(1);
        LocalDate pasadoManana = fechaManana.plusDays(1);

        reservaRepository.save(new ReservaModel(null, salaIngenieria, Integer.valueOf(docenteId), ayer,
                LocalTime.of(8, 0), LocalTime.of(9, 0), EstadoReserva.FINALIZADA));
        reservaRepository.save(new ReservaModel(null, salaAuditorio, otroUsuarioId, fechaManana,
                LocalTime.of(10, 0), LocalTime.of(11, 0), EstadoReserva.CANCELADA));
        reservaRepository.save(new ReservaModel(null, salaIngenieria, Integer.valueOf(docenteId), pasadoManana,
                LocalTime.of(14, 0), LocalTime.of(15, 0), EstadoReserva.CONFIRMADA));

        mockMvc.perform(get("/api/reservas/historial/facultad")
                        .param("fecha", fechaManana.toString())
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial.length()").value(1))
                .andExpect(jsonPath("$.historial[0].fecha").value(fechaManana.toString()));

        mockMvc.perform(get("/api/reservas/historial/facultad")
                        .param("fechaInicio", ayer.toString())
                        .param("fechaFin", fechaManana.toString())
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.historial.length()").value(2))
                .andExpect(jsonPath("$.historial[0].fecha").value(fechaManana.toString()))
                .andExpect(jsonPath("$.historial[1].fecha").value(ayer.toString()));
    }

    @Test
    void secretariaDebeGenerarListadoDeReportePorUsuario() throws Exception {
        reservaRepository.save(new ReservaModel(null, salaIngenieria, Integer.valueOf(docenteId), fechaManana.minusDays(2),
                LocalTime.of(8, 0), LocalTime.of(9, 0), EstadoReserva.FINALIZADA));
        reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana.minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), EstadoReserva.CANCELADA));
        reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana,
                LocalTime.of(14, 0), LocalTime.of(15, 0), EstadoReserva.CONFIRMADA));

        mockMvc.perform(get("/api/reservas/reporte/usuarios")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Reporte de uso por usuario generado exitosamente"))
                .andExpect(jsonPath("$.totalUsuarios").value(3))
                .andExpect(jsonPath("$.totalReservas").value(3))
                .andExpect(jsonPath("$.usuarios[0].nombreUsuario").value("Docente"))
                .andExpect(jsonPath("$.usuarios[0].totalReservas").value(1))
                .andExpect(jsonPath("$.usuarios[1].nombreUsuario").value("Docente Dos"))
                .andExpect(jsonPath("$.usuarios[1].totalReservas").value(2));
    }

    @Test
    void secretariaDebeGenerarDetalleDeReportePorUsuario() throws Exception {
        reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana.minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(11, 0), EstadoReserva.CANCELADA));
        reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana,
                LocalTime.of(14, 0), LocalTime.of(15, 0), EstadoReserva.CONFIRMADA));

        mockMvc.perform(get("/api/reservas/reporte/usuarios/{idUsuario}", otroUsuarioId)
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensaje").value("Reporte de uso del usuario generado exitosamente"))
                .andExpect(jsonPath("$.nombreUsuario").value("Docente Dos"))
                .andExpect(jsonPath("$.totalReservas").value(2))
                .andExpect(jsonPath("$.reservasRealizadas").value(1))
                .andExpect(jsonPath("$.reservasCanceladas").value(1))
                .andExpect(jsonPath("$.reservas.length()").value(2))
                .andExpect(jsonPath("$.reservas[0].nombreSala").value("Sala Magna"))
                .andExpect(jsonPath("$.reservas[0].fecha").value(fechaManana.toString()));
    }

    @Test
    void secretariaDebeFiltrarReportePorUsuarioConBusqueda() throws Exception {
        reservaRepository.save(new ReservaModel(null, salaIngenieria, otroUsuarioId, fechaManana,
                LocalTime.of(10, 0), LocalTime.of(11, 0), EstadoReserva.CONFIRMADA));

        mockMvc.perform(get("/api/reservas/reporte/usuarios")
                        .param("busqueda", "docente2")
                        .header("X-Facultad-Id", 10)
                        .header("X-Rol", "SECRETARIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsuarios").value(1))
                .andExpect(jsonPath("$.totalReservas").value(1))
                .andExpect(jsonPath("$.usuarios[0].nombreUsuario").value("Docente Dos"))
                .andExpect(jsonPath("$.usuarios[0].correoUsuario").value("docente2.test@uao.edu.co"));
    }
}
