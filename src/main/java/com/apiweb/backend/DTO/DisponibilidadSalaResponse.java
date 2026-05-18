package com.apiweb.backend.DTO;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisponibilidadSalaResponse {

    private Integer idSala;
    private String nombreSala;
    private LocalDate fecha;
    private List<HorarioDisponibleResponse> horariosDisponibles;
    private String franjaHorariaPermitida;
    private List<HorarioDisponibleResponse> horariosOcupados;
}
