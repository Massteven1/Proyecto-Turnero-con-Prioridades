package com.banco.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.banco.app.dto.request.LoginRequestDTO;
import com.banco.app.dto.response.AuthResponseDTO;
import com.banco.app.entity.RolUsuario;
import com.banco.app.entity.Usuario;
import com.banco.app.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

// Regresion del hallazgo de enumeracion de cuentas: usuario inexistente,
// contrasena incorrecta y cuenta inactiva deben responder EXACTAMENTE el mismo
// 401. Distinguir "cuenta inactiva" permitia confirmar usernames validos sin
// conocer la contrasena (y ademas BCrypt no se ejecutaba, delatando por tiempo).
class AuthServiceTest {

    private static final String CLAVE_CORRECTA = "ClaveSegura123!";
    private static final String MENSAJE_GENERICO = "Credenciales inválidas";

    private UsuarioRepository usuarioRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(usuarioRepository, passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("un username inexistente devuelve el 401 generico")
    void usernameInexistenteDevuelveCredencialesInvalidas() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertCredencialesInvalidas(login("fantasma", CLAVE_CORRECTA));
    }

    @Test
    @DisplayName("una contrasena incorrecta devuelve el mismo 401 generico")
    void contrasenaIncorrectaDevuelveCredencialesInvalidas() {
        when(usuarioRepository.findByUsername("operador1")).thenReturn(Optional.of(usuario(true)));

        assertCredencialesInvalidas(login("operador1", "OtraClave123!"));
    }

    @Test
    @DisplayName("una cuenta inactiva con contrasena correcta no revela que la cuenta existe")
    void cuentaInactivaNoRevelaElMotivo() {
        when(usuarioRepository.findByUsername("operador1")).thenReturn(Optional.of(usuario(false)));

        ResponseStatusException error = assertCredencialesInvalidas(login("operador1", CLAVE_CORRECTA));
        assertThat(error.getReason()).doesNotContainIgnoringCase("inactiva");
    }

    @Test
    @DisplayName("una cuenta activa con contrasena correcta recibe el token")
    void cuentaActivaDevuelveToken() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByUsername("operador1")).thenReturn(Optional.of(usuario));
        when(jwtService.generar(any(Usuario.class)))
                .thenReturn(new JwtService.TokenEmitido("token-de-prueba", LocalDateTime.of(2026, 11, 16, 12, 0)));

        AuthResponseDTO respuesta = authService.login(login("operador1", CLAVE_CORRECTA));

        assertThat(respuesta.getToken()).isEqualTo("token-de-prueba");
        assertThat(respuesta.getTipoToken()).isEqualTo("Bearer");
        assertThat(respuesta.getUsername()).isEqualTo("operador1");
    }

    private ResponseStatusException assertCredencialesInvalidas(LoginRequestDTO peticion) {
        ResponseStatusException error = catchThrowableOfType(
                () -> authService.login(peticion), ResponseStatusException.class);

        assertThat(error).isNotNull();
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(error.getReason()).isEqualTo(MENSAJE_GENERICO);
        return error;
    }

    private static LoginRequestDTO login(String username, String password) {
        LoginRequestDTO peticion = new LoginRequestDTO();
        peticion.setUsername(username);
        peticion.setPassword(password);
        return peticion;
    }

    private Usuario usuario(boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setUsername("operador1");
        usuario.setNombreCompleto("Operador Uno");
        usuario.setPasswordHash(passwordEncoder.encode(CLAVE_CORRECTA));
        usuario.setRol(RolUsuario.OPERADOR);
        usuario.setActivo(activo);
        return usuario;
    }
}
