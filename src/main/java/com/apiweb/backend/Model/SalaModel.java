package com.apiweb.backend.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sala")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sala")
    private Integer idSala;

    @Column(name = "nombre_sala", nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 120)
    private String ubicacion;

    @Column(nullable = false)
    private Integer capacidad;

    @Column(name = "id_facultad", nullable = false)
    private Integer facultadId;

    @Column(name = "estado", nullable = false)
    private boolean habilitada = true;
}
