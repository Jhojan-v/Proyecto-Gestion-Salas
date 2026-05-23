package com.apiweb.backend.Security;

public record UsuarioAutenticado(
        Integer idUsuario,
        String correo,
        String rol,
        Integer idFacultad) {
}
