package com.apiweb.backend.Controller;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.apiweb.backend.DTO.LoginRequest;
import com.apiweb.backend.Exception.BusinessException;
import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;
import com.apiweb.backend.Service.IUsuarioService;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*"}, allowCredentials = "true")
public class UsuarioController {

    private final IUsuarioService usuarioService;
    private final IUsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public UsuarioController(
            IUsuarioService usuarioService,
            IUsuarioRepository usuarioRepository,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    @PostMapping("/registrar")
    public String registrar(@RequestBody UsuarioModel usuario) {
        return usuarioService.registrar(usuario);
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioModel> login(
            @RequestBody(required = false) LoginRequest request,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String password,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String correoLogin = request != null && request.getCorreo() != null ? request.getCorreo() : correo;
        String passwordLogin = request != null && request.getPassword() != null ? request.getPassword() : password;

        if (correoLogin == null || correoLogin.isBlank() || passwordLogin == null || passwordLogin.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Correo y password son obligatorios");
        }

        String correoNormalizado = correoLogin.trim().toLowerCase(Locale.ROOT);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(correoNormalizado, passwordLogin));

            sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, httpRequest, httpResponse);
        } catch (AuthenticationException exception) {
            securityContextHolderStrategy.clearContext();
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos");
        }

        UsuarioModel usuario = usuarioRepository.findByCorreo(correoNormalizado);
        return ResponseEntity.ok(usuario);
    }
}
