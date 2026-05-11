package com.apiweb.backend.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apiweb.backend.DTO.CancelarReservaResponse;
import com.apiweb.backend.DTO.CrearReservaRequest;
import com.apiweb.backend.DTO.CrearReservaResponse;
import com.apiweb.backend.DTO.DisponibilidadSalaResponse;
import com.apiweb.backend.DTO.HorarioDisponibleResponse;
import com.apiweb.backend.Exception.BusinessException;
import com.apiweb.backend.Exception.RecursoNoEncontradoException;
import com.apiweb.backend.Model.AuditoriaModel;
import com.apiweb.backend.Model.EstadoReserva;
import com.apiweb.backend.Model.ReservaModel;
import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.AuditoriaRepository;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Repository.ReservaRepository;
import com.apiweb.backend.Repository.SalaRepository;

@Service
public class ReservaService {

    private static final LocalTime HORA_INICIO_PERMITIDA = LocalTime.of(7, 0);
    private static final LocalTime HORA_FIN_PERMITIDA = LocalTime.of(21, 30);
    private static final List<EstadoReserva> ESTADOS_OCUPACION = List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE);

    private final ReservaRepository reservaRepository;
    private final SalaRepository salaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    public ReservaService(ReservaRepository reservaRepository,
                          SalaRepository salaRepository,
                          IUsuarioRepository usuarioRepository,
                          AuditoriaRepository auditoriaRepository) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadSalaResponse> consultarDisponibilidad(LocalDate fecha, Integer facultadId, String usuarioId) {
        resolverUsuarioId(usuarioId);
        List<SalaModel> salas = salaRepository.findByFacultadIdAndHabilitadaTrueOrderByNombreAsc(facultadId);
        return salas.stream()
                .map(sala -> construirDisponibilidad(sala, fecha))
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadSalaResponse consultarDisponibilidadSala(Integer idSala, LocalDate fecha, Integer facultadId, String usuarioId) {
        resolverUsuarioId(usuarioId);
        SalaModel sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sala no existe"));

        if (!sala.getFacultadId().equals(facultadId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "La sala no pertenece a su facultad");
        }
        if (!sala.isHabilitada()) {
            throw new BusinessException(HttpStatus.CONFLICT, "La sala no esta habilitada para reservas");
        }

        return construirDisponibilidad(sala, fecha);
    }

    @Transactional
    public CrearReservaResponse crearReserva(CrearReservaRequest request, String usuarioId, Integer facultadId, String rolUsuario) {
        if (request.getIdSala() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El ID de la sala es obligatorio");
        }

        LocalDate fecha = request.resolverFecha();
        LocalTime horaInicio = request.resolverHoraInicio();
        LocalTime horaFin = request.resolverHoraFin();

        if (fecha == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha de la reserva es obligatoria");
        }
        if (horaInicio == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La hora de inicio es obligatoria");
        }
        if (horaFin == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La hora de fin es obligatoria");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha de la reserva debe ser hoy o una fecha futura");
        }
        if (request.getFechaHoraInicio() != null && request.getFechaHoraFin() != null
                && !request.getFechaHoraInicio().toLocalDate().equals(request.getFechaHoraFin().toLocalDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La reserva debe iniciar y finalizar el mismo dia");
        }

        Integer idUsuario = resolverUsuarioId(usuarioId);
        UsuarioModel usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (!usuario.getIdFacultad().equals(facultadId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "El usuario no pertenece a la facultad indicada");
        }

        SalaModel sala = salaRepository.findById(request.getIdSala())
                .orElseThrow(() -> new RecursoNoEncontradoException("La sala no existe"));

        if (!sala.getFacultadId().equals(facultadId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "La sala no pertenece a su facultad");
        }

        if (!sala.isHabilitada()) {
            throw new BusinessException(HttpStatus.CONFLICT, "La sala no estÃ¡ habilitada para reservas");
        }

        validarHorarioPermitido(horaInicio, horaFin);

        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior a la hora de inicio");
        }

        List<ReservaModel> solapadas = reservaRepository.findReservasSolapadas(
                request.getIdSala(),
                fecha,
                ESTADOS_OCUPACION,
                horaInicio,
                horaFin);

        if (!solapadas.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "La sala ya estÃ¡ reservada en el horario solicitado");
        }

        ReservaModel nuevaReserva = new ReservaModel();
        nuevaReserva.setSala(sala);
        nuevaReserva.setIdUsuario(idUsuario);
        nuevaReserva.setFecha(fecha);
        nuevaReserva.setHoraInicio(horaInicio);
        nuevaReserva.setHoraFin(horaFin);
        nuevaReserva.setEstado(EstadoReserva.CONFIRMADA);

        ReservaModel guardada = reservaRepository.save(nuevaReserva);

        return new CrearReservaResponse(
                guardada.getIdReserva(),
                guardada.getSala().getIdSala(),
                guardada.getSala().getNombre(),
                guardada.getIdUsuario(),
                guardada.getFecha(),
                guardada.getHoraInicio(),
                guardada.getHoraFin(),
                mapearEstadoParaFrontend(guardada.getEstado()),
                "Reserva creada exitosamente"
        );
    }

    @Transactional(readOnly = true)
    public List<CrearReservaResponse> listarReservasUsuario(String usuarioId) {
        Integer idUsuario = resolverUsuarioId(usuarioId);
        List<ReservaModel> reservas = reservaRepository.findByIdUsuarioAndEstadoInOrderByFechaDescHoraInicioDesc(
                idUsuario, List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE));

        return reservas.stream()
                .map(this::toCrearReservaResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CrearReservaResponse> listarReservasFacultad(LocalDate fecha, Integer facultadId, String rolUsuario) {
        if (!"SECRETARIA".equalsIgnoreCase(rolUsuario)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Solo una secretaria puede listar las reservas de la facultad");
        }

        List<ReservaModel> reservas = reservaRepository.findBySalaFacultadIdAndFechaAndEstadoInOrderByHoraInicioAsc(
                facultadId, fecha, ESTADOS_OCUPACION);

        return reservas.stream()
                .map(this::toCrearReservaResponse)
                .toList();
    }

    @Transactional
    public CancelarReservaResponse cancelarReserva(Integer idReserva, String usuarioId, String rolUsuario) {
        Integer idUsuario = resolverUsuarioId(usuarioId);

        ReservaModel reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe"));

        boolean esSecretaria = "SECRETARIA".equalsIgnoreCase(rolUsuario);
        boolean esDueno = reserva.getIdUsuario().equals(idUsuario);

        if (!esSecretaria && !esDueno) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "No tiene permiso para cancelar esta reserva");
        }

        if (esSecretaria) {
            UsuarioModel secretaria = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Secretaria no encontrada"));
            if (!secretaria.getIdFacultad().equals(reserva.getSala().getFacultadId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN, "La secretaria no pertenece a la facultad de la sala");
            }
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA || reserva.getEstado() == EstadoReserva.FINALIZADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "La reserva ya no estÃ¡ activa");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);

        AuditoriaModel auditoria = new AuditoriaModel();
        auditoria.setEntidad("reserva");
        auditoria.setEntidadId(reserva.getIdReserva().longValue());
        auditoria.setAccion("CANCELACION");
        auditoria.setUsuarioActor(idUsuario);
        auditoria.setCorreoActor(usuarioId);
        auditoria.setFechaHora(java.time.LocalDateTime.now());
        auditoria.setDatosAnteriores("{\"estado\":\"" + EstadoReserva.CONFIRMADA + "\"}");
        auditoria.setDatosNuevos("{\"estado\":\"" + EstadoReserva.CANCELADA + "\"}");
        auditoria.setObservacion("Cancelada por " + (esSecretaria ? "secretaria" : "usuario"));
        auditoriaRepository.save(auditoria);

        String canceladoPor = esSecretaria ? "SECRETARIA" : "USUARIO";

        return new CancelarReservaResponse(
                reserva.getIdReserva(),
                reserva.getSala().getIdSala(),
                reserva.getSala().getNombre(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin(),
                mapearEstadoParaFrontend(reserva.getEstado()),
                "Reserva cancelada exitosamente",
                canceladoPor
        );
    }

    private Integer resolverUsuarioId(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El encabezado X-Usuario-Id es obligatorio");
        }
        String valorNormalizado = usuarioId.trim();
        try {
            Integer id = Integer.valueOf(valorNormalizado);
            if (!usuarioRepository.existsById(id)) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no registrado en el sistema");
            }
            return id;
        } catch (NumberFormatException e) {
            UsuarioModel usuario = usuarioRepository.findByCorreo(valorNormalizado.toLowerCase(Locale.ROOT));
            if (usuario != null) {
                return usuario.getIdUsuario();
            }
            throw new BusinessException(HttpStatus.BAD_REQUEST, "X-Usuario-Id debe ser un nÃºmero vÃ¡lido");
        }
    }

    private void validarHorarioPermitido(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio.isBefore(HORA_INICIO_PERMITIDA) || horaFin.isAfter(HORA_FIN_PERMITIDA)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El horario de reserva debe estar entre " + HORA_INICIO_PERMITIDA + " y " + HORA_FIN_PERMITIDA);
        }
    }

    private DisponibilidadSalaResponse construirDisponibilidad(SalaModel sala, LocalDate fecha) {
        List<ReservaModel> reservas = reservaRepository.findBySalaIdSalaAndFechaAndEstadoInOrderByHoraInicioAsc(
                sala.getIdSala(), fecha, ESTADOS_OCUPACION);

        List<HorarioDisponibleResponse> horariosDisponibles = construirFranjasDisponibles(reservas);
        List<HorarioDisponibleResponse> horariosOcupados = horariosDisponibles.stream()
                .filter(horario -> Boolean.FALSE.equals(horario.getDisponible()))
                .toList();

        return new DisponibilidadSalaResponse(
                sala.getIdSala(),
                sala.getNombre(),
                fecha,
                horariosDisponibles,
                HORA_INICIO_PERMITIDA + " - " + HORA_FIN_PERMITIDA,
                horariosOcupados
        );
    }

    private CrearReservaResponse toCrearReservaResponse(ReservaModel reserva) {
        return new CrearReservaResponse(
                reserva.getIdReserva(),
                reserva.getSala().getIdSala(),
                reserva.getSala().getNombre(),
                reserva.getIdUsuario(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin(),
                mapearEstadoParaFrontend(reserva.getEstado()),
                null
        );
    }

    private List<HorarioDisponibleResponse> construirFranjasDisponibles(List<ReservaModel> reservas) {
        List<HorarioDisponibleResponse> horarios = new ArrayList<>();
        for (LocalTime inicio = HORA_INICIO_PERMITIDA; inicio.isBefore(HORA_FIN_PERMITIDA); inicio = inicio.plusMinutes(30)) {
            LocalTime inicioFranja = inicio;
            LocalTime finFranja = inicioFranja.plusMinutes(30);
            ReservaModel reservaSolapada = reservas.stream()
                    .filter(reserva -> reserva.getHoraInicio().isBefore(finFranja)
                            && reserva.getHoraFin().isAfter(inicioFranja))
                    .findFirst()
                    .orElse(null);

            boolean disponible = reservaSolapada == null;
            horarios.add(new HorarioDisponibleResponse(
                    inicioFranja,
                    finFranja,
                    disponible ? "DISPONIBLE" : mapearEstadoParaFrontend(reservaSolapada.getEstado()),
                    disponible));
        }
        return horarios;
    }

    private String mapearEstadoParaFrontend(EstadoReserva estado) {
        return switch (estado) {
            case CANCELADA -> "CANCELADA";
            case FINALIZADA -> "FINALIZADA";
            default -> "ACTIVA";
        };
    }
}
