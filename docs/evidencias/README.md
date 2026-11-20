# Evidencias de ejecución de las pruebas de aceptación P01 a P12

Ejecución del **2026-11-20** sobre el commit `3d1250b`. Todo lo que resume este reporte sale de los logs guardados en `logs/`, sin editar.

## Resumen

| Ejecución | Resultado | Log | Captura |
|---|---|---|---|
| Carpeta 9 de Postman (P01 a P12) | 14 peticiones, 88 aserciones, 0 fallidas, 0 omitidas | [postman-p01-p12.log](logs/postman-p01-p12.log) | [resumen](capturas/p01-p12-resumen.png) |
| Colección completa de Postman | 157 peticiones, 518 aserciones, 0 fallidas, 0 omitidas | [postman-coleccion-completa.log](logs/postman-coleccion-completa.log) | [resumen](capturas/coleccion-completa.png) |
| JUnit del backend (`mvn test`) | 31 pruebas, 0 fallas, 0 errores, 0 omitidas | [junit-mvn-test.log](logs/junit-mvn-test.log) | [resumen](capturas/junit.png) |

## Entorno

- Backend: Spring Boot 4.1.2 con JDK 26.0.1, el jar construido desde el commit indicado (`mvn package`), en el puerto 8099 para no interferir con la instancia de desarrollo.
- Base de datos: MariaDB 10.4.32 de XAMPP, base aislada creada con `db/banco_turnero_db.sql` justo antes de cada corrida y eliminada al terminar.
- Postman CLI 1.74.0: `postman collection run "postman/collections/Turnero con Prioridades — API"`, con `-i "9. Pruebas de aceptación (P01 a P12)"` para la carpeta de aceptación.

## Cómo se cubrieron los casos que necesitan preparación

- **P06, turno EN_ESPERA de un día anterior.** La API no permite crear turnos con otra fecha, como exige el documento. Antes de la corrida se insertó por SQL en la base de prueba el turno `ASE-<día anterior>-001` en `EN_ESPERA`. Sin ese turno la aserción queda omitida en vez de fallar.
- **P08, token vencido.** Se pasó `--env-var jwtSecret=<secreto del backend de prueba>`, para que el script firme un JWT con `exp` en el pasado. El secreto es solo de prueba y no aparece en los logs.
- **P01 y P10, persistencia tras reiniciar.** La carpeta corrió con `--delay-request 30000`. Al terminar P12 se detuvo el proceso del backend y se inició uno nuevo con la misma base; la última petición («P01 y P10 — comprobación tras reiniciar el backend») se ejecutó contra el proceso nuevo. Los tiempos y los dos arranques de Spring Boot están en [backend-reinicio.log](logs/backend-reinicio.log) ([captura](capturas/backend-reinicio.png)).
- **P04, envejecimiento.** El caso fija un umbral de 1 minuto en un servicio propio y espera 61 segundos, para no esperar los 15 minutos por defecto. La semilla trae además el escenario con el umbral por defecto (turno `CAJ-…-006` con 50 minutos de espera).
- **P12, frontend.** La petición verifica por API la pantalla pública. La parte de Angular se verificó en el navegador; las capturas de esa demostración están en la guía de la sección 6 del documento.

## Resultados esperados y obtenidos

### P01 — Flujo completo

**Resultado esperado (sección 8 del documento):** Ingresar, emitir turno, llamar, iniciar y finalizar; conservar datos después de reiniciar el backend.

**Resultado obtenido:** 11 peticiones HTTP, 5 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ login 200, emisión 201, llamado 200, inicio 200 y fin 200
- ✔ 200 OK
- ✔ el turno queda FINALIZADO con sus fechas
- ✔ 4 transiciones registradas a nombre del operador
- ✔ el turno de P01 sigue FINALIZADO con sus 4 transiciones (tras reiniciar el backend)

**Estado:** Aprobado. Evidencia: [captura](capturas/P01.png) y [log](logs/postman-p01-p12.log).

### P02 — Prioridad y FIFO

**Resultado esperado (sección 8 del documento):** Con R1, P1 y P2 sin envejecimiento y contador en 0, llamar P1 y luego P2; entre llamados, resolver el turno anterior (FINALIZADO, CANCELADO o AUSENTE) para liberar tanto el módulo como la cuenta del operador. Entre preferentes se respeta fecha de ingreso e ID ascendente.

**Resultado obtenido:** 14 peticiones HTTP, 5 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ contador de preferentes en 0 antes de llamar
- ✔ 200 OK
- ✔ sin resolver el turno anterior, el siguiente llamado responde 409
- ✔ se llama P1 y después P2, aunque R1 ingresó antes
- ✔ R1 sigue en espera

**Estado:** Aprobado. Evidencia: [captura](capturas/P02.png) y [log](logs/postman-p01-p12.log).

### P03 — Proporción 3 a 1

**Resultado esperado (sección 8 del documento):** Con P1 a P4 preferentes y R1 Regular no envejecido, contador 0 y proporcionPreferente=3 (valor por defecto), liberar el turno anterior y tanto la cuenta del operador como el módulo después de cada llamado; orden esperado: P1, P2, P3, R1, P4.

**Resultado obtenido:** 18 peticiones HTTP, 4 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ proporción 3 y contador 0 antes de llamar
- ✔ 200 OK
- ✔ orden P1, P2, P3, R1, P4
- ✔ contador después de cada llamado: 1, 2, 3, 0, 1

**Estado:** Aprobado. Evidencia: [captura](capturas/P03.png) y [log](logs/postman-p01-p12.log).

### P04 — Envejecimiento

**Resultado esperado (sección 8 del documento):** R1 ingresó antes que P1; con R1 esperando al menos el umbral configurado (15 minutos por defecto) y contador 0, llamar primero a R1 conservando su categoría original Regular.

**Resultado obtenido:** 8 peticiones HTTP, 5 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ umbral de 1 minuto y contador en 0
- ✔ 200 OK
- ✔ se llama primero a R1, que ingresó antes que P1
- ✔ R1 compite como preferente por envejecimiento
- ✔ R1 conserva su categoría original Regular

**Estado:** Aprobado. Evidencia: [captura](capturas/P04.png) y [log](logs/postman-p01-p12.log).

### P05 — Cola de un solo tipo

**Resultado esperado (sección 8 del documento):** Atender todos los turnos disponibles en cada caso de conjunto único, resolviendo cada turno antes del siguiente llamado para liberar el módulo y la cuenta del operador. Caso A: solo preferentes, con contador en el máximo configurado (3 por defecto), se siguen llamando preferentes y el contador permanece en el máximo; cuando aparece un Regular no envejecido, se llama en el siguiente turno. Caso B: solo Regular no envejecido, se atienden en FIFO y el contador se reinicia en 0. Cola vacía sin error interno y sin modificar el contador.

**Resultado obtenido:** 29 peticiones HTTP, 6 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ 200 OK
- ✔ caso A: los cuatro preferentes salen en orden y el contador queda en 3
- ✔ cola vacía: 200 con colaVacia = true y el contador sigue en 3
- ✔ caso A: el Regular no envejecido se llama en el siguiente turno y el contador vuelve a 0
- ✔ caso B: los Regular salen en FIFO y el contador queda en 0
- ✔ cola vacía otra vez: sin error y el contador persistido sigue en 0

**Estado:** Aprobado. Evidencia: [captura](capturas/P05.png) y [log](logs/postman-p01-p12.log).

### P06 — Estados y módulo

**Resultado esperado (sección 8 del documento):** Rechazar finalizar desde EN_ESPERA y repetir una finalización; rechazar llamar otro turno con el módulo ocupado o cuando la misma cuenta ya tiene otro turno activo en cualquier módulo/servicio. Rechazar a un OPERADOR que intente modificar el turno activo de otro operador o cancelar un EN_ESPERA de días anteriores; permitir al ADMIN resolverlo y registrar su identidad en el historial sin reemplazar al operador asignado originalmente.

**Resultado obtenido:** 24 peticiones HTTP, 7 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ finalizar desde EN_ESPERA: 409
- ✔ otra cuenta llama en el módulo ocupado: 409
- ✔ la cuenta con turno activo no llama en otro módulo ni en otro servicio: 409
- ✔ un OPERADOR modifica el turno activo de otro: 403
- ✔ finalizar 200 y repetir la finalización 409
- ✔ el ADMIN resuelve el turno ajeno: queda en el historial y el operador asignado no cambia
- ✔ EN_ESPERA de un día anterior: 403 para el OPERADOR y 200 para el ADMIN

**Estado:** Aprobado. Evidencia: [captura](capturas/P06.png) y [log](logs/postman-p01-p12.log).

### P07 — Validaciones

**Resultado esperado (sección 8 del documento):** Rechazar campos vacíos, referencias inexistentes, consecutivos/estados/fechas enviados por el cliente y datos fuera de longitud. Rechazar CANCELADO o AUSENTE sin motivo de 5-255 caracteres y rechazar password vacío en una actualización de usuario (null/omitido conserva el actual).

**Resultado obtenido:** 26 peticiones HTTP, 20 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ identificadorCliente vacío: 400 con el detalle del campo
- ✔ sin servicioId ni categoriaId: 400
- ✔ identificador de 2 caracteres: 400
- ✔ identificador de 101 caracteres: 400
- ✔ servicio inexistente: 404
- ✔ categoría inexistente: 404
- ✔ módulo de un servicio inexistente: 404
- ✔ JSON mal formado: 400
- ✔ username de 2 caracteres: 400
- ✔ password de 7 caracteres: 400
- ✔ prefijo en minúsculas y dígitos: 400
- ✔ parámetros de cola fuera de rango: 400
- ✔ consecutivo, estado y fechas enviados por el cliente se ignoran
- ✔ CANCELADO sin motivo: 400
- ✔ AUSENTE sin motivo: 400
- ✔ motivo de 4 caracteres: 400
- ✔ motivo de solo espacios: 400
- ✔ motivo de 256 caracteres: 400
- ✔ AUSENTE con motivo de 5 caracteres: 200
- ✔ password vacío al actualizar: 400; null u omitido: 200 y la contraseña sigue igual

**Estado:** Aprobado. Evidencia: [captura](capturas/P07.png) y [log](logs/postman-p01-p12.log).

### P08 — Seguridad JWT

**Resultado esperado (sección 8 del documento):** Rechazar token ausente, inválido o vencido con 401; después de emitir un token, comprobar que desactivar la cuenta o cambiar su rol hace que las siguientes solicitudes protegidas sean rechazadas con 401. Rechazar a OPERADOR intentando administrar usuarios, catálogos o historial con 403; impedir desactivar o degradar al último ADMIN activo y modificar usuarios con turnos activos.

**Resultado obtenido:** 36 peticiones HTTP, 10 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ sin token: 401
- ✔ token mal formado: 401
- ✔ firma alterada: 401
- ✔ firmado con otro secreto: 401
- ✔ token vencido: 401 (el mismo token vigente responde 200)
- ✔ cuenta desactivada: su token pasa de 200 a 401
- ✔ cambio de rol: el token anterior pasa de 200 a 401
- ✔ OPERADOR en usuarios, catálogos, parámetros, historial e indicadores: 403
- ✔ desactivar o degradar al último ADMIN activo: 409
- ✔ desactivar o cambiar el rol de una cuenta con turno activo: 409

**Estado:** Aprobado. Evidencia: [captura](capturas/P08.png) y [log](logs/postman-p01-p12.log).

### P09 — Dos operadores

**Resultado esperado (sección 8 del documento):** Con dos operadores, enviar solicitudes simultáneas sobre una misma cola y comprobar que reciben turnos distintos; si solo queda uno, únicamente una solicitud lo obtiene. Además, enviar solicitudes simultáneas desde una misma cuenta a módulos de dos servicios distintos y comprobar que solo obtiene un turno activo, gracias al bloqueo de la cuenta y a la revalidación posterior.

**Resultado obtenido:** 66 peticiones HTTP, 6 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ dos turnos y dos operadores a la vez: turnos distintos en 5 de 5 rondas
- ✔ un solo turno: una solicitud lo obtiene y la otra recibe colaVacia en 5 de 5 rondas
- ✔ misma cuenta en dos servicios a la vez: un solo turno activo en 5 de 5 rondas
- ✔ 200 OK
- ✔ ningún turno se entregó dos veces
- ✔ ningún turno del servicio quedó LLAMADO o EN_ATENCION

**Estado:** Aprobado. Evidencia: [captura](capturas/P09.png) y [log](logs/postman-p01-p12.log).

### P10 — Numeración simultánea

**Resultado esperado (sección 8 del documento):** Dos registros simultáneos del mismo servicio generan códigos consecutivos distintos. Cancelar un turno o reiniciar el backend no reutiliza números del día.

**Resultado obtenido:** 9 peticiones HTTP, 7 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ 10 registros simultáneos: 201 con consecutivos 1 a 10 sin repetir
- ✔ cancelar el último no libera su número: el siguiente es 11
- ✔ 200 OK
- ✔ el historial tiene los consecutivos 1 a 11, una sola vez cada uno
- ✔ el turno 10 quedó CANCELADO y su número no se reutilizó
- ✔ códigos con formato PREFIJO-YYYYMMDD-NNN
- ✔ la numeración de P10 continúa en 12 (tras reiniciar el backend)

**Estado:** Aprobado. Evidencia: [captura](capturas/P10.png) y [log](logs/postman-p01-p12.log).

### P11 — Historial e indicadores

**Resultado esperado (sección 8 del documento):** Filtros, transiciones de estado y conteos coinciden con SQL. Promedio de espera = fecha_inicio_atencion menos fecha_ingreso de los turnos con inicio registrado.

**Resultado obtenido:** 25 peticiones HTTP, 5 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ 200 OK
- ✔ conteos iguales a los turnos del servicio
- ✔ promedio de espera = inicio de atención menos ingreso de los 2 turnos atendidos
- ✔ filtros del historial por servicio, estado y fecha
- ✔ transiciones del turno finalizado en orden

**Estado:** Aprobado. Evidencia: [captura](capturas/P11.png) y [log](logs/postman-p01-p12.log).

### P12 — Frontend y persistencia

**Resultado esperado (sección 8 del documento):** Las acciones en Angular modifican el backend real. La pantalla pública se actualiza periódicamente y no expone documentos ni nombres completos.

**Resultado obtenido:** 6 peticiones HTTP, 4 aserciones aprobadas, 0 fallidas y 0 omitidas.

- ✔ 200 OK sin token
- ✔ el turno llamado aparece con su ventanilla
- ✔ no expone documentos ni nombres
- ✔ hasta 10 turnos en espera por servicio

**Estado:** Aprobado. Evidencia: [captura](capturas/P12.png) y [log](logs/postman-p01-p12.log).

## Pruebas JUnit del backend

| Clase | Pruebas | Fallas | Errores | Omitidas |
|---|---|---|---|---|
| `BackendApplicationTests` | 1 | 0 | 0 | 0 |
| `SecurityConfigTest` | 4 | 0 | 0 | 0 |
| `SwaggerDeshabilitadoTest` | 1 | 0 | 0 | 0 |
| `LoginThrottleTest` | 4 | 0 | 0 | 0 |
| `AuthServiceTest` | 5 | 0 | 0 | 0 |
| `ProyeccionColaTest` | 7 | 0 | 0 | 0 |
| `TurnoServiceTransicionTest` | 9 | 0 | 0 | 0 |
| **Total** | **31** | **0** | **0** | **0** |

## Cómo repetir la ejecución

1. Importar `db/banco_turnero_db.sql` en una base vacía y arrancar el backend con `JWT_SECRET` definida.
2. `postman collection run "postman/collections/Turnero con Prioridades — API" -i "9. Pruebas de aceptación (P01 a P12)" --env-var jwtSecret=<JWT_SECRET>`
3. Para P01 y P10: reiniciar el backend y enviar la petición «P01 y P10 — comprobación tras reiniciar el backend».
4. `mvn test` en `backend/`.
