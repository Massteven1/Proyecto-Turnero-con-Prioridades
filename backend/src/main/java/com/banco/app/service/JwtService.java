package com.banco.app.service;

import com.banco.app.config.ZonaHoraria;
import com.banco.app.entity.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

// Emite el JWT firmado (HS256) con la identidad exigida por la documentacion:
// sub = usuarioId, claim "rol" y exp con vigencia configurable (app.jwt.vigencia-ms).
// Emision sin estado: no se guarda nada del token en el servidor.
@Service
public class JwtService {

    // Registro que evita el desfase entre el exp real del token y la
    // fecha mostrada al usuario en AuthResponseDTO.expiracion
    public record TokenEmitido(String token, LocalDateTime expiracion) {
    }

    private final JwtEncoder encoder;
    private final Duration vigencia;

    public JwtService(JwtEncoder encoder, @Value("${app.jwt.vigencia-ms}") Long milisegundos) {
        this.encoder = encoder;
        this.vigencia = Duration.ofMillis(milisegundos);
    }

    public TokenEmitido generar(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(vigencia);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(usuario.getId()))
                .claim("rol", usuario.getRol().name())
                .issuedAt(ahora)
                .expiresAt(expira)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new TokenEmitido(token, LocalDateTime.ofInstant(expira, ZonaHoraria.ZONA));
    }

    public Duration getVigencia() {
        return vigencia;
    }
}
