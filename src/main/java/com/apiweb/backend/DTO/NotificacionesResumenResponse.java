package com.apiweb.backend.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionesResumenResponse {

    private long totalNoLeidas;
    private List<NotificacionResponse> notificaciones;
}
