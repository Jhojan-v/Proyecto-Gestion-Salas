package com.apiweb.backend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Service.IUsuarioService;

@RestController //api
@RequestMapping("/api/usuarios")//ruta localhost
@CrossOrigin(origins = "*") // onexión con frontend
public class UsuarioController {

    @Autowired //conecta con service
    private IUsuarioService usuarioService;

    // registro d usuario
    @PostMapping("/registrar")
public String registrar(@RequestBody UsuarioModel usuario) {
    return usuarioService.registrar(usuario);
}

    // login
    @PostMapping("/login")
public UsuarioModel login(@RequestBody UsuarioModel usuario) {
    return usuarioService.login(usuario.getCorreo(), usuario.getPassword());
}
}