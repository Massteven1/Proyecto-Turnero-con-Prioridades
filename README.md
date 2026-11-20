# Turnero con prioridades

Sistema de turnos bancarios con categorías preferentes, atención por módulos, pantalla pública de sala de espera e historial con indicadores. El backend en Spring Boot expone la API REST bajo `/api/v1` y el frontend en Angular la consume.

## Estructura

- `backend/`: API Spring Boot 4.1.2 (Java 21, Maven).
- `frontend/`: aplicación Angular 22.
- `db/banco_turnero_db.sql`: esquema y datos de demostración.
- `docs/`: documentación funcional (docx) y contrato de endpoints (xlsx).
- `postman/`: colección de la API.

## Requisitos

- JDK 21 o superior y Maven 3.9.
- MySQL 8.0.16 o superior.
- Node.js 22.22, 24.15 o 26 (probado con 26.2.0) y npm.

## Puesta en marcha

### 1. Base de datos

Con MySQL iniciado en el panel de XAMPP, importar `db/banco_turnero_db.sql`. El script crea la base `banco_turnero_db`, sus tablas y los datos de demostración.

- **phpMyAdmin**: pestaña **Importar**, seleccionar el archivo y pulsar **Continuar**.
- **Consola**, desde la raíz del repositorio:

  ```powershell
  cmd /c "C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < db\banco_turnero_db.sql"
  ```

El backend se conecta por defecto a `localhost:3306` con el usuario `root` sin contraseña, que es la configuración de XAMPP. Si tu instalación es distinta, define `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` (ver [Configuración opcional](#configuración-opcional)).

### 2. Secreto del JWT

El backend no arranca sin la variable de entorno `JWT_SECRET`, que debe tener al menos 32 caracteres. Para una prueba rápida desde la terminal basta con:

```powershell
$env:JWT_SECRET = "cadena-aleatoria-larga-de-al-menos-32-caracteres"
```

Para ejecutar desde NetBeans u otro IDE, conviene crearla como variable de usuario de Windows con un valor aleatorio. Después hay que cerrar y volver a abrir el IDE, porque solo lee las variables al iniciar:

```powershell
$b = [byte[]]::new(48); [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
[Environment]::SetEnvironmentVariable('JWT_SECRET', [Convert]::ToBase64String($b), 'User')
```

### 3. Backend

```powershell
cd backend
mvn spring-boot:run
```

En NetBeans, abrir la carpeta `backend` como proyecto y ejecutarlo con **Run Project**. La API queda en `http://localhost:8080`.

### 4. Frontend

```powershell
cd frontend
npm install
npm start
```

La aplicación queda en `http://localhost:4200`.

## Cuentas de prueba

| Usuario | Contraseña | Rol | Estado inicial |
|---|---|---|---|
| `admin` | `Admin123!` | ADMIN | Sin turno activo |
| `operador1` | `Operador123!` | OPERADOR | Turno `CAJ-AAAAMMDD-001` llamado en Ventanilla Caja 1 |
| `operador2` | `Operador456!` | OPERADOR | Turno `ASE-AAAAMMDD-001` en atención en Módulo Asesoría 1 |

La sala de espera (`http://localhost:4200/pantalla`) es pública y no pide iniciar sesión.

Los datos de demostración incluyen 2 servicios (Caja General con 2 ventanillas y Asesoría Comercial con 1 módulo), 4 categorías (Discapacidad, Embarazada y Adulto mayor como preferentes, y Regular) y 12 turnos en los seis estados con su historial. Las fechas se generan al importar el script: los códigos llevan la fecha de ese día (`AAAAMMDD`) y las esperas se cuentan desde ese momento.

**Escenario de envejecimiento (P04)**: el turno `CAJ-AAAAMMDD-006` es Regular e ingresa 50 minutos antes de la importación, antes que los preferentes `007`, `008` y `009` (30, 25 y 20 minutos). Con el umbral por defecto de 15 minutos, el siguiente llamado en una ventanilla libre de Caja debe devolver el `006`.

### Reiniciar los datos de demostración

Para volver al estado inicial, con el backend detenido, se borra la base y se importa otra vez el mismo script. Los turnos quedan con la fecha del día y las esperas vuelven a sus valores iniciales. Se pierde todo lo creado durante las pruebas.

- **phpMyAdmin**: seleccionar `banco_turnero_db`, abrir **Operaciones** y pulsar **Eliminar la base de datos (DROP)**. Después importar el script como en el paso 1.
- **Consola**, desde la raíz del repositorio:

  ```powershell
  & 'C:\xampp\mysql\bin\mysql.exe' -u root -e "DROP DATABASE IF EXISTS banco_turnero_db;"
  cmd /c "C:\xampp\mysql\bin\mysql.exe -u root --default-character-set=utf8mb4 < db\banco_turnero_db.sql"
  ```

## Documentación y pruebas

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`. Para los endpoints protegidos, pegar el token del login con el botón **Authorize**. El contrato en JSON está en `/v3/api-docs`.
- **Postman**: colección `postman/collections/Turnero con Prioridades — API`, con la variable `baseUrl` (`http://localhost:8080`). El login guarda el JWT en `{{token}}`. La colección crea servicios, módulos, usuarios y turnos de prueba, que después aparecen en la aplicación.
- **Documentos**: contrato de endpoints en `docs/Endpoints - Proyecto Turnero con Prioridades.xlsx` y reglas funcionales en `docs/Documentación Proyecto Turnero con Prioridades.docx`.
- **Evidencias de ejecución**: [`docs/evidencias/`](docs/evidencias/README.md) tiene el reporte de resultados esperados y obtenidos de P01 a P12, con los logs de la colección de Postman y de JUnit y capturas de cada ejecución.

Pruebas automáticas (las del backend usan H2 en memoria y no necesitan MySQL):

```powershell
cd backend
mvn test
cd ..\frontend
npx ng test --watch=false
```

## Configuración opcional

| Variable | Por defecto | Uso |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/banco_turnero_db?serverTimezone=America/Bogota` | Conexión a la base |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / vacío | Credenciales de la base |
| `JWT_EXPIRATION_MS` | `7200000` (2 horas) | Vigencia del token |
| `LOGIN_MAX_INTENTOS` / `LOGIN_VENTANA_MINUTOS` | `5` / `15` | Fallos de login permitidos por usuario y minutos de bloqueo |
| `SWAGGER_ENABLED` | `true` | `false` apaga Swagger UI y `/v3/api-docs` en producción |

La plantilla completa está en `backend/src/main/resources/application-example.yml`. Todos los tiempos usan la zona horaria `America/Bogota`.

## Seguridad

- El token es un JWT HS256. En cada petición protegida el backend revalida que la cuenta siga activa y con el mismo rol.
- El login responde el mismo 401 para usuario inexistente, contraseña incorrecta o cuenta inactiva, así no revela qué cuentas existen.
- Tras 5 intentos fallidos, ese usuario queda bloqueado 15 minutos y el login responde 429. El contador vive en memoria: reiniciar el backend lo limpia.
- La pantalla pública ignora el token, así que uno vencido no la interrumpe.
- CORS solo admite el origen `http://localhost:4200`.

## Solución de problemas

- **`Could not resolve placeholder 'JWT_SECRET'`**: falta la variable del paso 2. En NetBeans, reiniciar el IDE después de crearla.
- **`JWT_SECRET debe tener al menos 32 bytes`**: usar un secreto más largo.
- **Puerto 8080 ocupado**: buscar el proceso con `Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess` y detenerlo con `Stop-Process -Id <PID>`.
- **No conecta a la base**: iniciar MySQL en XAMPP y comprobar que el script está importado. Hibernate valida el esquema al arrancar y falla si falta una tabla o columna.

## Decisiones frente a la documentación

- Se usa Spring Boot 4.1.2, la versión vigente al iniciar el proyecto, en lugar de la anterior que cita el docx.
- Modificar un usuario con un turno activo responde 409 solo al desactivarlo o cambiarle el rol, no al renombrarlo ni al cambiar su contraseña.
- `GET /api/v1/turnos` atiende la cola (`?modo=COLA`, por defecto) y el historial (`?modo=HISTORIAL`, solo ADMIN).
- Pedir la cola de un `servicioId` inexistente responde 404.
- Si ningún turno fue atendido, el promedio de espera de los indicadores viaja en `null`.
