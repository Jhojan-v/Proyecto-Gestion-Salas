package com.apiweb.backend.DTO;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteReservasResponse {

    private String mensaje;
    private Integer facultadId;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Long totalReservas;
    private List<ReporteReservasSalaResponse> salas;
}
