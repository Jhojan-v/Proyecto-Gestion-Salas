package com.apiweb.backend.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.apiweb.backend.DTO.NotificacionResponse;
import com.apiweb.backend.DTO.NotificacionesResumenResponse;
import com.apiweb.backend.Exception.BusinessException;
import com.apiweb.backend.Exception.RecursoNoEncontradoException;
import com.apiweb.backend.Model.NotificacionModel;
import com.apiweb.backend.Repository.NotificacionRepository;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional
    public void notificar(Collection<Integer> destinatarios,
                          String tipo,
                          String titulo,
                          String mensaje,
                          String entidad,
                          Long entidadId) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return;
        }
        Set<Integer> unicos = new HashSet<>(destinatarios);
        unicos.removeIf(id -> id == null);
        if (unicos.isEmpty()) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        for (Integer idUsuario : unicos) {
            NotificacionModel notificacion = new NotificacionModel();
            notificacion.setIdUsuario(idUsuario);
            notificacion.setTipo(tipo);
            notificacion.setTitulo(titulo);
            notificacion.setMensaje(mensaje);
            notificacion.setEntidad(entidad);
            notificacion.setEntidadId(entidadId);
            notificacion.setLeida(false);
            notificacion.setFechaCreacion(ahora);
            notificacionRepository.save(notificacion);
        }
    }

    @Transactional(readOnly = true)
    public NotificacionesResumenResponse listarParaUsuario(Integer idUsuario) {
        List<NotificacionResponse> notificaciones = notificacionRepository
                .findByIdUsuarioOrderByFechaCreacionDesc(idUsuario)
                .stream()
                .map(this::toResponse)
                .toList();
        long noLeidas = notificacionRepository.countByIdUsuarioAndLeidaFalse(idUsuario);
        return new NotificacionesResumenResponse(noLeidas, notificaciones);
    }

    @Transactional
    public NotificacionResponse marcarComoLeida(Integer idNotificacion, Integer idUsuario) {
        NotificacionModel notificacion = notificacionRepository.findById(idNotificacion)
                .orElseThrow(() -> new RecursoNoEncontradoException("La notificacion no existe"));
        if (!notificacion.getIdUsuario().equals(idUsuario)) {
            throw new BusinessException(HttpStatus.FORBIDDEN,
                    "No tiene permiso para modificar esta notificacion");
        }
        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacion = notificacionRepository.save(notificacion);
        }
        return toResponse(notificacion);
    }

    @Transactional
    public long marcarTodasComoLeidas(Integer idUsuario) {
        return notificacionRepository.marcarTodasLeidas(idUsuario);
    }

    private NotificacionResponse toResponse(NotificacionModel notificacion) {
        return new NotificacionResponse(
                notificacion.getIdNotificacion(),
                notificacion.getTipo(),
                notificacion.getTitulo(),
                notificacion.getMensaje(),
                notificacion.getEntidad(),
                notificacion.getEntidadId(),
                notificacion.isLeida(),
                notificacion.getFechaCreacion());
    }
}
