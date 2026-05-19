package com.apiweb.backend.Service;

import com.apiweb.backend.Model.UsuarioModel;

public interface IUsuarioService {

    String registrar(UsuarioModel usuario);

    UsuarioModel login(String correo, String password);
}
