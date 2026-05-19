package com.apiweb.backend.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarEstadoSalaRequest {

    @NotNull(message = "El estado habilitada es obligatorio")
    private Boolean habilitada;
}
