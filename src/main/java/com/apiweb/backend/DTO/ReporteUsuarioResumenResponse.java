package com.apiweb.backend.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteUsuarioResumenResponse {

    private Integer idUsuario;
    private String nombreUsuario;
    private String correoUsuario;
    private String rolUsuario;
    private Integer facultadId;
    private Long totalReservas;
}
