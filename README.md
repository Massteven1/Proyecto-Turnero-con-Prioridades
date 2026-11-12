# Turnero con prioridades

Aplicación cliente-servidor para gestionar turnos bancarios con categorías preferentes, atención por módulos e historial.

## Estructura

- `backend/`: API Spring Boot, Java 21 y Maven.
- `frontend/`: aplicación Angular 22.
- `db/`: script SQL de MariaDB/MySQL y datos semilla.
- `docs/`: documentación funcional y contrato de endpoints.

## Requisitos

- Java JDK 21
- Maven 3.9+
- Spring Boot 4.1.2 (versión fijada en `backend/pom.xml`)
- MySQL 8+ o MariaDB 10.4+
- Node.js compatible con Angular 22 y npm 12

## Configuración

Las credenciales y secretos se definen mediante variables de entorno del sistema; la referencia completa está en `backend/src/main/resources/application-example.yml` (exige la documentación: variables `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` y `JWT_EXPIRATION_MS`, sin credenciales reales en el repositorio). Los valores por defecto de desarrollo sirven para un XAMPP local.

Inicializar la base de datos:

```sql
SOURCE db/banco_turnero_db.sql;
```

El script crea y selecciona la base `banco_turnero_db` y carga los datos semilla. La zona horaria del sistema es `America/Bogota` (UTC-5). Si el usuario de la base de datos es distinto de `root`, ajustar `DB_USERNAME`.

## Ejecución

Backend:

```powershell
cd backend
mvn spring-boot:run
```

Frontend:

```powershell
cd frontend
npm install
npm start
```

La aplicación Angular queda disponible en `http://localhost:4200` y la API en `http://localhost:8080`.

## Validación

```powershell
cd backend
mvn test

cd ..\frontend
npm run build
```

El backend usa una base H2 en memoria durante las pruebas para que el test de contexto no dependa de un MySQL local.

## Estado funcional

La documentación de endpoints en `docs/Endpoints - Proyecto Turnero con Prioridades.xlsx` es el contrato de implementación. La estructura inicial ya está preparada para añadir entidades, repositorios, servicios, controladores y seguridad JWT, pero esos módulos aún deben implementarse.
