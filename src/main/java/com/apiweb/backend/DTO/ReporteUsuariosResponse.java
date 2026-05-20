package com.apiweb.backend.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteUsuariosResponse {

    private String mensaje;
    private Integer facultadId;
    private Long totalUsuarios;
    private Long totalReservas;
    private List<ReporteUsuarioResumenResponse> usuarios;
}
