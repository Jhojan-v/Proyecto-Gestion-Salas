package com.apiweb.backend.Controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.DTO.NotificacionResponse;
import com.apiweb.backend.DTO.NotificacionesResumenResponse;
import com.apiweb.backend.Security.UsuarioContext;
import com.apiweb.backend.Service.NotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioContext usuarioContext;

    public NotificacionController(NotificacionService notificacionService, UsuarioContext usuarioContext) {
        this.notificacionService = notificacionService;
        this.usuarioContext = usuarioContext;
    }

    @GetMapping("/mias")
    public ResponseEntity<NotificacionesResumenResponse> listarMias(
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        Integer idUsuario = usuarioContext.resolverIdUsuario(usuarioId);
        return ResponseEntity.ok(notificacionService.listarParaUsuario(idUsuario));
    }

    @PatchMapping("/{idNotificacion}/leer")
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @PathVariable Integer idNotificacion,
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        Integer idUsuario = usuarioContext.resolverIdUsuario(usuarioId);
        return ResponseEntity.ok(notificacionService.marcarComoLeida(idNotificacion, idUsuario));
    }

    @PatchMapping("/leer-todas")
    public ResponseEntity<Map<String, Object>> marcarTodasLeidas(
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        Integer idUsuario = usuarioContext.resolverIdUsuario(usuarioId);
        long actualizadas = notificacionService.marcarTodasComoLeidas(idUsuario);
        return ResponseEntity.ok(Map.of("actualizadas", actualizadas));
    }
}
