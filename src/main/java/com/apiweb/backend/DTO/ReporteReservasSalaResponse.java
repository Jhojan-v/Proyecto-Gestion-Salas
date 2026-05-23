package com.apiweb.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteReservasSalaResponse {

    private Integer idSala;
    private String nombreSala;
    private String ubicacion;
    private Integer capacidad;
    private Long totalReservas;
}
