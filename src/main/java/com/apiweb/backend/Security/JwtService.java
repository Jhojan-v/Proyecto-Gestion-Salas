package com.apiweb.backend.Security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.apiweb.backend.Model.UsuarioModel;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${security.jwt.secret:gestion-salas-jwt-secret-key-uao-2026-segura-y-larga}") String secret,
            @Value("${security.jwt.expiration-ms:86400000}") long expirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generarToken(UsuarioModel usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getCorreo())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plusMillis(expirationMs)))
                .claims(Map.of(
                        "idUsuario", usuario.getIdUsuario(),
                        "rol", usuario.getRol(),
                        "idFacultad", usuario.getIdFacultad()))
                .signWith(secretKey)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerCorreo(String token) {
        return extraerClaims(token).getSubject();
    }

    public UsuarioAutenticado extraerUsuario(String token) {
        Claims claims = extraerClaims(token);
        return new UsuarioAutenticado(
                claims.get("idUsuario", Integer.class),
                claims.getSubject(),
                claims.get("rol", String.class),
                claims.get("idFacultad", Integer.class));
    }
}
