# Foro Usuarios

Microservicio de usuarios para el sistema de foros de DSY2205, Semana 2. Proyecto Spring Boot independiente con paquete base `cl.duoc.forousuarios` y puerto predeterminado **8081**.

## Estado actual

Primer bloque: base Maven, clase de arranque, perfiles local y Oracle y prueba de conexión local. Registro, login, gestión de cuentas y roles se implementarán en los siguientes bloques. En este estado no hay endpoints de negocio: una solicitud a `/` devuelve 404.

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

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

El perfil `local` usa H2 en memoria en modo Oracle. Hibernate crea y elimina el esquema local; los datos se pierden al detener la aplicación. La consola H2 está deshabilitada. Este perfil sirve para desarrollo, no acredita una conexión real a Oracle.

El perfil predeterminado es `local`. Si el entorno ya define `SPRING_PROFILES_ACTIVE`, ese valor prevalece; usar el perfil explícito mostrado para evitar confusiones. No activar `local` y `oracle` simultáneamente.

Después de `clean verify`, iniciar el JAR:

```powershell
java -jar target/foro-usuarios-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

El puerto es configurable con `PORT` o `--server.port=8083`. Para un puerto libre asignado automáticamente, usar `--server.port=0` y consultar la línea `Tomcat started on port ...` del registro de arranque.

## Perfil Oracle

Configurar por entorno `ORACLE_URL`, `ORACLE_USERNAME` y `ORACLE_PASSWORD`. El usuario debe pertenecer al esquema propio de usuarios; esta aplicación no consulta tablas de contenido. La URL puede incluir la ruta externa del Wallet mediante `TNS_ADMIN` según la configuración de la instancia.

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

Oracle usa `ddl-auto=validate` y no ejecuta SQL automáticamente. Los scripts para la tabla de usuarios se añadirán con la funcionalidad de registro. La conexión Oracle aún está pendiente de verificación.

## Pruebas

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
```

`AplicacionTest` fuerza el perfil `local`, inicia el contexto completo y comprueba una conexión real a H2 con JDBC. Los resultados están en `target/surefire-reports/`. `verify` genera el JAR ejecutable propio en `target/`.

## Git y documentación

Rama inicial de trabajo: `desarrollo`. El repositorio remoto se configurará al preparar la publicación con autorización del usuario.

Las exclusiones cubren compilados, cachés, variables de entorno, Wallet y certificados. Revisar siempre `git status` antes de agregar archivos.

- [Diseño y límites del sistema](docs/diseno.md).
- [Contrato HTTP de identidad](docs/contrato-identidad.md).
- [Plan del servicio](docs/plan-implementacion.md).
- [Progreso de ejecución](docs/progreso.md).
- [Resultados verificados](docs/verificacion.md).
