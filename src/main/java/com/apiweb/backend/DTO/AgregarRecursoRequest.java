package com.apiweb.backend.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgregarRecursoRequest {

    @NotBlank(message = "El codigo del recurso es obligatorio")
    @Size(max = 40, message = "El codigo del recurso no puede superar 40 caracteres")
    private String codigoRecurso;

    @NotBlank(message = "El nombre del recurso es obligatorio")
    @Size(max = 100, message = "El nombre del recurso no puede superar 100 caracteres")
    private String nombreRecurso;

    @NotNull(message = "La cantidad del recurso es obligatoria")
    @Min(value = 1, message = "La cantidad del recurso debe ser mayor o igual a 1")
    private Integer cantidad;

    @Size(max = 255, message = "La descripcion no puede superar 255 caracteres")
    private String descripcion;
}
