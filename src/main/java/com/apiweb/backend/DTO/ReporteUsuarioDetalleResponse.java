package com.apiweb.backend.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteUsuarioDetalleResponse {

    private String mensaje;
    private Integer idUsuario;
    private String nombreUsuario;
    private String correoUsuario;
    private String rolUsuario;
    private Integer facultadId;
    private Long totalReservas;
    private Long reservasRealizadas;
    private Long reservasCanceladas;
    private List<HistorialReservaResponse> reservas;
}
