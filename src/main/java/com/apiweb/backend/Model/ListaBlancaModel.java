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
@Table(name = "lista_blanca")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListaBlancaModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lista")
    private Integer idLista;

    @Column(name = "correo_autorizado", nullable = false, unique = true, length = 150)
    private String correoAutorizado;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
