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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.DTO.CancelarReservaResponse;
import com.apiweb.backend.DTO.CrearReservaRequest;
import com.apiweb.backend.DTO.CrearReservaResponse;
import com.apiweb.backend.DTO.DisponibilidadSalaResponse;
import com.apiweb.backend.Service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
@Validated
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping("/disponibilidad")
    public ResponseEntity<List<DisponibilidadSalaResponse>> consultarDisponibilidad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        return ResponseEntity.ok(reservaService.consultarDisponibilidad(fecha, facultadId, usuarioId));
    }

    @GetMapping("/disponibilidad/sala/{idSala}")
    public ResponseEntity<DisponibilidadSalaResponse> consultarDisponibilidadSala(
            @PathVariable Integer idSala,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        return ResponseEntity.ok(reservaService.consultarDisponibilidadSala(idSala, fecha, facultadId, usuarioId));
    }

    @PostMapping
    public ResponseEntity<CrearReservaResponse> crearReserva(
            @RequestBody CrearReservaRequest request,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return new ResponseEntity<>(
                reservaService.crearReserva(request, usuarioId, facultadId, rolUsuario),
                HttpStatus.CREATED);
    }

    @GetMapping("/mis-reservas")
    public ResponseEntity<List<CrearReservaResponse>> listarMisReservas(
            @RequestHeader("X-Usuario-Id") String usuarioId) {
        return ResponseEntity.ok(reservaService.listarReservasUsuario(usuarioId));
    }

    @GetMapping
    public ResponseEntity<List<CrearReservaResponse>> listarReservasFacultad(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestHeader("X-Facultad-Id") Integer facultadId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(reservaService.listarReservasFacultad(fecha, facultadId, rolUsuario));
    }

    @DeleteMapping("/{idReserva}")
    public ResponseEntity<CancelarReservaResponse> cancelarReserva(
            @PathVariable Integer idReserva,
            @RequestHeader("X-Usuario-Id") String usuarioId,
            @RequestHeader("X-Rol") String rolUsuario) {
        return ResponseEntity.ok(reservaService.cancelarReserva(idReserva, usuarioId, rolUsuario));
    }
}
