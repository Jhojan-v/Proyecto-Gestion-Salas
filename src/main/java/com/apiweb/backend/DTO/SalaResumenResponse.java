package com.apiweb.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaResumenResponse {

    private Integer idSala;
    private String nombre;
    private String ubicacion;
    private Integer capacidad;
    private boolean habilitada;
}
