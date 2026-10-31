# Foro Usuarios

Microservicio de usuarios para el sistema de foros de DSY2205, Semana 2. Proyecto Spring Boot independiente con paquete base `cl.duoc.forousuarios` y puerto predeterminado **8081**.

## Estado actual

Registro, inicio de sesión, identidad autenticada, gestión de cuentas y roles implementados. Cada usuario gestiona su cuenta; un administrador también puede listar y gestionar otras cuentas y cambiar roles. La eliminación desactiva la cuenta y bloquea su autenticación. Las rutas protegidas sin token devuelven 401, incluida `/`. La conexión Oracle y las evidencias Postman siguen pendientes.

Esta aplicación tiene su propio repositorio Git y se compila por separado. La futura integración con `foro-contenido` será exclusivamente HTTP. No se requieren código fuente, JAR, tablas ni archivos de configuración de otro proyecto para compilar o iniciar esta base.

## Requisitos

- JDK 17; `JAVA_HOME` apunta a su instalación.
- Maven Wrapper incluido: descarga Maven 3.9.14 en su primer uso. Se requiere acceso a Maven Central si las herramientas o dependencias no están en la caché del equipo.
- Spring Boot 3.5.16. Las versiones transitivas se administran mediante sus BOM, incluido Oracle JDBC.

Ejecutar los comandos desde la raíz de este repositorio. PowerShell:

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
```

En Linux o macOS usar `sh ./mvnw clean verify`, con JDK 17 disponible.

## Desarrollo local

Antes de iniciar, definir `JWT_SECRET_BASE64` con una clave aleatoria de al menos 32 bytes codificada en Base64. Es obligatoria para todos los perfiles de ejecución y no tiene valor predeterminado. Para generar una clave en esta terminal sin mostrarla:

```powershell
$taskJwtKey = New-Object byte[] 32
$taskRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$taskRng.GetBytes($taskJwtKey)
$env:JWT_SECRET_BASE64 = [Convert]::ToBase64String($taskJwtKey)
$taskRng.Dispose()
```

Mantener la misma clave entre reinicios si se desea conservar la validez de los tokens todavía vigentes. Cambiarla invalida los tokens anteriores. Para las pruebas Maven se utiliza exclusivamente una clave de fixture bajo `src/test/resources`, que no se incluye en el JAR.

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

El perfil `local` usa H2 en memoria en modo Oracle. Hibernate crea y elimina el esquema local; los datos se pierden al detener la aplicación. La consola H2 está deshabilitada. Este perfil sirve para desarrollo, no acredita una conexión real a Oracle.

El perfil predeterminado es `local`. Si el entorno ya define `SPRING_PROFILES_ACTIVE`, ese valor prevalece; usar el perfil explícito mostrado para evitar confusiones. No activar `local` y `oracle` simultáneamente.

Después de `clean verify`, iniciar el JAR desde una terminal con `JWT_SECRET_BASE64` definida:

```powershell
java -jar target/foro-usuarios-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

El puerto es configurable con `PORT` o `--server.port=8083`. Para un puerto libre asignado automáticamente, usar `--server.port=0` y consultar la línea `Tomcat started on port ...` del registro de arranque.

## Perfil Oracle

Configurar por entorno `ORACLE_URL`, `ORACLE_USERNAME`, `ORACLE_PASSWORD` y `JWT_SECRET_BASE64`. El usuario debe pertenecer al esquema propio de usuarios; esta aplicación no consulta tablas de contenido. La URL puede incluir la ruta externa del Wallet mediante `TNS_ADMIN` según la configuración de la instancia.

```powershell
$env:ORACLE_URL = 'jdbc:oracle:thin:@SERVICIO?TNS_ADMIN=C:/Oracle/Wallet_Foro'
$env:ORACLE_USERNAME = 'FORO_USUARIOS'
$taskCredential = Get-Credential -UserName $env:ORACLE_USERNAME -Message 'Credenciales del esquema de usuarios'
$env:ORACLE_PASSWORD = $taskCredential.GetNetworkCredential().Password
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=oracle'
# Al detener la aplicación:
Remove-Item Env:ORACLE_PASSWORD
```

Los valores de servicio, ruta y usuario del ejemplo son marcadores y deben ajustarse. Las variables no tienen credenciales predeterminadas. Spring Boot no carga `.env` automáticamente.

Antes de iniciar con Oracle, ejecutar [database/01_estructura.sql](database/01_estructura.sql) como propietario del esquema propio de usuarios. El script crea USUARIOS y sus restricciones, sin eliminar tablas anteriores; se ejecuta una sola vez. Oracle usa `ddl-auto=validate` y no ejecuta SQL automáticamente. La conexión y las operaciones reales contra Oracle aún están pendientes de verificación.

## Registro de usuarios

POST `http://localhost:8081/api/auth/registro`, con Content-Type `application/json`:

```json
{
  "nombre": "Ana Pérez",
  "correo": "ana@example.test",
  "password": "ClaveSoloEjemplo123"
}
```

Contraseña ilustrativa para pruebas locales. Respuesta: 201, cabecera Location `/api/usuarios/{id}` y JSON con id, nombre, correo y rol. El ID es generado por la base y el rol es siempre USUARIO al registrarse. La cuenta se consulta mediante GET `/api/usuarios/{id}` con su token o el de un administrador; `/api/auth/me` consulta la identidad del token presentado.

Reglas de entrada:

- Nombre obligatorio, hasta 100 caracteres; se quitan espacios exteriores.
- Correo obligatorio y válido, hasta 254 caracteres; se quitan espacios exteriores y se convierte a minúsculas antes de validar y guardar. La unicidad se exige también mediante una restricción de base de datos para cubrir solicitudes simultáneas.
- Contraseña obligatoria, entre 8 y 72 bytes UTF-8. No se recorta ni normaliza; las letras acentuadas pueden ocupar más de un byte. BCrypt genera un hash con salt diferente por cuenta.
- Solo se aceptan nombre, correo y password como cadenas de texto. Campos desconocidos como rol, id, activo o passwordHash producen 400.

Un correo repetido produce 409. JSON malformado, tipos incorrectos y validaciones incumplidas producen 400. Los errores usan `application/problem+json`, con status, title y detail; los de validación incluyen errors por campo. No se devuelven la contraseña ni su hash.

Ejemplo PowerShell con el servidor iniciado:

```powershell
$taskBody = @{ nombre = 'Ana Pérez'; correo = 'ana@example.test'; password = 'ClaveSoloEjemplo123' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/auth/registro' `
    -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($taskBody))
```

## Inicio de sesión e identidad

POST `http://localhost:8081/api/auth/login`, con correo y password como cadenas JSON. Responde 200 con accessToken, tokenType `Bearer`, expiresIn `900` y usuario (id, nombre, correo y rol).

```powershell
$taskLoginBody = @{ correo = 'ana@example.test'; password = 'ClaveSoloEjemplo123' } | ConvertTo-Json
$taskLogin = Invoke-RestMethod -Method Post -Uri 'http://localhost:8081/api/auth/login' `
    -ContentType 'application/json' -Body $taskLoginBody
Invoke-RestMethod -Uri 'http://localhost:8081/api/auth/me' `
    -Headers @{ Authorization = "Bearer $($taskLogin.accessToken)" }
```

El correo se normaliza como en el registro; la contraseña se compara exactamente. Cuenta inexistente, inactiva o contraseña incorrecta devuelven el mismo error 401. Los datos de entrada inválidos producen 400.

Solo POST registro y POST login son públicos. Las demás rutas requieren `Authorization: Bearer <token>`. La API no usa sesiones, cookies de autenticación, formulario de login ni HTTP Basic. Los errores de autenticación usan ProblemDetail y `WWW-Authenticate: Bearer`.

El JWT utiliza HS256, issuer `foro-usuarios`, audiencia `foro-api`, sujeto igual al ID de usuario y vigencia de 900 segundos, con tolerancia temporal de 30 segundos. Firma inválida, vencimiento, ausencia de claims obligatorios o identificador inválido producen 401.

Cada petición autenticada consulta la cuenta en la base. Una cuenta inactiva o eliminada deja de autorizar con sus tokens anteriores; un cambio de rol se refleja en la siguiente petición. `/api/auth/me` devuelve solo id, nombre, correo y rol. No hay refresh token. Un cambio de contraseña no revocará por sí solo los JWT ya emitidos antes de su vencimiento; cambiar la clave de firma invalida todos los tokens.

## Gestión de cuentas y roles

Todas estas rutas requieren token. Las respuestas de cuenta incluyen solo id, nombre, correo y rol.

| Operación | Permiso | Resultado |
|---|---|---|
| GET /api/usuarios | Administrador | 200, lista de cuentas activas ordenada por ID |
| GET /api/usuarios/{id} | Propietario o administrador | 200, cuenta activa |
| PUT /api/usuarios/{id} | Propietario o administrador | 200, cuenta actualizada |
| DELETE /api/usuarios/{id} | Propietario o administrador | 204 sin cuerpo, cuenta desactivada |
| PUT /api/usuarios/{id}/rol | Administrador | 200, rol actualizado |

PUT de cuenta requiere nombre y correo; password es opcional. Omitirlo o enviar `null` conserva el hash actual. Si se proporciona una nueva contraseña, debe cumplir los límites del registro y no contener solo espacios. No se aceptan rol, activo o id en esta operación. Los correos se normalizan y su unicidad se conserva también al editar.

Ejemplo de cambio de rol: `{"rol":"ADMINISTRADOR"}` o `{"rol":"USUARIO"}`. Los valores numéricos, desconocidos o nulos producen 400. Una petición sin permiso devuelve 403; una cuenta inexistente o inactiva, 404; un correo duplicado o intento de quitar el último administrador activo, 409.

No se permite desactivar al último administrador activo ni cambiarlo a USUARIO. Las operaciones que pueden quitar ese privilegio bloquean primero las filas de administradores activos en orden de ID y hacen el recuento después; la actualización del destino también usa bloqueo y lectura actualizada. Esta regla se prueba con solicitudes concurrentes en H2 y debe comprobarse adicionalmente en Oracle.

DELETE conserva la fila con ACTIVO=0 para preservar la autoría histórica del foro. La cuenta deja de aparecer en el listado ordinario, no se consulta por ID y sus tokens reciben 401. Su correo permanece reservado: no se permite registrar otra cuenta con el mismo correo.

## Crear el primer administrador

El registro público siempre crea USUARIO. Para crear el primer administrador, habilitar explícitamente el inicializador en el servidor, con JWT_SECRET_BASE64 ya definida:

```powershell
$env:BOOTSTRAP_ADMIN_ENABLED = 'true'
$env:BOOTSTRAP_ADMIN_NOMBRE = 'Administrador'
$env:BOOTSTRAP_ADMIN_CORREO = 'admin@example.test'
$taskAdminCredential = Get-Credential -UserName $env:BOOTSTRAP_ADMIN_CORREO -Message 'Contraseña del primer administrador'
$env:BOOTSTRAP_ADMIN_PASSWORD = $taskAdminCredential.GetNetworkCredential().Password
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
# Después de detener el servidor:
Remove-Item Env:BOOTSTRAP_ADMIN_PASSWORD
Remove-Item Env:BOOTSTRAP_ADMIN_ENABLED
```

Para Oracle usar su perfil y las variables de conexión correspondientes. El inicializador está deshabilitado por defecto y requiere nombre, correo y contraseña válidos al habilitarlo. Si ya existe un administrador activo, no crea otro ni modifica contraseñas; si no hay administradores y el correo pertenece a una cuenta existente, se detiene sin promoverla. Las credenciales no se imprimen.

Ejecutar la inicialización con una instancia a la vez: los arranques simultáneos con el mismo correo pueden competir por la restricción de unicidad y uno fallar. En Oracle basta inicializar una vez porque los datos persisten. En H2 se pierde la base al detenerla, de modo que hay que inicializar de nuevo en cada arranque donde se necesite un administrador.

## Pruebas

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

`AplicacionTest` comprueba una conexión real a H2 con JDBC. `RegistroApiTest` cubre registro, hash y salt BCrypt, normalización, concurrencia, unicidad, campos reservados, límites UTF-8, validación y tipos JSON. `AuthApiTest` prueba el flujo completo de login e identidad y los rechazos de autenticación. `JwtServiceTest` comprueba firma, claims, clave y vencimiento con reloj fijo, sin esperas.

Las pruebas de contexto fuerzan los perfiles `local` y `test`, usan servicios y repositorios reales y no requieren Oracle ni una clave de producción. El perfil `test` solo aporta la clave pública de fixture desde los recursos de pruebas; no usarla en ejecución real. Los resultados están en `target/surefire-reports/`. `verify` genera el JAR ejecutable propio en `target/`.

`UsuariosApiTest` cubre permisos, consulta, edición, cambio de contraseña/rol, baja lógica y protección del último administrador. `AdministradorConcurrenteTest` prueba desactivaciones y cambios de rol simultáneos, y que una edición de perfil no restaure un rol anterior. Las pruebas del inicializador verifican habilitación explícita, idempotencia y credenciales inválidas. El perfil test también deshabilita el inicializador por defecto; sus pruebas de habilitación usan una base H2 aislada.

## Git y documentación

Rama inicial de trabajo: `desarrollo`. El repositorio remoto se configurará al preparar la publicación con autorización del usuario.

Las exclusiones cubren compilados, cachés, variables de entorno, Wallet y certificados. Revisar siempre `git status` antes de agregar archivos.

- [Diseño y límites del sistema](docs/diseno.md).
- [Contrato HTTP de identidad](docs/contrato-identidad.md).
- [Plan del servicio](docs/plan-implementacion.md).
- [Progreso de ejecución](docs/progreso.md).
- [Resultados verificados](docs/verificacion.md).

## Preparación para Oracle y entrega

La colección y entorno propios están en [postman/](postman/README.md). Las credenciales del administrador se completan solo localmente; los tokens usan variables de ejecución. Importar y ejecutar la colección completa en Collection Runner. La revisión de JSON/JavaScript y sus flujos HTTP locales ya se realizó; las aserciones de Postman real y Oracle siguen pendientes.

- [Conexión, esquema y pasos de persistencia Oracle](docs/oracle-y-entrega.md).
- [Consultas SQL de verificación sin secretos](database/03_verificacion.sql).
- [Matriz de evidencia y pendientes](docs/matriz-cumplimiento.md).
- [Guion conjunto de video de 8–9 minutos](docs/guion-video.md).

Los esquemas y tablas Oracle no se modificaron durante esta preparación. No se han publicado remotos, generado una entrega final ni grabado/subido el video. El alcance sigue siendo exclusivamente backend en dos repositorios separados.
