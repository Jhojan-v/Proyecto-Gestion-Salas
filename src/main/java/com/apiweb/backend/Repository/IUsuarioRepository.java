package com.apiweb.backend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.apiweb.backend.Model.UsuarioModel;

public interface IUsuarioRepository extends JpaRepository<UsuarioModel, Integer> {

    UsuarioModel findByCorreo(String correo);
}
