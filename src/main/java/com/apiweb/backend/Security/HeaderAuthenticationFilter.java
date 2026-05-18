package com.apiweb.backend.Security;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.apiweb.backend.Model.UsuarioModel;
import com.apiweb.backend.Repository.IUsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final IUsuarioRepository usuarioRepository;

    public HeaderAuthenticationFilter(IUsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/")
                || "/api/health".equals(path)
                || "/api/usuarios/login".equals(path)
                || "/api/usuarios/registrar".equals(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsuarioModel usuario = resolverUsuario(request.getHeader("X-Usuario-Id"));
            if (usuario != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                usuario.getCorreo(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private UsuarioModel resolverUsuario(String usuarioHeader) {
        if (usuarioHeader == null || usuarioHeader.isBlank()) {
            return null;
        }

        String valor = usuarioHeader.trim();
        try {
            Integer idUsuario = Integer.valueOf(valor);
            return usuarioRepository.findById(idUsuario).orElse(null);
        } catch (NumberFormatException exception) {
            return usuarioRepository.findByCorreo(valor.toLowerCase(Locale.ROOT));
        }
    }
}
