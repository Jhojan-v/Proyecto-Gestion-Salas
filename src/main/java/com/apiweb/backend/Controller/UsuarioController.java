package com.apiweb.backend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
    public UsuarioModel login(@RequestParam String email,
                              @RequestParam String password) {
        return usuarioService.login(email, password);
    }

}