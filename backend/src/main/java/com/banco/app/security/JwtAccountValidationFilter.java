package com.banco.app.security;

import com.banco.app.entity.Usuario;
import com.banco.app.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

// Requisito central de la documentacion: en CADA peticion protegida se vuelve
// a consultar la cuenta actual y se rechaza con 401 si:
//   1) el usuarioId del claim sub ya no existe,
//   2) la cuenta esta inactiva (activo=false),
//   3) el rol vigente no coincide con el claim "rol" del token.
// Asi una desactivacion o un cambio de rol invalida los tokens anteriores
// aunque su firma siga siendo valida, sin necesidad de lista de revocacion.
// Corre DESPUES de BearerTokenAuthenticationFilter (aqui ya hay JWT autenticado).
public class JwtAccountValidationFilter extends OncePerRequestFilter {

    private final UsuarioRepository usuarioRepository;
    private final ObjectMapper objectMapper;

    public JwtAccountValidationFilter(UsuarioRepository usuarioRepository, ObjectMapper objectMapper) {
        this.usuarioRepository = usuarioRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain cadena)
            throws ServletException, IOException {

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        // Solo actua sobre peticiones con JWT; las rutas publicas pasan sin mas
        if (autenticacion instanceof JwtAuthenticationToken tokenJwt) {
            if (cuentaInvalida(tokenJwt, request, response)) {
                SecurityContextHolder.clearContext();
                return;
            }
        }

        cadena.doFilter(request, response);
    }

    private boolean cuentaInvalida(JwtAuthenticationToken tokenJwt, HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String sub = tokenJwt.getToken().getSubject();
        String rolDelClaim = tokenJwt.getToken().getClaimAsString("rol");

        Long usuarioId;
        try {
            usuarioId = Long.parseLong(sub);
        } catch (NumberFormatException ex) {
            // sub alterado o mal formado: el token se considera invalido
            JsonErrorWriter.escribir(response, objectMapper, HttpStatus.UNAUTHORIZED, "Token inválido", request.getRequestURI());
            return true;
        }

        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);

        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            JsonErrorWriter.escribir(response, objectMapper, HttpStatus.UNAUTHORIZED, "La cuenta no existe o está inactiva", request.getRequestURI());
            return true;
        }

        if (rolDelClaim == null || !usuario.getRol().name().equals(rolDelClaim)) {
            JsonErrorWriter.escribir(response, objectMapper, HttpStatus.UNAUTHORIZED, "El rol de la cuenta cambió; vuelva a iniciar sesión", request.getRequestURI());
            return true;
        }

        return false;
    }
}
