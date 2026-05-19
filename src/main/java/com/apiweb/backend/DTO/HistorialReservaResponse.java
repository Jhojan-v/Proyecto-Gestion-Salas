package com.apiweb.backend.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialReservaResponse {

    private Integer idReserva;
    private Integer idSala;
    private String nombreSala;
    private Integer idUsuario;
    private String nombreProfesor;
    private String correoProfesor;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String estado;

    @JsonProperty("fechaHoraInicio")
    public String getFechaHoraInicio() {
        return fecha == null || horaInicio == null ? null : LocalDateTime.of(fecha, horaInicio).toString();
    }

    @JsonProperty("fechaHoraFin")
    public String getFechaHoraFin() {
        return fecha == null || horaFin == null ? null : LocalDateTime.of(fecha, horaFin).toString();
    }
}
