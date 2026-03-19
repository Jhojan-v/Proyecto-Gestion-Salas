package com.apiweb.backend.Service;

import com.apiweb.backend.Model.UsuarioModel;

public interface IUsuarioService {
    //metodo para registrar usuario
    String registrar(UsuarioModel usuario);

    //metodo para login
    UsuarioModel login(String email, String password);

}
