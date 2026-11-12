package com.banco.app.service;

import com.banco.app.dto.request.LoginRequestDTO;
import com.banco.app.dto.response.AuthResponseDTO;
import com.banco.app.entity.Usuario;
import com.banco.app.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// Autenticacion del personal bancario (contrato POST /api/v1/auth/login):
// solo lectura sobre Usuario y verificacion BCrypt del passwordHash.
// El mensaje de 401 es unico para credenciales invalidas, usuario inexistente
// o cuenta inactiva: no se filtra informacion de cual fue el motivo.
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO peticion) {
        Usuario usuario = usuarioRepository.findByUsername(peticion.getUsername()).orElse(null);

        if (usuario == null || !passwordEncoder.matches(peticion.getPassword(), usuario.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // La baja logica impide el login aunque la contrasena sea correcta
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta está inactiva");
        }

        return AuthResponseDTO.de(usuario, jwtService.generar(usuario));
    }
}
