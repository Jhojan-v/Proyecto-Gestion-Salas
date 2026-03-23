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
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "correo", unique = true, nullable = false, length = 150)
    private String correo;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol;

    @Column(name = "id_facultad", nullable = false)
    private Integer idFacultad;
}
