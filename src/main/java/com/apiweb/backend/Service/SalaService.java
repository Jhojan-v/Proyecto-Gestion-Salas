package com.apiweb.backend.Service;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

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
import com.apiweb.backend.Repository.RecursoSalaRepository;
import com.apiweb.backend.Repository.RecursoTecnologicoRepository;
import com.apiweb.backend.Repository.ReservaRepository;
import com.apiweb.backend.Repository.SalaRepository;

@Service
public class SalaService {

    private static final String ROL_SECRETARIA = "SECRETARIA";

    private final SalaRepository salaRepository;
    private final RecursoSalaRepository recursoSalaRepository;
    private final RecursoTecnologicoRepository recursoTecnologicoRepository;
    private final ReservaRepository reservaRepository;
    private final AuditoriaRepository auditoriaRepository;

    public SalaService(
            SalaRepository salaRepository,
            RecursoSalaRepository recursoSalaRepository,
            RecursoTecnologicoRepository recursoTecnologicoRepository,
            ReservaRepository reservaRepository,
            AuditoriaRepository auditoriaRepository) {
        this.salaRepository = salaRepository;
        this.recursoSalaRepository = recursoSalaRepository;
        this.recursoTecnologicoRepository = recursoTecnologicoRepository;
        this.reservaRepository = reservaRepository;
        this.auditoriaRepository = auditoriaRepository;
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

        RecursoTecnologicoModel recursoTecnologico = recursoTecnologicoRepository
                .findByCodigoRecursoIgnoreCase(codigo)
                .orElseGet(() -> recursoTecnologicoRepository.save(
                        new RecursoTecnologicoModel(null, codigo, nombre, null, true)));

        if (!nombre.equals(recursoTecnologico.getNombreRecurso())) {
            recursoTecnologico.setNombreRecurso(nombre);
            recursoTecnologico = recursoTecnologicoRepository.save(recursoTecnologico);
        }

        RecursoSalaModel recursoSala = recursoSalaRepository
                .findBySalaIdSalaAndRecursoCodigoRecursoIgnoreCase(idSala, codigo)
                .orElse(null);

        String accion;
        String datosAnteriores;
        if (recursoSala == null) {
            recursoSala = new RecursoSalaModel();
            recursoSala.setSala(sala);
            recursoSala.setRecurso(recursoTecnologico);
            recursoSala.setCantidad(request.getCantidad());
            accion = "AGREGAR_RECURSO_SALA";
            datosAnteriores = "{}";
        } else {
            datosAnteriores = serializarRecurso(recursoSala);
            recursoSala.setCantidad(recursoSala.getCantidad() + request.getCantidad());
            accion = "ACTUALIZAR_CANTIDAD_RECURSO_SALA";
        }

        RecursoSalaModel guardado = recursoSalaRepository.save(recursoSala);
        registrarAuditoria("sala_recurso", guardado.getIdRecursoSala().longValue(), accion, usuarioId,
                datosAnteriores, serializarRecurso(guardado), null);

        String mensaje = accion.equals("AGREGAR_RECURSO_SALA")
                ? "El recurso fue agregado correctamente a la sala"
                : "El recurso ya estaba asignado y se actualizo la cantidad";

        return new RecursoSalaResponse(
                guardado.getIdRecursoSala(),
                guardado.getRecurso().getCodigoRecurso(),
                guardado.getRecurso().getNombreRecurso(),
                guardado.getCantidad(),
                mensaje);
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

        recursoSalaRepository.delete(recursoSala);
        registrarAuditoria("sala_recurso", idRecursoSalaEliminado.longValue(),
                "RETIRAR_RECURSO_SALA", usuarioId,
                datosAnteriores, "{}",
                "Recurso retirado de la sala: " + nombreRecursoEliminado);

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

    private String normalizarTexto(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
