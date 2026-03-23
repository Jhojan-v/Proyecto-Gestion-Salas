package com.apiweb.backend.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sala_recurso")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecursoSalaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sala_recurso")
    private Integer idRecursoSala;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sala")
    private SalaModel sala;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_recurso")
    private RecursoTecnologicoModel recurso;

    @Column(nullable = false)
    private Integer cantidad;
}
