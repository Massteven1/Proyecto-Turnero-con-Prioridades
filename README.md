# Turnero con prioridades

Aplicación cliente-servidor para gestionar turnos bancarios con categorías preferentes, atención por módulos e historial. El backend expone la API REST bajo `/api/v1` y el frontend en Angular consume esos endpoints.

## Estructura

- `backend/`: API Spring Boot, Java 21 y Maven.
- `frontend/`: aplicación Angular 22.
- `db/`: script SQL de inicialización y datos semilla.
- `docs/`: documentación funcional y contrato de endpoints.
- `postman/`: colección ejecutable de la API.

## Requisitos

| Herramienta | Versión exacta usada |
|---|---|
| Java JDK | 21 (fijado en `backend/pom.xml` con `java.version=21`; se puede compilar con un JDK superior) |
| Spring Boot | 4.1.2 |
| Maven | 3.9.16 |
| MySQL | 8.0.16 o superior (requerido para que el motor aplique las restricciones `CHECK` y las columnas generadas). Probado en MariaDB 10.4.32 de XAMPP |
| Node.js | 26.2.0 |
| npm | 12.0.2 |
| Angular / Angular CLI | 22.1.x / 22.1.2 |

## Configuración

Las credenciales y secretos viven fuera del repositorio. La plantilla completa está en `backend/src/main/resources/application-example.yml`; las variables son:

| Variable | Obligatoria | Uso |
|---|---|---|
| `DB_URL` | No (default local) | Cadena JDBC de MySQL/MariaDB |
| `DB_USERNAME` | No (default `root`) | Usuario de la base |
| `DB_PASSWORD` | No (default vacío) | Contraseña de la base |
| `JWT_SECRET` | **Sí** | Firma HS256 del token; 32 caracteres o más |
| `JWT_EXPIRATION_MS` | No (default `7200000`) | Vigencia del token en milisegundos |

En PowerShell, antes de levantar el backend:

```powershell
$env:JWT_SECRET = "cadena-aleatoria-larga-de-al-menos-32-caracteres"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = ""
```

Sin `JWT_SECRET` la aplicación no arranca: el secreto no tiene valor por defecto para no versionar credenciales.

## Base de datos

`db/banco_turnero_db.sql` es el script reproducible de inicialización: crea la base `banco_turnero_db`, el esquema completo y los datos semilla en un único archivo (cumple el rol de `schema.sql` + `data.sql`). No elimina tablas existentes: si ya hay una instalación previa, respaldarla y preparar una base vacía antes de ejecutarlo.

```sql
SOURCE db/banco_turnero_db.sql;
```

La zona horaria oficial del sistema es `America/Bogota` (UTC-5). El script genera las marcas de demostración relativas al momento de ejecutarse, y la aplicación calcula todos los tiempos con `ZoneId.of("America/Bogota")`.

## Ejecución

Backend:

```powershell
$env:JWT_SECRET = "cadena-aleatoria-larga-de-al-menos-32-caracteres"
cd backend
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm start
```

La API queda en `http://localhost:8080` y la aplicación Angular en `http://localhost:4200`.

## Cuentas de prueba y datos semilla

Las contraseñas de la tabla son las de prueba en texto claro; en la base solo se almacena su hash BCrypt (`$2a$10$...`).

| Usuario | Contraseña | Rol | Estado inicial |
|---|---|---|---|
| `admin` | `Admin123!` | ADMIN | Sin turno activo |
| `operador1` | `Operador123!` | OPERADOR | Turno `CAJ-AAAAMMDD-001` en `LLAMADO`, Ventanilla Caja 1 (módulo 1) |
| `operador2` | `Operador456!` | OPERADOR | Turno `ASE-AAAAMMDD-001` en `EN_ATENCION`, Módulo Asesoría 1 (módulo 3) |

Datos de demostración:

- 4 categorías base: Discapacidad, Embarazada, Adulto mayor (preferentes) y Regular.
- 2 servicios: Caja General (`CAJ`) con 2 módulos y Asesoría Comercial (`ASE`) con 1 módulo.
- 12 turnos en los seis estados (`EN_ESPERA`, `LLAMADO`, `EN_ATENCION`, `FINALIZADO`, `CANCELADO`, `AUSENTE`) con su historial de transiciones.

### Escenario P04 (envejecimiento)

El script siembra el turno `CAJ-AAAAMMDD-006` de categoría **Regular** con `fecha_ingreso` 50 minutos antes de la ejecución, es decir, muy por encima del umbral por defecto de 15 minutos. Los preferentes `007`, `008` y `009` ingresaron después (30, 25 y 20 minutos). El estado de cola de Caja queda con `contador_preferentes = 0`.

Por lo tanto, al llamar el siguiente turno en un módulo libre de Caja, la regla de envejecimiento debe devolver primero el `006` conservando su categoría Regular. Como el cliente nunca puede fijar `fecha_ingreso`, la única forma de reproducir el escenario es mediante la semilla. Verificación directa:

```sql
SELECT codigo, categoria_nombre_snapshot, estado, fecha_ingreso,
       TIMESTAMPDIFF(MINUTE, fecha_ingreso, NOW()) AS minutos_esperando
FROM turnos
WHERE servicio_id = 1 AND estado = 'EN_ESPERA'
ORDER BY fecha_ingreso;
```

Si la base se inicializó hace horas, todos los turnos en espera habrán envejecido; para repetir el escenario exacto, reinicializar sobre una base vacía.

## Documentación de la API

- Contrato de endpoints y DTOs: `docs/Endpoints - Proyecto Turnero con Prioridades.xlsx`.
- Documentación funcional: `docs/Documentación Proyecto Turnero con Prioridades.docx`.
- Colección ejecutable: `postman/collections/Turnero con Prioridades — API` (formato SDK YAML). Usa la variable `baseUrl` (por defecto `http://localhost:8080`), guarda el JWT del login en `{{token}}` y lo envía como `Authorization: Bearer`. El workflow `.github/workflows/postman-sync.yml` la sincroniza con Postman al pushear a `main`.
- Swagger/OpenAPI (springdoc): con el backend en marcha, `http://localhost:8080/swagger-ui.html` muestra la interfaz y `http://localhost:8080/v3/api-docs` el contrato en JSON. El contrato se genera desde los controladores y DTOs: no se mantiene a mano. Para probar endpoints protegidos, usar el botón **Authorize** y pegar el token que devuelve el login.

## Aclaraciones sobre la documentación

`docs/` es la referencia funcional del proyecto: el contrato de endpoints del xlsx y las reglas del docx se aplican literalmente, salvo donde la documentación se contradice a sí misma o no coincide con el entorno de trabajo. Estas son las decisiones acordadas con el responsable del proyecto:

| Tema | Lo que dice la documentación | Decisión aplicada |
|---|---|---|
| Versión de Spring Boot | Una versión anterior a la actual | Se mantiene **4.1.2**, la vigente al iniciar el proyecto; los endpoints, DTOs y validaciones del contrato no cambian |
| Motor de base de datos | MySQL 8.0.16 o superior | Se usa **MariaDB 10.4.32 de XAMPP**, que soporta `CHECK` y columnas generadas; el esquema, los triggers y los datos semilla son los mismos |
| Casos P01–P12 | La matriz de aceptación se exige como casos de la colección de Postman | Se completan **cuando termine el backend**, no fase por fase: los casos atraviesan varios endpoints a la vez |
| Frontend Angular | Aparece intercalado en el plan de fases | Se inicia **cuando el backend esté terminado**; ninguna fase del backend depende del cliente |
| 409 al modificar un usuario | El xlsx y la sección transaccional discrepan sobre su alcance | El 409 aplica solo a **desactivar la cuenta o cambiar el rol** de un usuario con turno activo, no a renombrarlo ni a cambiar su contraseña |
| Consulta de turnos | El contrato describe la cola y el historial como consultas separadas y, sobre el historial, la tabla de permisos del docx contradice la sección del endpoint: la primera deja al OPERADOR consultar la fecha actual y la segunda lo declara exclusivo de ADMIN | `GET /api/v1/turnos` es una sola ruta con `?modo=COLA` (valor por defecto) y `?modo=HISTORIAL`, exclusivo de ADMIN; el xlsx confirma el 403 para OPERADOR |
| Servicio inexistente en la cola | La fila del xlsx de `GET /api/v1/turnos` enumera 400, 401 y 403, sin 404 | `?modo=COLA` con un `servicioId` que no existe responde **404 Not Found** con `ErrorResponseDTO`, igual que el resto de rutas que reciben un ID por parámetro; un servicio existente sin turnos en espera responde 200 con la lista vacía |
| Promedio de espera sin turnos atendidos | `IndicadoresDTO` define `tiempoPromedioEsperaSegundos` y su texto formateado, pero no especifica el caso en que ningún turno del alcance registró inicio de atención | Ambos campos viajan en **null** (no `0` ni `"00 min 00 seg"`): sin turnos atendidos no hay promedio que calcular y `AVG` sobre cero filas es nulo; los conteos por estado sí responden en cero |

## Validación

```powershell
cd backend
mvn test

cd ..\frontend
npm run build
```

Las pruebas del backend usan H2 en memoria (`backend/src/test/resources/application.properties`) para no depender de un MySQL local. Además del arranque del contexto, `mvn test` cubre las reglas que más se rompen:

- `ProyeccionColaTest`: proporción N:1 de preferentes, FIFO de regulares, envejecimiento por umbral y continuidad del contador persistido.
- `TurnoServiceTransicionTest`: matriz de transiciones (409), propiedad del turno (403), excepción del ADMIN y motivo obligatorio (400).
- `AuthServiceTest`: el 401 del login es único para usuario inexistente, contraseña incorrecta y cuenta inactiva.

La colección de Postman se ejecuta contra el backend en marcha y cubre las fases implementadas.

## Seguridad y solución de problemas

- **Logout**: Angular elimina el token del cliente. El JWT emitido sigue siendo criptográficamente válido hasta su `exp` porque el alcance no incluye lista de revocación; en cambio, desactivar la cuenta o cambiar su rol hace que el backend rechace el token en la siguiente petición con 401.
- **Login sin filtración de cuentas**: `POST /api/v1/auth/login` responde siempre el mismo `401` (`Credenciales inválidas`) para usuario inexistente, contraseña incorrecta o cuenta inactiva. Cuando el usuario no existe se compara igual contra BCrypt con un hash señuelo, para que el tiempo de respuesta tampoco delate si la cuenta existe.
- **Puerto 8080 ocupado**: identificar y detener el proceso, o levantar en otro puerto.

  ```powershell
  Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
  Stop-Process -Id <PID>
  mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8099
  ```

  Si se cambia el puerto, actualizar `baseUrl` en la colección de Postman.
- **Fallo de conexión a MySQL/MariaDB**: verificar que el servicio esté iniciado (panel de XAMPP), que `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` apunten a la instancia correcta y que la base tenga el esquema cargado; con `ddl-auto=validate`, Hibernate falla al arrancar si falta una tabla o columna.
- **CORS**: solo se permite el origen `http://localhost:4200` (`SecurityConfig`). Si Angular se sirve en otro puerto u host, agregar ese origen en `corsConfigurationSource()`.
- **401 por token ausente, inválido o vencido**: la vigencia por defecto es de 2 horas (`JWT_EXPIRATION_MS`). Volver a iniciar sesión; el 401 también aparece si la cuenta fue desactivada o su rol cambió después de emitir el token.
- **`Could not resolve placeholder 'JWT_SECRET'`**: falta exportar la variable de entorno antes de `mvn spring-boot:run`.
- **Angular no compila**: usar Node.js 26 y npm 12, y ejecutar `npm install` dentro de `frontend/`.

## Estado funcional

Implementado:

- Autenticación con JWT (`POST /api/v1/auth/login`) y revalidación de cuenta y rol en cada petición protegida.
- Usuarios (`GET`, `POST`, `PUT /api/v1/usuarios`): BCrypt, username inmutable, 409 por turno activo y por último ADMIN activo, 403 a OPERADOR.
- Catálogos: servicios (`GET`, `POST`, `PUT`), módulos (`GET`, `POST`, `PUT`), categorías (`GET`, `PUT`) y parámetros de cola (`GET`, `PUT`), con autorización por rol y bloqueos pesimistas en el orden definido.
- Turnos: emisión (`POST /api/v1/turnos`) con consecutivo diario y snapshot de categoría, llamado (`POST /api/v1/modulos/{id}/siguiente`) con proporción N:1 y envejecimiento, y transiciones (`PATCH /api/v1/turnos/{id}/estado`) con motivo obligatorio, propiedad del turno e historial de auditoría.
- Consulta: cola proyectada por servicio (`GET /api/v1/turnos?modo=COLA`), historial con filtros de fecha, servicio y estado (`GET /api/v1/turnos?modo=HISTORIAL`, exclusivo de ADMIN) y pantalla pública sin datos personales (`GET /api/v1/publico/pantalla`).
- Historial e indicadores: detalle de un turno con sus transiciones cronológicas (`GET /api/v1/turnos/{id}`, ADMIN sobre cualquier fecha y OPERADOR solo sobre el día actual) e indicadores por fecha y servicio con conteos por estado y promedio de espera (`GET /api/v1/turnos/indicadores`, exclusivo de ADMIN).

Pendiente:

- Vistas del frontend Angular y los casos P01–P12 de la colección de Postman, que se completan al cerrar el backend.
