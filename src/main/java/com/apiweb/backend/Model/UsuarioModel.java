package com.apiweb.backend.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class UsuarioModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_usuario;

    // correo del usuario (único)
    @Column(unique = true, nullable = false)
    private String correo;

    //nombre del usuario
    @Column(nullable = false)
    private String nombre;

    // contraseña
    @Column(nullable = false)
    private String password;

    // facultad del usuario
    @Column(nullable = false)
    private String facultad;

    // rol (DOCENTE o SECRETARIA)
    private String rol;

    // ===== GETTERS Y SETTERS =====

    public Integer getId() {
        return id_usuario;
    }

    public void setId(Integer id_usuario) {
        this.id_usuario = id_usuario;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFacultad() {
        return facultad;
    }

    public void setFacultad(String facultad) {
        this.facultad = facultad;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}