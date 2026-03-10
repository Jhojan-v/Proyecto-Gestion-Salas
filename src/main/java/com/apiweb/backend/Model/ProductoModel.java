package com.apiweb.backend.Model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table (name="Producto")
@Data
@AllArgsConstructor
@NoArgsConstructor

public class ProductoModel {
    @Id
    private Integer idProducto;
    private String nombre;
    @ManyToOne
    @JoinColumn (name="idCategoria")
    private CategoriaModel categoriaId;
}