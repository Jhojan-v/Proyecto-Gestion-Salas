package com.apiweb.backend.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 255)
    private String password;

    @Column(name = "rol", nullable = false, length = 20)
    private String rol;

    @Column(name = "id_facultad", nullable = false)
    private Integer idFacultad;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Integer getId() {
        return idUsuario;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Integer getUsuarioId() {
        return idUsuario;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Integer getFacultadId() {
        return idFacultad;
    }
}
