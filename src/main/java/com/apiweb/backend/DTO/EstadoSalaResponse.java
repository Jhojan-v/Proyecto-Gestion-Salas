package com.apiweb.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoSalaResponse {

    private Integer idSala;
    private boolean habilitada;
    private String mensaje;
    private Integer reservaCanceladaId;
}
