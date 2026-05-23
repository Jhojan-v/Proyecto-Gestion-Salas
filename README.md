# Proyecto Gestion de Salas - Backend

Backend REST para la gestion de salas, reservas, recursos tecnologicos, usuarios, notificaciones y reportes. El proyecto esta construido con Spring Boot, Spring Security, JWT, JPA/Hibernate y MySQL.

## Estado actual del proyecto

- Aplicacion Java 21 con Spring Boot 3.4.4.
- Gestion de persistencia con Spring Data JPA y MySQL.
- Seguridad stateless con JWT. Las rutas publicas son `/`, `/api/health`, `/api/usuarios/login` y `/api/usuarios/registrar`.
- Pruebas de integracion configuradas con H2 en memoria.
- Maven Wrapper incluido para ejecutar el proyecto sin instalar Maven globalmente.
- Script `start-dev.ps1` incluido para ambiente local en Windows con MySQL local y carga de usuarios base.

## Requisitos

- Java JDK 21.
- MySQL Server 8 o compatible.
- PowerShell en Windows si se usa el script de desarrollo.
- Git, para clonar o actualizar el repositorio.

No es obligatorio instalar Maven porque el repositorio incluye `mvnw.cmd`.

## Instalacion

1. Clonar el repositorio:

```powershell
git clone <URL_DEL_REPOSITORIO>
cd Proyecto-Gestion-Salas
```

2. Verificar Java:

```powershell
java -version
```

La version debe ser Java 21.

3. Descargar dependencias y compilar:

```powershell
.\mvnw.cmd clean install
```

## Configuracion

La configuracion principal esta en `src/main/resources/application.properties`.

Variables de entorno soportadas:

| Variable | Valor por defecto | Descripcion |
| --- | --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/gestion_salas?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Bogota` | URL de conexion a MySQL. |
| `SPRING_DATASOURCE_USERNAME` | `root` | Usuario de base de datos. |
| `SPRING_DATASOURCE_PASSWORD` | `root` | Password de base de datos. |
| `SESSION_COOKIE_SECURE` | `false` | Activa cookie segura cuando se despliega con HTTPS. |

Ejemplo de configuracion manual en PowerShell:

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/gestion_salas?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=America/Bogota"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="root"
```

Hibernate esta configurado con `spring.jpa.hibernate.ddl-auto=update`, por lo que crea o actualiza las tablas al iniciar la aplicacion.

## Ejecucion local

### Opcion 1: ejecucion rapida con script de desarrollo

Este camino es util en Windows. El script busca `mysqld.exe` y `mysql.exe`, inicializa una base local dentro de `target/`, levanta MySQL si hace falta, inicia el backend y crea usuarios base.

```powershell
.\start-dev.ps1
```

Si MySQL no esta en el PATH, definir la ruta base:

```powershell
$env:MYSQL_BASE_DIR="C:\Program Files\MySQL\MySQL Server 8.0"
.\start-dev.ps1
```

Si se necesita forzar un puerto local para MySQL:

```powershell
$env:LOCAL_MYSQL_PORT="3307"
.\start-dev.ps1
```

Cuando termina correctamente, el backend queda disponible en:

```text
http://localhost:8080
```

### Opcion 2: ejecucion manual

1. Crear o tener disponible MySQL en `localhost:3306`.
2. Configurar las variables de entorno si el usuario/password no son `root/root`.
3. Ejecutar:

```powershell
.\mvnw.cmd spring-boot:run
```

4. Verificar el estado:

```powershell
curl http://localhost:8080/api/health
```

## Ejecucion en servidor

1. Instalar Java 21 y MySQL en el servidor.
2. Crear la base de datos o permitir que la URL la cree automaticamente:

```sql
CREATE DATABASE IF NOT EXISTS gestion_salas;
```

3. Definir variables de entorno con credenciales reales:

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://<HOST_DB>:3306/gestion_salas?useSSL=false&serverTimezone=America/Bogota"
export SPRING_DATASOURCE_USERNAME="<USUARIO_DB>"
export SPRING_DATASOURCE_PASSWORD="<PASSWORD_DB>"
export SESSION_COOKIE_SECURE="true"
```

4. Construir el artefacto:

```bash
./mvnw clean package -DskipTests
```

En Windows:

```powershell
.\mvnw.cmd clean package -DskipTests
```

5. Ejecutar el JAR:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

El servicio usa el puerto `8080` por defecto. Si se requiere otro puerto:

```bash
java -jar target/backend-0.0.1-SNAPSHOT.jar --server.port=8081
```

## Pruebas

Las pruebas usan H2 en memoria con modo MySQL. Para ejecutarlas:

```powershell
.\mvnw.cmd test
```

En Linux o macOS:

```bash
./mvnw test
```

## Endpoints principales

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| `GET` | `/` | Endpoint raiz. |
| `GET` | `/api/health` | Verificacion de salud del backend. |
| `POST` | `/api/usuarios/registrar` | Registro de usuarios. |
| `POST` | `/api/usuarios/login` | Autenticacion y obtencion de JWT. |
| `GET` | `/api/salas` | Listado de salas. |
| `POST` | `/api/salas` | Creacion de salas. |
| `GET` | `/api/salas/{idSala}` | Detalle de sala. |
| `PUT` | `/api/salas/{idSala}` | Actualizacion de sala. |
| `PATCH` | `/api/salas/{idSala}/estado` | Actualizacion de estado de sala. |
| `POST` | `/api/salas/{idSala}/recursos` | Agregar recursos a una sala. |
| `DELETE` | `/api/salas/{idSala}/recursos` | Retirar recursos de una sala. |
| `POST` | `/api/reservas` | Crear reserva. |
| `GET` | `/api/reservas` | Listar reservas. |
| `GET` | `/api/reservas/disponibilidad` | Consultar disponibilidad. |
| `GET` | `/api/reservas/mis-reservas` | Reservas del usuario autenticado. |
| `GET` | `/api/reservas/mi-historial` | Historial del usuario autenticado. |
| `GET` | `/api/reservas/reporte/reservas` | Reporte de reservas. |
| `GET` | `/api/reservas/reporte/horas` | Reporte de horas. |
| `GET` | `/api/reservas/reporte/usuarios` | Reporte de usuarios. |
| `GET` | `/api/notificaciones/mias` | Notificaciones del usuario autenticado. |
| `PATCH` | `/api/notificaciones/{idNotificacion}/leer` | Marcar notificacion como leida. |
| `PATCH` | `/api/notificaciones/leer-todas` | Marcar todas las notificaciones como leidas. |

Excepto las rutas publicas, los endpoints requieren el encabezado:

```text
Authorization: Bearer <TOKEN_JWT>
```

## Ejemplos de uso

Registrar usuario:

```powershell
curl -X POST http://localhost:8080/api/usuarios/registrar `
  -H "Content-Type: application/json" `
  -d '{"nombre":"Docente Ingenieria","correo":"docente.ingenieria@uao.edu.co","password":"ClaveSegura1!","rol":"DOCENTE","idFacultad":1}'
```

Iniciar sesion:

```powershell
curl -X POST http://localhost:8080/api/usuarios/login `
  -H "Content-Type: application/json" `
  -d '{"correo":"docente.ingenieria@uao.edu.co","password":"ClaveSegura1!"}'
```

Consumir un endpoint protegido:

```powershell
curl http://localhost:8080/api/salas `
  -H "Authorization: Bearer <TOKEN_JWT>"
```

## Estructura del proyecto

```text
src/main/java/com/apiweb/backend
+-- Controller    # Controladores REST
+-- DTO           # Objetos de entrada y salida de la API
+-- Exception     # Manejo centralizado de errores
+-- Model         # Entidades JPA
+-- Repository    # Repositorios Spring Data
+-- Security      # Configuracion de seguridad, JWT y filtros
+-- Service       # Logica de negocio
```

## Notas importantes

- El proyecto usa `NoOpPasswordEncoder`, por lo que las contrasenas no se cifran. Para produccion se debe migrar a `BCryptPasswordEncoder`.
- CORS esta habilitado para origenes locales `http://localhost:*` y `http://127.0.0.1:*`.
- El puerto por defecto del backend es `8080`.
- Los logs del script de desarrollo se generan dentro de `target/`.
