package com.apiweb.backend.Controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.apiweb.backend.Service.SalaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/salas")
@Validated
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class SalaController {

    private final SalaService salaService;

    public SalaController(SalaService salaService) {
        this.salaService = salaService;
    }

    @PostMapping
    public ResponseEntity<SalaDetalleResponse> crearSala(
            @Valid @RequestBody CrearSalaRequest request,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return new ResponseEntity<>(
                salaService.crearSala(request, usuarioId, facultadId, rolUsuario),
                HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<SalaResumenResponse>> listarSalas(
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(salaService.listarSalas(facultadId, rolUsuario));
    }

    @GetMapping("/{idSala}")
    public ResponseEntity<SalaDetalleResponse> obtenerDetalle(
            @PathVariable Integer idSala,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(salaService.obtenerDetalle(idSala, facultadId, rolUsuario));
    }

    @PutMapping("/{idSala}")
    public ResponseEntity<SalaDetalleResponse> editarSala(
            @PathVariable Integer idSala,
            @Valid @RequestBody ActualizarSalaRequest request,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(salaService.editarSala(idSala, request, usuarioId, facultadId, rolUsuario));
    }

    @PatchMapping("/{idSala}/estado")
    public ResponseEntity<EstadoSalaResponse> actualizarEstado(
            @PathVariable Integer idSala,
            @Valid @RequestBody ActualizarEstadoSalaRequest request,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(salaService.actualizarEstado(idSala, request, usuarioId, facultadId, rolUsuario));
    }

    @PostMapping("/{idSala}/recursos")
    public ResponseEntity<RecursoSalaResponse> agregarRecurso(
            @PathVariable Integer idSala,
            @Valid @RequestBody AgregarRecursoRequest request,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return new ResponseEntity<>(
                salaService.agregarRecurso(idSala, request, usuarioId, facultadId, rolUsuario),
                HttpStatus.CREATED);
    }

    @DeleteMapping("/{idSala}/recursos")
    public ResponseEntity<RetirarRecursoResponse> retirarRecurso(
            @PathVariable Integer idSala,
            @Valid @RequestBody RetirarRecursoRequest request,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(salaService.retirarRecurso(idSala, request, usuarioId, facultadId, rolUsuario));
    }

    @DeleteMapping("/{idSala}/recursos/{idRecursoSala}")
    public ResponseEntity<RetirarRecursoResponse> retirarRecursoPorId(
            @PathVariable Integer idSala,
            @PathVariable Integer idRecursoSala,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(
                salaService.retirarRecurso(idSala, idRecursoSala, usuarioId, facultadId, rolUsuario));
    }
}
