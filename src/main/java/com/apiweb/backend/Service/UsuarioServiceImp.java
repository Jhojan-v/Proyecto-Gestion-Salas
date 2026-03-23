package com.apiweb.backend.Service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;

@Service
public class UsuarioServiceImp implements IUsuarioService {

    private static final Set<String> CORREOS_SECRETARIA = Set.of(
            "sec1@uao.edu.co",
            "sec2@uao.edu.co",
            "secretaria.ingenieria@uao.edu.co");

    private final IUsuarioRepository repo;

    public UsuarioServiceImp(IUsuarioRepository repo) {
        this.repo = repo;
    }

    @Override
    public String registrar(UsuarioModel usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()
                || usuario.getCorreo() == null || usuario.getCorreo().isBlank()
                || usuario.getPassword() == null || usuario.getPassword().isBlank()
                || usuario.getIdFacultad() == null) {
            return "Campos obligatorios vacios";
        }

        if (!usuario.getCorreo().endsWith("@uao.edu.co")) {
            return "Correo no institucional";
        }

        String password = usuario.getPassword();
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[^a-zA-Z0-9].*")) {
            return "Contrasena invalida";
        }

        if (repo.findByCorreo(usuario.getCorreo()) != null) {
            return "Correo ya registrado";
        }

        if (CORREOS_SECRETARIA.contains(usuario.getCorreo().toLowerCase())) {
            return "Las secretarias no pueden registrarse.";
        }

        usuario.setRol("DOCENTE");
        repo.save(usuario);
        return "Registro exitoso - Rol: " + usuario.getRol();
    }

    @Override
    public UsuarioModel login(String correo, String password) {
        UsuarioModel user = repo.findByCorreo(correo);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}
