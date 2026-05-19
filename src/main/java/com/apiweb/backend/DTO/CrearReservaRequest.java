package com.apiweb.backend.DTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrearReservaRequest {

    private Integer idSala;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private LocalDateTime fechaHoraInicio;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private LocalDateTime fechaHoraFin;

    public LocalDate resolverFecha() {
        if (fecha != null) {
            return fecha;
        }
        if (fechaHoraInicio != null) {
            return fechaHoraInicio.toLocalDate();
        }
        return null;
    }

    public LocalTime resolverHoraInicio() {
        if (horaInicio != null) {
            return horaInicio;
        }
        if (fechaHoraInicio != null) {
            return fechaHoraInicio.toLocalTime();
        }
        return null;
    }

    public LocalTime resolverHoraFin() {
        if (horaFin != null) {
            return horaFin;
        }
        if (fechaHoraFin != null) {
            return fechaHoraFin.toLocalTime();
        }
        return null;
    }
}
