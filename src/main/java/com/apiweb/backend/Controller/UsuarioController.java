package com.apiweb.backend.Controller;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.DTO.AuthResponse;
import com.apiweb.backend.DTO.LoginRequest;
import com.apiweb.backend.Exception.BusinessException;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Security.JwtService;
import com.apiweb.backend.Service.IUsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final IUsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public UsuarioController(
            IUsuarioService usuarioService,
            IUsuarioRepository usuarioRepository,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/registrar")
    public String registrar(@RequestBody UsuarioModel usuario) {
        return usuarioService.registrar(usuario);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody(required = false) LoginRequest request,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String password) {
        String correoLogin = request != null && request.getCorreo() != null ? request.getCorreo() : correo;
        String passwordLogin = request != null && request.getPassword() != null ? request.getPassword() : password;

        if (correoLogin == null || correoLogin.isBlank() || passwordLogin == null || passwordLogin.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Correo y password son obligatorios");
        }

        String correoNormalizado = correoLogin.trim().toLowerCase(Locale.ROOT);

        try {
            Authentication ignored = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(correoNormalizado, passwordLogin));
        } catch (AuthenticationException exception) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos");
        }

        UsuarioModel usuario = usuarioRepository.findByCorreo(correoNormalizado);
        return ResponseEntity.ok(new AuthResponse(
                jwtService.generarToken(usuario),
                "Bearer",
                usuario));
    }
}
