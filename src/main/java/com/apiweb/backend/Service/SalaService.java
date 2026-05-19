package com.apiweb.backend.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apiweb.backend.DTO.ActualizarEstadoSalaRequest;
import com.apiweb.backend.DTO.ActualizarSalaRequest;
import com.apiweb.backend.DTO.AgregarRecursoRequest;
import com.apiweb.backend.DTO.CrearSalaRequest;
import com.apiweb.backend.DTO.EstadoSalaResponse;
import com.apiweb.backend.DTO.RecursoSalaResponse;
import com.apiweb.backend.DTO.RetirarRecursoRequest;
import com.apiweb.backend.DTO.RetirarRecursoResponse;
import com.apiweb.backend.DTO.SalaDetalleResponse;
import com.apiweb.backend.DTO.SalaResumenResponse;
import com.apiweb.backend.Exception.BusinessException;
import com.apiweb.backend.Exception.RecursoNoEncontradoException;
import com.apiweb.backend.Model.AuditoriaModel;
import com.apiweb.backend.Model.EstadoReserva;
import com.apiweb.backend.Model.RecursoSalaModel;
import com.apiweb.backend.Model.RecursoTecnologicoModel;
import com.apiweb.backend.Model.ReservaModel;
import com.apiweb.backend.Model.SalaModel;
import com.apiweb.backend.Repository.AuditoriaRepository;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Repository.RecursoSalaRepository;
import com.apiweb.backend.Repository.RecursoTecnologicoRepository;
import com.apiweb.backend.Repository.ReservaRepository;
import com.apiweb.backend.Repository.SalaRepository;
import com.apiweb.backend.Security.UsuarioContext;

@Service
public class SalaService {

    private static final String ROL_SECRETARIA = "SECRETARIA";
    private static final List<EstadoReserva> ESTADOS_ACTIVOS =
            List.of(EstadoReserva.CONFIRMADA, EstadoReserva.PENDIENTE);

    private final SalaRepository salaRepository;
    private final RecursoSalaRepository recursoSalaRepository;
    private final RecursoTecnologicoRepository recursoTecnologicoRepository;
    private final ReservaRepository reservaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final IUsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final UsuarioContext usuarioContext;

    public SalaService(
            SalaRepository salaRepository,
            RecursoSalaRepository recursoSalaRepository,
            RecursoTecnologicoRepository recursoTecnologicoRepository,
            ReservaRepository reservaRepository,
            AuditoriaRepository auditoriaRepository,
            IUsuarioRepository usuarioRepository,
            NotificacionService notificacionService,
            UsuarioContext usuarioContext) {
        this.salaRepository = salaRepository;
        this.recursoSalaRepository = recursoSalaRepository;
        this.recursoTecnologicoRepository = recursoTecnologicoRepository;
        this.reservaRepository = reservaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.usuarioContext = usuarioContext;
    }

    @Transactional
    public SalaDetalleResponse crearSala(
            CrearSalaRequest request,
            String usuarioId,
            Integer facultadIdHeader,
            String rolUsuario) {
        validarRolSecretaria(rolUsuario);
        validarUsuario(usuarioId);

        Integer facultadId = resolverFacultadId(request, facultadIdHeader);
        String nombreNormalizado = request.getNombre().trim();
        if (salaRepository.existsByNombreIgnoreCaseAndFacultadId(nombreNormalizado, facultadId)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Ya existe una sala con ese nombre en la facultad");
        }

        SalaModel sala = new SalaModel();
        sala.setNombre(nombreNormalizado);
        sala.setUbicacion(request.getUbicacion().trim());
        sala.setCapacidad(request.getCapacidad());
        sala.setFacultadId(facultadId);
        sala.setHabilitada(true);

        SalaModel creada = salaRepository.save(sala);
        registrarAuditoria("sala", creada.getIdSala().longValue(), "CREACION_SALA", usuarioId,
                "{}", serializarSala(creada), "Sala creada correctamente");

        return toDetalle(creada);
    }

    @Transactional(readOnly = true)
    public List<SalaResumenResponse> listarSalas(Integer facultadId, String rolUsuario) {
        validarFacultadId(facultadId);

        List<SalaModel> salas = esRolSecretaria(rolUsuario)
                ? salaRepository.findByFacultadIdOrderByNombreAsc(facultadId)
                : salaRepository.findByFacultadIdAndHabilitadaTrueOrderByNombreAsc(facultadId);

        return salas
                .stream()
                .map(this::toResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalaDetalleResponse obtenerDetalle(Integer idSala, Integer facultadId, String rolUsuario) {
        validarFacultadId(facultadId);
        SalaModel sala = obtenerSalaDeFacultad(idSala, facultadId);
        if (!esRolSecretaria(rolUsuario) && !sala.isHabilitada()) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Solo puede consultar salas habilitadas de su propia facultad");
        }
        return toDetalle(sala);
    }

    @Transactional
    public SalaDetalleResponse editarSala(
            Integer idSala,
            ActualizarSalaRequest request,
            String usuarioId,
            Integer facultadId,
            String rolUsuario) {
        validarAccesoSecretaria(facultadId, rolUsuario);
        validarUsuario(usuarioId);

        SalaModel sala = obtenerSalaDeFacultad(idSala, facultadId);
        if (!sala.isHabilitada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La sala no esta disponible para edicion porque se encuentra deshabilitada");
        }

        String nombreNormalizado = request.getNombre().trim();
        if (salaRepository.existsByNombreIgnoreCaseAndFacultadIdAndIdSalaNot(nombreNormalizado, facultadId, idSala)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Ya existe una sala con ese nombre en la facultad");
        }

        String datosAnteriores = serializarSala(sala);
        sala.setNombre(nombreNormalizado);
        sala.setUbicacion(request.getUbicacion().trim());
        sala.setCapacidad(request.getCapacidad());
        SalaModel actualizada = salaRepository.save(sala);

        registrarAuditoria("sala", actualizada.getIdSala().longValue(), "EDICION_SALA", usuarioId,
                datosAnteriores, serializarSala(actualizada), null);

        notificarUsuariosSalaActualizada(
                actualizada,
                usuarioId,
                "SALA_EDITADA",
                "Sala actualizada: " + actualizada.getNombre(),
                "La sala '" + actualizada.getNombre()
                        + "' fue editada. Ubicacion: " + actualizada.getUbicacion()
                        + ". Capacidad: " + actualizada.getCapacidad() + ".");

        return toDetalle(actualizada);
    }

    @Transactional
    public EstadoSalaResponse actualizarEstado(
            Integer idSala,
            ActualizarEstadoSalaRequest request,
            String usuarioId,
            Integer facultadId,
            String rolUsuario) {
        validarAccesoSecretaria(facultadId, rolUsuario);
        validarUsuario(usuarioId);

        SalaModel sala = obtenerSalaDeFacultad(idSala, facultadId);
        String datosAnteriores = serializarSala(sala);
        Integer reservaCanceladaId = null;
        String mensaje = request.getHabilitada()
                ? "La sala fue habilitada correctamente"
                : "La sala fue deshabilitada correctamente";

        if (!request.getHabilitada()) {
            ReservaModel reservaActiva = reservaRepository
                    .findFirstBySalaIdSalaAndEstadoOrderByFechaDescHoraInicioDesc(idSala, EstadoReserva.CONFIRMADA)
                    .orElse(null);

            if (reservaActiva != null) {
                reservaActiva.setEstado(EstadoReserva.CANCELADA);
                reservaRepository.save(reservaActiva);
                reservaCanceladaId = reservaActiva.getIdReserva();
                mensaje = "La sala fue deshabilitada y la reserva activa fue cancelada";
            }
        }

        sala.setHabilitada(request.getHabilitada());
        SalaModel actualizada = salaRepository.save(sala);

        registrarAuditoria("sala", actualizada.getIdSala().longValue(), "CAMBIO_ESTADO_SALA", usuarioId,
                datosAnteriores, serializarSala(actualizada), mensaje);

        notificarUsuariosSalaActualizada(
                actualizada,
                usuarioId,
                actualizada.isHabilitada() ? "SALA_HABILITADA" : "SALA_DESHABILITADA",
                "Cambio de estado: " + actualizada.getNombre(),
                mensaje);

        return new EstadoSalaResponse(actualizada.getIdSala(), actualizada.isHabilitada(), mensaje, reservaCanceladaId);
    }

    @Transactional
    public RecursoSalaResponse agregarRecurso(
            Integer idSala,
            AgregarRecursoRequest request,
            String usuarioId,
            Integer facultadId,
            String rolUsuario) {
        validarAccesoSecretaria(facultadId, rolUsuario);
        validarUsuario(usuarioId);

        SalaModel sala = obtenerSalaDeFacultad(idSala, facultadId);
        if (!sala.isHabilitada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La sala no esta disponible para agregar recursos porque se encuentra deshabilitada");
        }

        String codigo = request.getCodigoRecurso().trim();
        String nombre = request.getNombreRecurso().trim();
        String descripcion = request.getDescripcion() == null ? null : request.getDescripcion().trim();

        RecursoTecnologicoModel existente = recursoTecnologicoRepository
                .findByCodigoRecursoIgnoreCase(codigo)
                .orElse(null);

        if (existente != null) {
            boolean asignado = recursoSalaRepository
                    .findBySalaIdSalaAndRecursoCodigoRecursoIgnoreCase(
                            existente.getIdRecurso() == null ? -1 : existente.getIdRecurso(), codigo)
                    .isPresent();
            boolean asignadoOtraSala = recursoSalaRepository.existsByRecursoCodigoRecursoIgnoreCase(codigo);
            if (asignado || asignadoOtraSala) {
                throw new BusinessException(HttpStatus.CONFLICT,
                        "El codigo '" + codigo + "' ya esta asignado a una sala. Cada unidad debe tener un codigo unico");
            }
        }

        RecursoTecnologicoModel recursoTecnologico = existente != null
                ? existente
                : recursoTecnologicoRepository.save(
                        new RecursoTecnologicoModel(null, codigo, nombre, descripcion, true));

        if (existente != null) {
            boolean cambio = false;
            if (!nombre.equals(recursoTecnologico.getNombreRecurso())) {
                recursoTecnologico.setNombreRecurso(nombre);
                cambio = true;
            }
            if (descripcion != null && !descripcion.equals(recursoTecnologico.getDescripcion())) {
                recursoTecnologico.setDescripcion(descripcion);
                cambio = true;
            }
            if (cambio) {
                recursoTecnologico = recursoTecnologicoRepository.save(recursoTecnologico);
            }
        }

        RecursoSalaModel recursoSala = new RecursoSalaModel();
        recursoSala.setSala(sala);
        recursoSala.setRecurso(recursoTecnologico);
        recursoSala.setCantidad(1);

        RecursoSalaModel guardado = recursoSalaRepository.save(recursoSala);
        registrarAuditoria("sala_recurso", guardado.getIdRecursoSala().longValue(),
                "AGREGAR_RECURSO_SALA", usuarioId,
                "{}", serializarRecurso(guardado),
                "Recurso registrado como unidad unica con codigo " + codigo);

        notificarUsuariosSalaActualizada(
                sala,
                usuarioId,
                "RECURSO_AGREGADO",
                "Recurso agregado a sala " + sala.getNombre(),
                "Se registro el recurso " + nombre + " (codigo " + codigo + ") en la sala " + sala.getNombre());

        return new RecursoSalaResponse(
                guardado.getIdRecursoSala(),
                guardado.getRecurso().getCodigoRecurso(),
                guardado.getRecurso().getNombreRecurso(),
                guardado.getCantidad(),
                "El recurso fue registrado correctamente como unidad unica");
    }

    @Transactional
    public RetirarRecursoResponse retirarRecurso(
            Integer idSala,
            RetirarRecursoRequest request,
            String usuarioId,
            Integer facultadId,
            String rolUsuario) {
        validarAccesoSecretaria(facultadId, rolUsuario);
        validarUsuario(usuarioId);

        SalaModel sala = obtenerSalaDeFacultad(idSala, facultadId);
        if (!sala.isHabilitada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La sala no esta disponible para retirar recursos porque se encuentra deshabilitada");
        }

        String codigo = request.getCodigoRecurso().trim();
        RecursoSalaModel recursoSala = recursoSalaRepository
                .findBySalaIdSalaAndRecursoCodigoRecursoIgnoreCase(idSala, codigo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El recurso con codigo '" + codigo + "' no esta asociado a esta sala"));

        return eliminarRecursoSala(recursoSala, usuarioId);
    }

    @Transactional
    public RetirarRecursoResponse retirarRecurso(
            Integer idSala,
            Integer idRecursoSala,
            String usuarioId,
            Integer facultadId,
            String rolUsuario) {
        validarAccesoSecretaria(facultadId, rolUsuario);
        validarUsuario(usuarioId);

        SalaModel sala = obtenerSalaDeFacultad(idSala, facultadId);
        if (!sala.isHabilitada()) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "La sala no esta disponible para retirar recursos porque se encuentra deshabilitada");
        }

        RecursoSalaModel recursoSala = recursoSalaRepository.findById(idRecursoSala)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El recurso asociado a la sala no existe"));

        if (!recursoSala.getSala().getIdSala().equals(idSala)) {
            throw new RecursoNoEncontradoException(
                    "El recurso asociado no pertenece a la sala indicada");
        }

        return eliminarRecursoSala(recursoSala, usuarioId);
    }

    private RetirarRecursoResponse eliminarRecursoSala(RecursoSalaModel recursoSala, String usuarioId) {
        Integer idRecursoSalaEliminado = recursoSala.getIdRecursoSala();
        String codigoRecursoEliminado = recursoSala.getRecurso().getCodigoRecurso();
        String nombreRecursoEliminado = recursoSala.getRecurso().getNombreRecurso();
        String datosAnteriores = serializarRecurso(recursoSala);
        SalaModel sala = recursoSala.getSala();

        recursoSalaRepository.delete(recursoSala);
        registrarAuditoria("sala_recurso", idRecursoSalaEliminado.longValue(),
                "RETIRAR_RECURSO_SALA", usuarioId,
                datosAnteriores, "{}",
                "Recurso retirado de la sala: " + nombreRecursoEliminado);

        notificarUsuariosSalaActualizada(
                sala,
                usuarioId,
                "RECURSO_RETIRADO",
                "Recurso retirado de sala " + sala.getNombre(),
                "Se retiro el recurso " + nombreRecursoEliminado
                        + " (codigo " + codigoRecursoEliminado + ") de la sala " + sala.getNombre());

        return new RetirarRecursoResponse(
                idRecursoSalaEliminado,
                codigoRecursoEliminado,
                nombreRecursoEliminado,
                "El recurso fue retirado correctamente de la sala");
    }

    private void validarAccesoSecretaria(Integer facultadId, String rolUsuario) {
        validarFacultadId(facultadId);
        validarRolSecretaria(rolUsuario);
    }

    private void validarRolSecretaria(String rolUsuario) {
        if (!esRolSecretaria(rolUsuario)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Solo una secretaria autenticada y autorizada puede gestionar salas");
        }
    }

    private boolean esRolSecretaria(String rolUsuario) {
        return rolUsuario != null && ROL_SECRETARIA.equalsIgnoreCase(rolUsuario.trim());
    }

    private void validarFacultadId(Integer facultadId) {
        if (facultadId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El encabezado X-Facultad-Id es obligatorio");
        }
    }

    private void validarUsuario(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El encabezado X-Usuario-Id es obligatorio");
        }
    }

    private Integer resolverFacultadId(CrearSalaRequest request, Integer facultadIdHeader) {
        if (request.getFacultadId() != null) {
            return request.getFacultadId();
        }

        if (request.getFacultad() != null && !request.getFacultad().isBlank()) {
            return switch (normalizarTexto(request.getFacultad())) {
                case "facultad de ingenieria y ciencias basicas" -> 1;
                case "facultad de comunicacion social, humanidades y artes" -> 2;
                case "facultad de arquitectura, urbanismo y diseno" -> 3;
                case "facultad de administracion" -> 4;
                default -> throw new BusinessException(HttpStatus.BAD_REQUEST,
                        "La facultad indicada no es valida");
            };
        }

        if (facultadIdHeader != null) {
            return facultadIdHeader;
        }

        throw new BusinessException(HttpStatus.BAD_REQUEST,
                "Debe indicar la facultad por encabezado, id o nombre");
    }

    private SalaModel obtenerSalaDeFacultad(Integer idSala, Integer facultadId) {
        SalaModel sala = salaRepository.findById(idSala)
                .orElseThrow(() -> new RecursoNoEncontradoException("La sala no existe"));

        if (!sala.getFacultadId().equals(facultadId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "Solo puede gestionar salas de su propia facultad");
        }
        return sala;
    }

    private void registrarAuditoria(
            String entidad,
            Long entidadId,
            String accion,
            String usuarioHeader,
            String datosAnteriores,
            String datosNuevos,
            String observacion) {
        AuditoriaModel auditoria = new AuditoriaModel();
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setAccion(accion);
        auditoria.setUsuarioActor(parseUsuarioActor(usuarioHeader));
        auditoria.setCorreoActor(usuarioHeader);
        auditoria.setFechaHora(LocalDateTime.now());
        auditoria.setDatosAnteriores(datosAnteriores);
        auditoria.setDatosNuevos(datosNuevos);
        auditoria.setObservacion(observacion);
        auditoriaRepository.save(auditoria);
    }

    private Integer parseUsuarioActor(String usuarioHeader) {
        try {
            return Integer.valueOf(usuarioHeader);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private SalaResumenResponse toResumen(SalaModel sala) {
        return new SalaResumenResponse(
                sala.getIdSala(),
                sala.getNombre(),
                sala.getUbicacion(),
                sala.getCapacidad(),
                sala.isHabilitada());
    }

    private SalaDetalleResponse toDetalle(SalaModel sala) {
        List<RecursoSalaResponse> recursos = recursoSalaRepository
                .findBySalaIdSalaOrderByRecursoNombreRecursoAsc(sala.getIdSala())
                .stream()
                .map(recurso -> new RecursoSalaResponse(
                        recurso.getIdRecursoSala(),
                        recurso.getRecurso().getCodigoRecurso(),
                        recurso.getRecurso().getNombreRecurso(),
                        recurso.getCantidad(),
                        null))
                .toList();

        return new SalaDetalleResponse(
                sala.getIdSala(),
                sala.getNombre(),
                sala.getUbicacion(),
                sala.getCapacidad(),
                sala.getFacultadId(),
                sala.isHabilitada(),
                recursos);
    }

    private String serializarSala(SalaModel sala) {
        return "{\"idSala\":" + sala.getIdSala()
                + ",\"nombre\":\"" + escapar(sala.getNombre())
                + "\",\"ubicacion\":\"" + escapar(sala.getUbicacion())
                + "\",\"capacidad\":" + sala.getCapacidad()
                + ",\"facultadId\":" + sala.getFacultadId()
                + ",\"habilitada\":" + sala.isHabilitada()
                + "}";
    }

    private String serializarRecurso(RecursoSalaModel recurso) {
        return "{\"idSalaRecurso\":" + recurso.getIdRecursoSala()
                + ",\"codigoRecurso\":\"" + escapar(recurso.getRecurso().getCodigoRecurso())
                + "\",\"nombreRecurso\":\"" + escapar(recurso.getRecurso().getNombreRecurso())
                + "\",\"cantidad\":" + recurso.getCantidad()
                + ",\"idSala\":" + recurso.getSala().getIdSala()
                + "}";
    }

    private String escapar(String valor) {
        return valor == null ? "" : valor.replace("\"", "\\\"");
    }

    private void notificarUsuariosSalaActualizada(
            SalaModel sala,
            String usuarioHeader,
            String tipo,
            String titulo,
            String mensaje) {
        Set<Integer> destinatarios = new LinkedHashSet<>();
        destinatarios.addAll(reservaRepository.buscarUsuariosConReservasActivas(
                sala.getIdSala(), ESTADOS_ACTIVOS, LocalDate.now()));
        destinatarios.addAll(usuarioRepository.buscarIdsPorFacultadYRol(
                sala.getFacultadId(), ROL_SECRETARIA));

        Integer actor = resolverActor(usuarioHeader);
        if (actor != null) {
            destinatarios.remove(actor);
        }
        if (destinatarios.isEmpty()) {
            return;
        }
        notificacionService.notificar(new ArrayList<>(destinatarios), tipo, titulo, mensaje,
                "sala", sala.getIdSala().longValue());
    }

    private Integer resolverActor(String usuarioHeader) {
        if (usuarioHeader == null || usuarioHeader.isBlank()) {
            return null;
        }
        try {
            return usuarioContext.resolverIdUsuario(usuarioHeader);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String normalizarTexto(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
