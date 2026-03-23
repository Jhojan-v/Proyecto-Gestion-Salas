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
@Table(name = "recurso_tecnologico")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecursoTecnologicoModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_recurso")
    private Integer idRecurso;

    @Column(name = "codigo_recurso", nullable = false, length = 40)
    private String codigoRecurso;

    @Column(name = "nombre_recurso", nullable = false, length = 100)
    private String nombreRecurso;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;
}
