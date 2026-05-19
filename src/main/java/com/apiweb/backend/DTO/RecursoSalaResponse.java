package com.apiweb.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecursoSalaResponse {

    private Integer idRecursoSala;
    private String codigoRecurso;
    private String nombreRecurso;
    private Integer cantidad;
    private String mensaje;
}
