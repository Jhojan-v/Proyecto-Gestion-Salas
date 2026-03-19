package com.apiweb.backend.Service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;

@Service
public class UsuarioServiceImp implements IUsuarioService {

    //reprositorio para acceder a la base de datos
    @Autowired
    private IUsuarioRepository repo;

    //crear registro de usuario
    @Override
    public String registrar(UsuarioModel usuario) {

        //validar los campos vacios
        if (usuario.getCorreo() == null || usuario.getPassword() == null) {
            return "Campos obligatorios vacíos";
        }

        //validar que no hayan campos vacios en facultad
        if (usuario.getFacultad() == null || usuario.getFacultad().isEmpty()) {
            return "la facultad es obligatoria";
        }

        //correo institucional
        if (!usuario.getCorreo().contains("@") || !usuario.getCorreo().contains(".")) {
            return "Formato de correo inválido";
        }

        if (!usuario.getCorreo().endsWith("@uao.edu.co")) {
            return "Correo no institucional";
        }

        //validar contraseña
        String password = usuario.getPassword();
        if (password.length() < 8 ||
            !password.matches(".*[A-Z].*") ||
            !password.matches(".*[^a-zA-Z0-9].*")) {

            return "Contraseña inválida";
        }

        //validar si ya existe
        if (repo.findByEmail(usuario.getCorreo()) != null) {
            return "Correo ya registrado";
        }

        //lista de secretarias
        List<String> secretarias = Arrays.asList(
            "sec1@uao.edu.co",
            "sec2@uao.edu.co"
        );

        //si es secretaria no puede registrarse
        if (secretarias.contains(usuario.getCorreo())) {
            return "Las secretarias no pueden registrarse.";
        }

        //asignar rol automatico
        usuario.setRol("DOCENTE");

        //guardar usuario
        repo.save(usuario);

        return "Registro exitoso - Rol: " + usuario.getRol();
    }

    //despues de crear el usuario se hace el login
    @Override
    public UsuarioModel login(String email, String passwordString) {

        //primero se busca el usuaurio por su coreo
        UsuarioModel user = repo.findByEmail(email);

        //verifica si existe y si coincide con la contraseña
        if (user != null && user.getPassword().equals(passwordString)) {
            return user; //login existoso si se cumple
        }

        //si no se cumple 
        return null;
    }
}
