package com.banco.app.service;

import com.banco.app.dto.request.LoginRequestDTO;
import com.banco.app.dto.response.AuthResponseDTO;
import com.banco.app.entity.Usuario;
import com.banco.app.repository.UsuarioRepository;
import com.banco.app.security.LoginThrottle;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Autenticacion del personal bancario (contrato POST /api/v1/auth/login):
// solo lectura sobre Usuario y verificacion BCrypt del passwordHash.
// Un unico 401 ("Credenciales invalidas") cubre usuario inexistente,
// contrasena incorrecta y cuenta inactiva: no se filtra cual fue el motivo.
@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginThrottle loginThrottle;

    // Hash senuelo: cuando el username no existe se compara igual contra BCrypt
    // para que el tiempo de respuesta no delate la existencia de la cuenta.
    private final String hashSenuelo;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
            LoginThrottle loginThrottle) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginThrottle = loginThrottle;
        this.hashSenuelo = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO peticion) {
        // Antes que nada: si el username agoto sus intentos, 429 sin tocar la base.
        loginThrottle.verificarPermitido(peticion.getUsername());

        Usuario usuario = usuarioRepository.findByUsername(peticion.getUsername()).orElse(null);

        String hash = usuario != null && usuario.getPasswordHash() != null
                ? usuario.getPasswordHash()
                : hashSenuelo;
        boolean coincide = passwordEncoder.matches(peticion.getPassword(), hash);

        // La baja logica impide el login aunque la contrasena sea correcta, pero
        // responde el mismo mensaje: distinguir "cuenta inactiva" permitiria
        // enumerar cuentas validas sin conocer la contrasena.
        if (usuario == null || !coincide || !Boolean.TRUE.equals(usuario.getActivo())) {
            loginThrottle.registrarFallo(peticion.getUsername());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS);
        }

        loginThrottle.registrarExito(peticion.getUsername());
        return AuthResponseDTO.de(usuario, jwtService.generar(usuario));
    }
}
