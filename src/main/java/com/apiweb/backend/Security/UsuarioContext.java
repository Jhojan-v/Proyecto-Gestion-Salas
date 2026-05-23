package com.apiweb.backend.Security;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        UsuarioAutenticado usuarioAutenticado = obtenerAutenticado();
        if (usuarioAutenticado != null) {
            return usuarioAutenticado.idUsuario();
        }
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

    public Integer resolverFacultadId(Integer headerFacultadId) {
        UsuarioAutenticado usuarioAutenticado = obtenerAutenticado();
        if (usuarioAutenticado != null) {
            return usuarioAutenticado.idFacultad();
        }
        if (headerFacultadId == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El encabezado X-Facultad-Id es obligatorio");
        }
        return headerFacultadId;
    }

    public String resolverRol(String headerRol) {
        UsuarioAutenticado usuarioAutenticado = obtenerAutenticado();
        if (usuarioAutenticado != null) {
            return usuarioAutenticado.rol();
        }
        if (headerRol == null || headerRol.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El encabezado X-Rol es obligatorio");
        }
        return headerRol.trim().toUpperCase(Locale.ROOT);
    }

    public Integer resolverIdUsuarioActual() {
        return resolverIdUsuario(null);
    }

    private UsuarioAutenticado obtenerAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UsuarioAutenticado usuarioAutenticado) {
            return usuarioAutenticado;
        }
        return null;
    }
}
