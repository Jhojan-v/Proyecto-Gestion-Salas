package com.apiweb.backend.Service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.apiweb.backend.Model.ListaBlancaModel;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Repository.ListaBlancaRepository;

@Service
public class UsuarioServiceImp implements IUsuarioService {

    private final IUsuarioRepository repo;
    private final ListaBlancaRepository listaBlancaRepository;

    public UsuarioServiceImp(IUsuarioRepository repo, ListaBlancaRepository listaBlancaRepository) {
        this.repo = repo;
        this.listaBlancaRepository = listaBlancaRepository;
    }

    @Override
    public String registrar(UsuarioModel usuario) {
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()
                || usuario.getCorreo() == null || usuario.getCorreo().isBlank()
                || usuario.getPassword() == null || usuario.getPassword().isBlank()
                || usuario.getIdFacultad() == null) {
            return "Campos obligatorios vacios";
        }

        String correoNormalizado = usuario.getCorreo().trim().toLowerCase(Locale.ROOT);
        usuario.setCorreo(correoNormalizado);

        if (!correoNormalizado.endsWith("@uao.edu.co")) {
            return "Correo no institucional";
        }

        String password = usuario.getPassword();
        if (password.length() < 8
                || !password.matches(".*[A-Z].*")
                || !password.matches(".*[^a-zA-Z0-9].*")) {
            return "Contrasena invalida";
        }

        if (repo.findByCorreo(correoNormalizado) != null) {
            return "Correo ya registrado";
        }

        ListaBlancaModel correoAutorizado = listaBlancaRepository
                .findByCorreoAutorizadoIgnoreCaseAndActivoTrue(correoNormalizado)
                .orElse(null);

        if (correoAutorizado != null) {
            usuario.setRol("SECRETARIA");
            repo.save(usuario);
            listaBlancaRepository.delete(correoAutorizado);
            return "Registro exitoso - Rol: " + usuario.getRol();
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
