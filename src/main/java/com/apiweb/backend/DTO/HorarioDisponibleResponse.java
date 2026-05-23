package com.apiweb.backend.DTO;

import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HorarioDisponibleResponse {

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String estado;
    private Boolean disponible;
}
