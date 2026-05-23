package com.apiweb.backend.Controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.DTO.CancelarReservaResponse;
import com.apiweb.backend.DTO.CrearReservaRequest;
import com.apiweb.backend.DTO.CrearReservaResponse;
import com.apiweb.backend.DTO.DisponibilidadSalaResponse;
import com.apiweb.backend.DTO.HistorialReservasFacultadResponse;
import com.apiweb.backend.DTO.ReporteReservasResponse;
import com.apiweb.backend.DTO.ReporteHorasResponse;
import com.apiweb.backend.DTO.ReporteUsuarioDetalleResponse;
import com.apiweb.backend.DTO.ReporteUsuariosResponse;
import com.apiweb.backend.Security.UsuarioContext;
import com.apiweb.backend.Service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
@Validated
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioContext usuarioContext;

    public ReservaController(ReservaService reservaService, UsuarioContext usuarioContext) {
        this.reservaService = reservaService;
        this.usuarioContext = usuarioContext;
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<DisponibilidadSalaResponse>> consultarDisponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
        return ResponseEntity.ok(reservaService.consultarDisponibilidad(
                fecha,
                usuarioContext.resolverFacultadId(facultadId),
                String.valueOf(usuarioContext.resolverIdUsuario(usuarioId))));
    }

    @GetMapping("/disponibilidad/sala/{idSala}")
    public ResponseEntity<DisponibilidadSalaResponse> consultarDisponibilidadSala(
            @PathVariable Integer idSala,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
        return ResponseEntity.ok(reservaService.consultarDisponibilidadSala(
                idSala,
                fecha,
                usuarioContext.resolverFacultadId(facultadId),
                String.valueOf(usuarioContext.resolverIdUsuario(usuarioId))));
    }

    @PostMapping
    public ResponseEntity<CrearReservaResponse> crearReserva(
            @RequestBody CrearReservaRequest request,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario) {
        return new ResponseEntity<>(
                reservaService.crearReserva(
                        request,
                        String.valueOf(usuarioContext.resolverIdUsuario(usuarioId)),
                        usuarioContext.resolverFacultadId(facultadId),
                        usuarioContext.resolverRol(rolUsuario)),
                HttpStatus.CREATED);
    }

    @GetMapping("/mis-reservas")
    public ResponseEntity<List<CrearReservaResponse>> listarMisReservas(
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
        return ResponseEntity.ok(reservaService.listarReservasUsuario(
                String.valueOf(usuarioContext.resolverIdUsuario(usuarioId))));
    }

    @GetMapping("/mi-historial")
    public ResponseEntity<HistorialReservasFacultadResponse> consultarMiHistorial(
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId) {
        return ResponseEntity.ok(reservaService.consultarHistorialDocente(
                String.valueOf(usuarioContext.resolverIdUsuario(usuarioId))));
    }

    @GetMapping("/reporte/reservas")
    public ResponseEntity<ReporteReservasResponse> generarReporteReservas(
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reservaService.generarReporteReservas(
                usuarioContext.resolverFacultadId(facultadId),
                usuarioContext.resolverRol(rolUsuario),
                fechaInicio,
                fechaFin));
    }

    @GetMapping("/reporte/horas")
    public ResponseEntity<ReporteHorasResponse> generarReporteHoras(
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reservaService.generarReporteHoras(
                usuarioContext.resolverFacultadId(facultadId),
                usuarioContext.resolverRol(rolUsuario),
                fechaInicio,
                fechaFin));
    }

    @GetMapping("/reporte/usuarios")
    public ResponseEntity<ReporteUsuariosResponse> generarReporteUsuarios(
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario,
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(reservaService.generarReporteUsuarios(
                usuarioContext.resolverFacultadId(facultadId),
                usuarioContext.resolverRol(rolUsuario),
                busqueda));
    }

    @GetMapping("/reporte/usuarios/{idUsuario}")
    public ResponseEntity<ReporteUsuarioDetalleResponse> generarReporteUsuarioDetalle(
            @PathVariable Integer idUsuario,
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario) {
        return ResponseEntity.ok(reservaService.generarReporteUsuarioDetalle(
                idUsuario,
                usuarioContext.resolverFacultadId(facultadId),
                usuarioContext.resolverRol(rolUsuario)));
    }

    @GetMapping("/historial/facultad")
    public ResponseEntity<HistorialReservasFacultadResponse> consultarHistorialFacultad(
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario,
            @RequestParam(required = false) String profesor,
            @RequestParam(required = false) String sala,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        return ResponseEntity.ok(reservaService.consultarHistorialFacultad(
                usuarioContext.resolverFacultadId(facultadId),
                usuarioContext.resolverRol(rolUsuario),
                profesor,
                sala,
                fecha,
                fechaInicio,
                fechaFin));
    }

    @GetMapping
    public ResponseEntity<List<CrearReservaResponse>> listarReservasFacultad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario) {
        return ResponseEntity.ok(reservaService.listarReservasFacultad(
                fecha,
                usuarioContext.resolverFacultadId(facultadId),
                usuarioContext.resolverRol(rolUsuario)));
    }

    @PutMapping("/{idReserva}")
    public ResponseEntity<CrearReservaResponse> editarReserva(
            @PathVariable Integer idReserva,
            @RequestBody CrearReservaRequest request,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
            @RequestHeader(value = "X-Facultad-Id", required = false) Integer facultadId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario) {
        return ResponseEntity.ok(
                reservaService.editarReserva(
                        idReserva,
                        request,
                        String.valueOf(usuarioContext.resolverIdUsuario(usuarioId)),
                        facultadId != null ? facultadId : usuarioContext.resolverFacultadId(facultadId),
                        usuarioContext.resolverRol(rolUsuario)));
    }

    @DeleteMapping("/{idReserva}")
    public ResponseEntity<CancelarReservaResponse> cancelarReserva(
            @PathVariable Integer idReserva,
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId,
            @RequestHeader(value = "X-Rol", required = false) String rolUsuario) {
        return ResponseEntity.ok(reservaService.cancelarReserva(
                idReserva,
                String.valueOf(usuarioContext.resolverIdUsuario(usuarioId)),
                usuarioContext.resolverRol(rolUsuario)));
    }
}
