package com.apiweb.backend.Security;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.apiweb.backend.Exception.BusinessException;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;

@Component
public class UsuarioContext {

    private final IUsuarioRepository usuarioRepository;

    public UsuarioContext(IUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Integer resolverIdUsuario(String headerUsuario) {
        if (headerUsuario == null || headerUsuario.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El encabezado X-Usuario-Id es obligatorio");
        }
        String valor = headerUsuario.trim();
        try {
            Integer id = Integer.valueOf(valor);
            if (!usuarioRepository.existsById(id)) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no registrado en el sistema");
            }
            return id;
        } catch (NumberFormatException ex) {
            UsuarioModel usuario = usuarioRepository.findByCorreo(valor.toLowerCase(Locale.ROOT));
            if (usuario != null) {
                return usuario.getIdUsuario();
            }
            throw new BusinessException(HttpStatus.BAD_REQUEST, "X-Usuario-Id debe ser un valor valido");
        }
    }
}
