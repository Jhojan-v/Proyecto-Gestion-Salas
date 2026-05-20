package com.apiweb.backend.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apiweb.backend.DTO.CancelarReservaResponse;
import com.apiweb.backend.DTO.CrearReservaRequest;
import com.apiweb.backend.DTO.CrearReservaResponse;
import com.apiweb.backend.DTO.DisponibilidadSalaResponse;
import com.apiweb.backend.DTO.HistorialReservaResponse;
import com.apiweb.backend.DTO.HistorialReservasFacultadResponse;
import com.apiweb.backend.DTO.HorarioDisponibleResponse;
import com.apiweb.backend.DTO.ReporteHorasResponse;
import com.apiweb.backend.DTO.ReporteHorasSalaResponse;
import com.apiweb.backend.DTO.ReporteUsuarioDetalleResponse;
import com.apiweb.backend.DTO.ReporteUsuarioResumenResponse;
import com.apiweb.backend.DTO.ReporteUsuariosResponse;
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
    private final NotificacionService notificacionService;

    public ReservaService(ReservaRepository reservaRepository,
                          SalaRepository salaRepository,
                          IUsuarioRepository usuarioRepository,
                          AuditoriaRepository auditoriaRepository,
                          NotificacionService notificacionService) {
        this.reservaRepository = reservaRepository;
        this.salaRepository = salaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.notificacionService = notificacionService;
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

        notificarCambioReserva(
                guardada,
                idUsuario,
                "RESERVA_CREADA",
                "Nueva reserva en " + sala.getNombre(),
                "Se registro la reserva del " + guardada.getFecha()
                        + " de " + guardada.getHoraInicio() + " a " + guardada.getHoraFin()
                        + " en la sala " + sala.getNombre() + ".",
                "SECRETARIA".equalsIgnoreCase(rolUsuario));

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
    public HistorialReservasFacultadResponse consultarHistorialDocente(String usuarioId) {
        Integer idUsuario = resolverUsuarioId(usuarioId);
        List<ReservaModel> reservas = reservaRepository.findByIdUsuarioOrderByFechaDescHoraInicioDesc(idUsuario);
        return construirRespuestaHistorial(reservas);
    }

    @Transactional(readOnly = true)
    public ReporteHorasResponse generarReporteHoras(
            Integer facultadId,
            String rolUsuario,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
        if (!"SECRETARIA".equalsIgnoreCase(rolUsuario)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Solo una secretaria puede generar el reporte de horas reservadas");
        }
        if (facultadId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La facultad de la secretaria es obligatoria");
        }
        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha inicial no puede ser posterior a la fecha final");
        }

        List<SalaModel> salasFacultad = salaRepository.findByFacultadIdOrderByNombreAsc(facultadId);

        Map<Integer, ReporteHorasSalaResponse> acumulado = new LinkedHashMap<>();
        for (SalaModel sala : salasFacultad) {
            acumulado.put(sala.getIdSala(), new ReporteHorasSalaResponse(
                    sala.getIdSala(),
                    sala.getNombre(),
                    sala.getUbicacion(),
                    sala.getCapacidad(),
                    0L,
                    0.0));
        }

        List<ReservaModel> reservas = reservaRepository.buscarReservasParaReporte(
                facultadId,
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA, EstadoReserva.PENDIENTE),
                fechaInicio,
                fechaFin);

        long totalReservas = 0L;
        double totalHoras = 0.0;
        for (ReservaModel reserva : reservas) {
            double horas = calcularHoras(reserva.getHoraInicio(), reserva.getHoraFin());
            ReporteHorasSalaResponse acumuladoSala = acumulado.computeIfAbsent(
                    reserva.getSala().getIdSala(),
                    id -> new ReporteHorasSalaResponse(
                            reserva.getSala().getIdSala(),
                            reserva.getSala().getNombre(),
                            reserva.getSala().getUbicacion(),
                            reserva.getSala().getCapacidad(),
                            0L,
                            0.0));
            acumuladoSala.setTotalReservas(acumuladoSala.getTotalReservas() + 1L);
            acumuladoSala.setTotalHoras(redondear(acumuladoSala.getTotalHoras() + horas));
            totalReservas++;
            totalHoras += horas;
        }

        List<ReporteHorasSalaResponse> salas = acumulado.values().stream()
                .sorted(Comparator.comparing(ReporteHorasSalaResponse::getNombreSala,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        String mensaje = totalReservas == 0
                ? "No existen reservas registradas para los criterios seleccionados"
                : "Reporte de horas reservadas generado exitosamente";

        return new ReporteHorasResponse(
                mensaje,
                facultadId,
                fechaInicio,
                fechaFin,
                totalReservas,
                redondear(totalHoras),
                salas);
    }

    @Transactional(readOnly = true)
    public ReporteUsuariosResponse generarReporteUsuarios(
            Integer facultadId,
            String rolUsuario,
            String busqueda) {
        validarSecretariaReporte(facultadId, rolUsuario);

        String busquedaFiltro = normalizarFiltro(busqueda);
        List<UsuarioModel> usuarios = busquedaFiltro == null
                ? usuarioRepository.findByIdFacultadOrderByNombreAsc(facultadId)
                : usuarioRepository.buscarUsuariosPorFacultad(facultadId, busquedaFiltro);

        Map<Integer, Long> reservasPorUsuario = new LinkedHashMap<>();
        for (ReservaModel reserva : reservaRepository.findBySalaFacultadIdOrderByFechaDescHoraInicioDesc(facultadId)) {
            reservasPorUsuario.merge(reserva.getIdUsuario(), 1L, Long::sum);
        }

        List<ReporteUsuarioResumenResponse> resumen = usuarios.stream()
                .map(usuario -> new ReporteUsuarioResumenResponse(
                        usuario.getIdUsuario(),
                        usuario.getNombre(),
                        usuario.getCorreo(),
                        usuario.getRol(),
                        usuario.getIdFacultad(),
                        reservasPorUsuario.getOrDefault(usuario.getIdUsuario(), 0L)))
                .toList();

        long totalReservas = resumen.stream()
                .mapToLong(ReporteUsuarioResumenResponse::getTotalReservas)
                .sum();

        String mensaje = resumen.isEmpty()
                ? "No existen usuarios para los criterios seleccionados"
                : "Reporte de uso por usuario generado exitosamente";

        return new ReporteUsuariosResponse(
                mensaje,
                facultadId,
                (long) resumen.size(),
                totalReservas,
                resumen);
    }

    @Transactional(readOnly = true)
    public ReporteUsuarioDetalleResponse generarReporteUsuarioDetalle(
            Integer idUsuario,
            Integer facultadId,
            String rolUsuario) {
        validarSecretariaReporte(facultadId, rolUsuario);

        UsuarioModel usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (!usuario.getIdFacultad().equals(facultadId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "El usuario no pertenece a la facultad de la secretaria");
        }

        List<ReservaModel> reservas = reservaRepository
                .findByIdUsuarioAndSalaFacultadIdOrderByFechaDescHoraInicioDesc(idUsuario, facultadId);
        long reservasCanceladas = reservas.stream()
                .filter(reserva -> reserva.getEstado() == EstadoReserva.CANCELADA)
                .count();
        long reservasRealizadas = reservas.size() - reservasCanceladas;
        List<HistorialReservaResponse> historial = reservas.stream()
                .map(this::toHistorialReservaResponse)
                .toList();

        String mensaje = reservas.isEmpty()
                ? "No existen reservas registradas para el usuario seleccionado"
                : "Reporte de uso del usuario generado exitosamente";

        return new ReporteUsuarioDetalleResponse(
                mensaje,
                usuario.getIdUsuario(),
                usuario.getNombre(),
                usuario.getCorreo(),
                usuario.getRol(),
                usuario.getIdFacultad(),
                (long) reservas.size(),
                reservasRealizadas,
                reservasCanceladas,
                historial);
    }

    private void validarSecretariaReporte(Integer facultadId, String rolUsuario) {
        if (!"SECRETARIA".equalsIgnoreCase(rolUsuario)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Solo una secretaria puede generar reportes de la facultad");
        }
        if (facultadId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La facultad de la secretaria es obligatoria");
        }
    }

    private double calcularHoras(LocalTime horaInicio, LocalTime horaFin) {
        if (horaInicio == null || horaFin == null) {
            return 0.0;
        }
        long minutos = java.time.Duration.between(horaInicio, horaFin).toMinutes();
        if (minutos <= 0) {
            return 0.0;
        }
        return minutos / 60.0;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
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

    @Transactional(readOnly = true)
    public HistorialReservasFacultadResponse consultarHistorialFacultad(
            Integer facultadId,
            String rolUsuario,
            String profesor,
            String sala,
            LocalDate fecha,
            LocalDate fechaInicio,
            LocalDate fechaFin) {
        if (!"SECRETARIA".equalsIgnoreCase(rolUsuario)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Solo una secretaria puede consultar el historial de la facultad");
        }

        LocalDate fechaInicioFiltro = fecha != null ? fecha : fechaInicio;
        LocalDate fechaFinFiltro = fecha != null ? fecha : fechaFin;

        if (fechaInicioFiltro != null && fechaFinFiltro != null && fechaInicioFiltro.isAfter(fechaFinFiltro)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha inicial no puede ser posterior a la fecha final");
        }

        String profesorFiltro = normalizarFiltro(profesor);
        String salaFiltro = normalizarFiltro(sala);
        boolean filtraProfesor = profesorFiltro != null;
        List<Integer> profesorIds = List.of(-1);

        if (filtraProfesor) {
            profesorIds = usuarioRepository.buscarProfesoresPorFacultad(facultadId, profesorFiltro).stream()
                    .map(UsuarioModel::getIdUsuario)
                    .toList();
            if (profesorIds.isEmpty()) {
                return construirRespuestaHistorial(List.of());
            }
        }

        List<ReservaModel> reservas = reservaRepository.buscarHistorialFacultad(
                facultadId,
                fechaInicioFiltro,
                fechaFinFiltro,
                salaFiltro,
                filtraProfesor,
                profesorIds);

        return construirRespuestaHistorial(reservas);
    }

    @Transactional
    public CrearReservaResponse editarReserva(
            Integer idReserva,
            CrearReservaRequest request,
            String usuarioId,
            Integer facultadId,
            String rolUsuario) {
        Integer idUsuario = resolverUsuarioId(usuarioId);

        ReservaModel reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new RecursoNoEncontradoException("La reserva no existe"));

        boolean esSecretaria = "SECRETARIA".equalsIgnoreCase(rolUsuario);
        boolean esDueno = reserva.getIdUsuario().equals(idUsuario);

        if (!esSecretaria && !esDueno) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "No tiene permiso para editar esta reserva");
        }

        if (esSecretaria) {
            UsuarioModel secretaria = usuarioRepository.findById(idUsuario)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Secretaria no encontrada"));
            if (!secretaria.getIdFacultad().equals(reserva.getSala().getFacultadId())) {
                throw new BusinessException(HttpStatus.FORBIDDEN,
                        "La secretaria no pertenece a la facultad de la sala");
            }
        }

        if (reserva.getEstado() == EstadoReserva.CANCELADA || reserva.getEstado() == EstadoReserva.FINALIZADA) {
            throw new BusinessException(HttpStatus.CONFLICT, "Solo se pueden editar reservas activas");
        }

        LocalDate fecha = request.resolverFecha();
        LocalTime horaInicio = request.resolverHoraInicio();
        LocalTime horaFin = request.resolverHoraFin();

        if (fecha == null || horaInicio == null || horaFin == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Fecha, hora de inicio y hora de fin son obligatorias para editar la reserva");
        }
        if (fecha.isBefore(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha de la reserva debe ser hoy o una fecha futura");
        }
        validarHorarioPermitido(horaInicio, horaFin);
        if (horaFin.isBefore(horaInicio) || horaFin.equals(horaInicio)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La hora de fin debe ser posterior a la hora de inicio");
        }

        SalaModel salaDestino = request.getIdSala() != null
                && !request.getIdSala().equals(reserva.getSala().getIdSala())
                ? salaRepository.findById(request.getIdSala())
                        .orElseThrow(() -> new RecursoNoEncontradoException("La sala no existe"))
                : reserva.getSala();

        if (esSecretaria && !salaDestino.getFacultadId().equals(reserva.getSala().getFacultadId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "La sala destino debe pertenecer a la misma facultad de la reserva");
        }
        if (!esSecretaria && !salaDestino.getFacultadId().equals(facultadId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "La sala no pertenece a su facultad");
        }
        if (!salaDestino.isHabilitada()) {
            throw new BusinessException(HttpStatus.CONFLICT, "La sala destino no esta habilitada");
        }

        List<ReservaModel> solapadas = reservaRepository.findReservasSolapadas(
                salaDestino.getIdSala(), fecha, ESTADOS_OCUPACION, horaInicio, horaFin);
        boolean conflicto = solapadas.stream()
                .anyMatch(otra -> !otra.getIdReserva().equals(reserva.getIdReserva()));
        if (conflicto) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La sala ya esta reservada en el horario solicitado");
        }

        String anterior = "{\"idSala\":" + reserva.getSala().getIdSala()
                + ",\"fecha\":\"" + reserva.getFecha()
                + "\",\"horaInicio\":\"" + reserva.getHoraInicio()
                + "\",\"horaFin\":\"" + reserva.getHoraFin() + "\"}";

        reserva.setSala(salaDestino);
        reserva.setFecha(fecha);
        reserva.setHoraInicio(horaInicio);
        reserva.setHoraFin(horaFin);
        ReservaModel actualizada = reservaRepository.save(reserva);

        String nuevo = "{\"idSala\":" + actualizada.getSala().getIdSala()
                + ",\"fecha\":\"" + actualizada.getFecha()
                + "\",\"horaInicio\":\"" + actualizada.getHoraInicio()
                + "\",\"horaFin\":\"" + actualizada.getHoraFin() + "\"}";
        registrarAuditoriaReserva(actualizada.getIdReserva(), "EDICION", idUsuario, usuarioId,
                anterior, nuevo, "Reserva editada por " + (esSecretaria ? "secretaria" : "usuario"));

        notificarCambioReserva(
                actualizada,
                idUsuario,
                "RESERVA_EDITADA",
                "Reserva actualizada",
                "Tu reserva en " + actualizada.getSala().getNombre()
                        + " quedo programada para el " + actualizada.getFecha()
                        + " de " + actualizada.getHoraInicio() + " a " + actualizada.getHoraFin() + ".",
                esSecretaria);

        return new CrearReservaResponse(
                actualizada.getIdReserva(),
                actualizada.getSala().getIdSala(),
                actualizada.getSala().getNombre(),
                actualizada.getIdUsuario(),
                actualizada.getFecha(),
                actualizada.getHoraInicio(),
                actualizada.getHoraFin(),
                mapearEstadoParaFrontend(actualizada.getEstado()),
                "Reserva actualizada exitosamente");
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

        notificarCambioReserva(
                reserva,
                idUsuario,
                "RESERVA_CANCELADA",
                "Reserva cancelada",
                "La reserva en la sala " + reserva.getSala().getNombre()
                        + " del " + reserva.getFecha()
                        + " (" + reserva.getHoraInicio() + " - " + reserva.getHoraFin()
                        + ") fue cancelada por la " + (esSecretaria ? "secretaria" : "persona docente") + ".",
                esSecretaria);

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

    private void registrarAuditoriaReserva(
            Integer idReserva,
            String accion,
            Integer idUsuario,
            String correoActor,
            String datosAnteriores,
            String datosNuevos,
            String observacion) {
        AuditoriaModel auditoria = new AuditoriaModel();
        auditoria.setEntidad("reserva");
        auditoria.setEntidadId(idReserva.longValue());
        auditoria.setAccion(accion);
        auditoria.setUsuarioActor(idUsuario);
        auditoria.setCorreoActor(correoActor);
        auditoria.setFechaHora(java.time.LocalDateTime.now());
        auditoria.setDatosAnteriores(datosAnteriores);
        auditoria.setDatosNuevos(datosNuevos);
        auditoria.setObservacion(observacion);
        auditoriaRepository.save(auditoria);
    }

    private void notificarCambioReserva(
            ReservaModel reserva,
            Integer actorId,
            String tipo,
            String titulo,
            String mensaje,
            boolean accionPorSecretaria) {
        List<Integer> destinatarios = new ArrayList<>();
        if (accionPorSecretaria) {
            if (reserva.getIdUsuario() != null && !reserva.getIdUsuario().equals(actorId)) {
                destinatarios.add(reserva.getIdUsuario());
            }
        } else {
            destinatarios.addAll(usuarioRepository.buscarIdsPorFacultadYRol(
                    reserva.getSala().getFacultadId(), "SECRETARIA"));
            destinatarios.removeIf(id -> id.equals(actorId));
        }
        if (destinatarios.isEmpty()) {
            return;
        }
        notificacionService.notificar(destinatarios, tipo, titulo, mensaje,
                "reserva", reserva.getIdReserva().longValue());
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

    private HistorialReservasFacultadResponse construirRespuestaHistorial(List<ReservaModel> reservas) {
        List<HistorialReservaResponse> historial = reservas.stream()
                .map(this::toHistorialReservaResponse)
                .toList();
        String mensaje = historial.isEmpty()
                ? "No existe un registro de reservas para los criterios seleccionados"
                : "Historial de reservas consultado exitosamente";
        return new HistorialReservasFacultadResponse(mensaje, historial);
    }

    private HistorialReservaResponse toHistorialReservaResponse(ReservaModel reserva) {
        UsuarioModel profesor = usuarioRepository.findById(reserva.getIdUsuario()).orElse(null);
        return new HistorialReservaResponse(
                reserva.getIdReserva(),
                reserva.getSala().getIdSala(),
                reserva.getSala().getNombre(),
                reserva.getIdUsuario(),
                profesor == null ? null : profesor.getNombre(),
                profesor == null ? null : profesor.getCorreo(),
                reserva.getFecha(),
                reserva.getHoraInicio(),
                reserva.getHoraFin(),
                reserva.getEstado().name()
        );
    }

    private String normalizarFiltro(String filtro) {
        return filtro == null || filtro.isBlank() ? null : filtro.trim();
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
