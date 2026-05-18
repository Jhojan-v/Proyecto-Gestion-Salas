package com.apiweb.backend.DTO;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HistorialReservasFacultadResponse {

    private String mensaje;
    private List<HistorialReservaResponse> historial;
}
