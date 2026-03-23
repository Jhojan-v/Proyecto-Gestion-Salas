package com.apiweb.backend.Controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Service.IUsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:5173")
public class UsuarioController {

    private final IUsuarioService usuarioService;

    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registrar")
    public String registrar(@RequestBody UsuarioModel usuario) {
        return usuarioService.registrar(usuario);
    }

    @PostMapping("/login")
    public UsuarioModel login(
            @RequestParam String correo,
            @RequestParam String password) {
        return usuarioService.login(correo, password);
    }
}
