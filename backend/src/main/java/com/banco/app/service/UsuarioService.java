package com.banco.app.service;

import com.banco.app.dto.request.UsuarioCreateDTO;
import com.banco.app.dto.request.UsuarioUpdateDTO;
import com.banco.app.dto.response.UsuarioDTO;
import com.banco.app.entity.EstadoTurno;
import com.banco.app.entity.RolUsuario;
import com.banco.app.entity.Usuario;
import com.banco.app.repository.TurnoRepository;
import com.banco.app.repository.UsuarioRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

// CRUD de usuarios del personal (contrato GET/POST/PUT /api/v1/usuarios).
// Reglas de negocio de la documentacion:
// - username unico e inmutable (el PUT no lo recibe) y password siempre BCrypt
// - 409 al desactivar o cambiar el rol de un usuario con turno activo
//   (LLAMADO o EN_ATENCION): primero debe resolverse ese turno
// - 409 al desactivar o degradar al ultimo ADMIN activo, serializando los
//   cambios que afectan ADMIN mediante bloqueo pesimista de sus cuentas
// - la desactivacion es logica (activo=false): nunca se borra el historial
@Service
public class UsuarioService {

    private static final List<EstadoTurno> ESTADOS_ACTIVOS =
            List.of(EstadoTurno.LLAMADO, EstadoTurno.EN_ATENCION);

    private final UsuarioRepository usuarioRepository;
    private final TurnoRepository turnoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, TurnoRepository turnoRepository,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.turnoRepository = turnoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioDTO> listar(Boolean activo) {
        List<Usuario> usuarios = activo == null
                ? usuarioRepository.findAllByOrderByIdAsc()
                : usuarioRepository.findByActivoOrderByIdAsc(activo);
        return usuarios.stream().map(UsuarioDTO::de).toList();
    }

    @Transactional
    public UsuarioDTO crear(UsuarioCreateDTO peticion) {
        // Validacion previa a la restriccion uk_usuarios_username (respaldo en BD)
        if (usuarioRepository.existsByUsername(peticion.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(peticion.getUsername());
        usuario.setNombreCompleto(peticion.getNombreCompleto());
        usuario.setPasswordHash(passwordEncoder.encode(peticion.getPassword()));
        usuario.setRol(peticion.getRol());
        // Todo usuario nuevo nace activo (contrato del endpoint)
        usuario.setActivo(true);

        return UsuarioDTO.de(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioDTO actualizar(Long id, UsuarioUpdateDTO peticion) {
        // Bloqueo del contrato: la cuenta objetivo y, cuando la actualizacion puede
        // reducir la cantidad de ADMIN (degradar o desactivar), todas las cuentas
        // ADMIN activas, en una sola sentencia y en orden ascendente de id.
        boolean puedeReducirAdmins = peticion.getRol() != RolUsuario.ADMIN
                || Boolean.FALSE.equals(peticion.getActivo());

        Usuario usuario = puedeReducirAdmins
                ? bloquearObjetivoConAdmins(id)
                : usuarioRepository.findByIdForUpdate(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        boolean cambiaRol = peticion.getRol() != usuario.getRol();
        boolean seDesactiva = Boolean.TRUE.equals(usuario.getActivo())
                && Boolean.FALSE.equals(peticion.getActivo());

        // 409: desactivar o cambiar el rol de quien tiene un turno LLAMADO/EN_ATENCION.
        // El alcance es deliberado (decision de contrato): nombre y contrasena no
        // afectan la operacion del turno asignado, porque el JWT no caduca por
        // cambiar la clave y la pantalla publica no muestra nombres. Bloquearlos
        // impediria corregir un dato o rotar una credencial durante la atencion.
        // La documentacion usa el mismo criterio para servicios y modulos: se
        // permite renombrar con turnos activos y solo se bloquea desactivar.
        if ((cambiaRol || seDesactiva)
                && turnoRepository.existsByOperadorIdAndEstadoIn(id, ESTADOS_ACTIVOS)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario tiene un turno activo (LLAMADO o EN_ATENCION); resuélvalo antes de modificarlo");
        }

        // 409: proteger al ultimo ADMIN activo. Las cuentas ADMIN ya quedaron
        // bloqueadas por la sentencia de arriba; se vuelve a contar dentro de la
        // transaccion para no degradar al ultimo ADMIN en una carrera.
        boolean degradaOdesactivaAdmin = usuario.getRol() == RolUsuario.ADMIN
                && Boolean.TRUE.equals(usuario.getActivo())
                && (peticion.getRol() != RolUsuario.ADMIN || Boolean.FALSE.equals(peticion.getActivo()));

        if (degradaOdesactivaAdmin) {
            long adminsActivos = usuarioRepository.countByRolAndActivo(RolUsuario.ADMIN, true);
            if (adminsActivos <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "No se puede desactivar o degradar al último ADMIN activo");
            }
        }

        usuario.setNombreCompleto(peticion.getNombreCompleto());
        // password null u omitido conserva el hash actual (contrato)
        if (peticion.getPassword() != null && !peticion.getPassword().isEmpty()) {
            usuario.setPasswordHash(passwordEncoder.encode(peticion.getPassword()));
        }
        usuario.setRol(peticion.getRol());
        usuario.setActivo(peticion.getActivo());

        return UsuarioDTO.de(usuarioRepository.save(usuario));
    }

    // Bloquea la cuenta objetivo junto con las cuentas ADMIN activas y devuelve
    // la objetivo. Si el id no existe, la sentencia igual bloquea las ADMIN y el
    // filtrado no encuentra la cuenta: se responde 404.
    private Usuario bloquearObjetivoConAdmins(Long id) {
        return usuarioRepository.bloquearUsuarioYAdminsActivos(id).stream()
                .filter(cuenta -> cuenta.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
