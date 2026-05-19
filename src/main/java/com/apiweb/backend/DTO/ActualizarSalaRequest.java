package com.apiweb.backend.DTO;

import jakarta.validation.constraints.Max;
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
public class ActualizarSalaRequest {

    @NotBlank(message = "El nombre de la sala es obligatorio")
    @Size(max = 100, message = "El nombre de la sala no puede superar 100 caracteres")
    private String nombre;

    @NotBlank(message = "La ubicacion de la sala es obligatoria")
    @Size(max = 120, message = "La ubicacion no puede superar 120 caracteres")
    private String ubicacion;

    @NotNull(message = "La capacidad es obligatoria")
    @Min(value = 2, message = "La capacidad debe estar entre 2 y 100")
    @Max(value = 100, message = "La capacidad debe estar entre 2 y 100")
    private Integer capacidad;
}
