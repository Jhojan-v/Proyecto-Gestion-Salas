package com.apiweb.backend.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaDetalleResponse {

    private Integer idSala;
    private String nombre;
    private String ubicacion;
    private Integer capacidad;
    private Integer facultadId;
    private boolean habilitada;
    private List<RecursoSalaResponse> recursos;
}
