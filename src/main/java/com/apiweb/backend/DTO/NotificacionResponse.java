package com.apiweb.backend.DTO;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionResponse {

    private Integer idNotificacion;
    private String tipo;
    private String titulo;
    private String mensaje;
    private String entidad;
    private Long entidadId;
    private boolean leida;
    private LocalDateTime fechaCreacion;
}
