package com.banco.app.config;

import com.banco.app.repository.UsuarioRepository;
import com.banco.app.security.JsonAccessDeniedHandler;
import com.banco.app.security.JsonAuthenticationEntryPoint;
import com.banco.app.security.JwtAccountValidationFilter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Seguridad del API: sesion stateless con JWT firmado (HS256).
// - Rutas publicas: POST /api/v1/auth/login y GET /api/v1/publico/pantalla.
// - Todo lo demas exige JWT valido; los roles se aplican con @PreAuthorize
//   en cada controller (contrato del catalogo de endpoints del xlsx).
// - El token trae sub=usuarioId, claim "rol" y exp. El filtro JwtAccountValidationFilter
//   revalida en cada peticion protegida que la cuenta siga activa y su rol vigente.
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String secreto;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, UsuarioRepository usuarioRepository, ObjectMapper objectMapper) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/publico/pantalla").permitAll()
                // Swagger UI y el contrato OpenAPI son documentacion publica
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                .authenticationEntryPoint(new JsonAuthenticationEntryPoint(objectMapper))
                .accessDeniedHandler(new JsonAccessDeniedHandler(objectMapper)))
            // Despues de validar la firma del JWT, el filtro revalida la cuenta
            // (existencia, activo y rol vigente) contra la base en cada peticion
            .addFilterAfter(new JwtAccountValidationFilter(usuarioRepository, objectMapper), BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    // Deja que el frontend Angular (puerto 4200) pueda llamar a este backend (puerto 8080)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(List.of("http://localhost:4200"));
        configuracion.addAllowedMethod("*");
        configuracion.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuracion);
        return source;
    }

    // El encoder que sabe hashear y verificar contrasenas con BCrypt
    // (compatible con los hashes $2a$10$ de los datos semilla)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(clave()).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return NimbusJwtEncoder.withSecretKey(clave()).build();
    }

    // Clave HMAC-SHA256 a partir del secreto configurado (JWT_SECRET)
    private SecretKey clave() {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    // Traduce el claim "rol" del token a la autoridad ROLE_ADMIN / ROLE_OPERADOR
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
