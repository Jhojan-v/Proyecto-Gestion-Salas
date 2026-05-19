package com.apiweb.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.apiweb.backend.Model.ListaBlancaModel;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Repository.ListaBlancaRepository;
import com.apiweb.backend.Service.IUsuarioService;

@SpringBootTest
class UsuarioServiceIntegrationTest {

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private ListaBlancaRepository listaBlancaRepository;

    @BeforeEach
    void limpiarDatos() {
        usuarioRepository.deleteAll();
        listaBlancaRepository.deleteAll();
    }

    @Test
    void debeRegistrarDocenteCuandoCorreoNoEstaEnListaBlanca() {
        UsuarioModel usuario = new UsuarioModel(null, "Docente Demo", "docente.demo@uao.edu.co",
                "ClaveSegura1!", null, 1);

        String respuesta = usuarioService.registrar(usuario);

        UsuarioModel guardado = usuarioRepository.findByCorreo("docente.demo@uao.edu.co");
        assertEquals("Registro exitoso - Rol: DOCENTE", respuesta);
        assertNotNull(guardado);
        assertEquals("DOCENTE", guardado.getRol());
    }

    @Test
    void debeRegistrarSecretariaYEliminarCorreoDeListaBlanca() {
        listaBlancaRepository.save(new ListaBlancaModel(null, "secretaria.ingenieria@uao.edu.co", true));
        UsuarioModel usuario = new UsuarioModel(null, "Secretaria Ingenieria", "secretaria.ingenieria@uao.edu.co",
                "ClaveSegura1!", null, 1);

        String respuesta = usuarioService.registrar(usuario);

        UsuarioModel guardado = usuarioRepository.findByCorreo("secretaria.ingenieria@uao.edu.co");
        assertEquals("Registro exitoso - Rol: SECRETARIA", respuesta);
        assertNotNull(guardado);
        assertEquals("SECRETARIA", guardado.getRol());
        assertFalse(listaBlancaRepository
                .findByCorreoAutorizadoIgnoreCaseAndActivoTrue("secretaria.ingenieria@uao.edu.co")
                .isPresent());
    }
}
