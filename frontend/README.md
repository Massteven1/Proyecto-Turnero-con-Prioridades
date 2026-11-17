# Frontend Angular: Turnos de sucursal

Aplicación de una sola página (Angular 22, componentes standalone, señales y detección de cambios sin zone.js) que consume la API REST del backend en `http://localhost:8080/api/v1`. El backend solo acepta el origen `http://localhost:4200` por CORS.

## Ejecución

```powershell
npm install
npm start          # ng serve en http://localhost:4200
npm run build      # build de producción en dist/frontend
npx ng test --watch=false
```

La URL de la API está en `src/app/services/api-url.ts` (`API_URL`).

## Vistas

| Ruta | Acceso | Vista |
|---|---|---|
| `/login` | Público | Ingreso del personal: usuario, contraseña, "Recordar usuario" y vista previa de la sala de espera con acceso a la pantalla pública |
| `/pantalla` | Público | Pantalla de sala de espera: turno llamado, turnos en ventanillas y próximos turnos por servicio; se actualiza cada 5 segundos |
| `/operacion` | OPERADOR, ADMIN | Ventanilla: elegir módulo, llamar el siguiente turno con un solo botón, iniciar, finalizar, cancelar o marcar ausente con motivo, cola proyectada con cancelación por fila, registro de turnos nuevos y estado de la proporción N:1 |
| `/admin/servicios` | ADMIN | Alta, edición y baja lógica de servicios (prefijo inmutable) |
| `/admin/modulos` | ADMIN | Alta, edición y baja lógica de ventanillas; bloquea desactivar módulos ocupados y activar módulos de servicios inactivos |
| `/admin/categorias` | ADMIN | Renombrar las cuatro categorías base (prioridad y estado fijos) |
| `/admin/parametros` | ADMIN | Espera máxima (1 a 240 min) y proporción (1 a 10) por servicio |
| `/admin/usuarios` | ADMIN | Alta y edición de cuentas; contraseña opcional al editar |
| `/historial` | ADMIN | Filtros por fecha, servicio y estado, indicadores de la jornada y cajón de detalle con las transiciones; desde el detalle el ADMIN resuelve turnos abiertos |

Después del login, ADMIN entra a `/admin` y OPERADOR a `/operacion`. Los guards (`src/app/guard/auth-guard.ts`) solo ordenan la navegación: Spring Security vuelve a validar cada permiso.

## Sesión y seguridad

- El JWT se guarda en `sessionStorage` (dura lo que la pestaña) junto con rol, nombre y `expiracion`. "Recordar usuario" guarda solo el username en `localStorage`.
- Al llegar la `expiracion` la sesión se cierra y el login muestra "Tu sesión expiró". Un 401 en una ruta protegida (cuenta desactivada o rol cambiado) también cierra la sesión.
- `authInterceptor` agrega `Authorization: Bearer` solo a la API y **excluye** `/auth/login` y `/publico/pantalla`: la pantalla pública nunca recibe el token y un 401 suyo no cierra la sesión (`auth-interceptor.spec.ts`).
- Cerrar sesión elimina el token del cliente; el JWT emitido sigue siendo válido hasta su `exp` porque el alcance no incluye lista de revocación.

## Recuperar el turno en curso

`ModuloDTO` incluye `turnoActivoId` además de `turnoActivoCodigo`, un campo que se agregó al contrato del backend. Al abrir la ventanilla, el panel consulta `GET /turnos/{id}` de los módulos ocupados y retoma el turno `LLAMADO` o `EN_ATENCION` de la cuenta. Así, recargar la página o volver a iniciar sesión no deja la ventanilla bloqueada.

## Diseño

- Paleta de papel de seguridad (`--papel #ebf0e8`), tinta azul pizarra (`--tinta #1b2838`), sello violeta para llamar (`--sello #573a9c`), ámbar para la prioridad y para los códigos de la pantalla pública (`--led #f2b035`).
- Tipografía Atkinson Hyperlegible, creada para personas con baja visión: la versión Next para la interfaz y la Mono para los códigos de turno, donde `0/O` y `1/I` deben distinguirse. Las fuentes se sirven desde `node_modules` (`@fontsource-variable`), sin CDN.
- El elemento propio del sistema es la ficha de turno troquelada (`.ticket`, `.corte`): aparece en la ventanilla, en el turno emitido, en el login y en la pantalla pública. El consecutivo del código va en negrita porque es lo que el cliente compara con su ficha.
- Estilos compartidos en `src/styles.css`. Las vistas ajustan el código con las variables `--codigo-tamano`, `--codigo-color` y `--codigo-serie`.
