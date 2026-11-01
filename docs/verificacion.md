# Verificación de la base de Foro Usuarios

Fecha: 29 de octubre de 2026. Alcance: Tasks 1 a 4 del plan. Se conserva la evidencia histórica de cada bloque y se agrega la gestión de cuentas y roles. Las verificaciones locales usan H2; Oracle sigue pendiente.

## Resultados

| Comprobación | Resultado observado |
|---|---|
| JDK | Microsoft OpenJDK 17.0.20.1 |
| Maven | Wrapper configurado para 3.9.14 |
| Spring Boot | 3.5.16 |
| Prueba previa a la clase principal | Fallo esperado: falta @SpringBootConfiguration |
| clean verify final | BUILD SUCCESS, código de salida 0 |
| AplicacionTest | 1 prueba, 0 fallos, 0 errores, 0 omitidas |
| Conexión de prueba | JDBC real a H2, SELECT 1 correcto |
| Empaquetado | target/foro-usuarios-0.0.1-SNAPSHOT.jar ejecutable |
| Arranque del JAR | Perfil local, conexión a H2 y Tomcat operativo |
| HTTP del JAR | GET / devuelve 404 previsto; aún no hay endpoints de negocio |
| Revisión independiente | Sin defectos críticos ni importantes en este bloque |
| Oracle | Pendiente de conexión y operaciones reales |

El arranque HTTP utilizó 127.0.0.1 y un puerto libre asignado automáticamente (46282 en esta ejecución). El proceso de comprobación se cerró al terminar.

## Reproducir

Con JDK 17 y las dependencias accesibles desde Maven Central o la caché del equipo, ejecutar desde la raíz. Para el JAR actual definir primero JWT_SECRET_BASE64 mediante el procedimiento de README:

```powershell
.\mvnw.cmd clean verify
java -jar target/foro-usuarios-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.address=127.0.0.1 --server.port=0
```

Consultar el puerto indicado por Tomcat. En el bloque inicial `/` devolvía 404; en la versión actual es una ruta protegida y sin token devuelve 401. Probar registro, login y `/api/auth/me` según README. Detener con Ctrl+C.

La verificación en este equipo se hizo con `mvnw.cmd -o -B -ntp clean verify` y una caché Maven existente seleccionada mediante `-Dmaven.repo.local`. JAVA_HOME apuntó a un JDK 17 ya disponible en el equipo. Estas selecciones solo corresponden al comando de comprobación; el proyecto no declara dependencias de rutas de otros repositorios.

## Incidencias del entorno

La ejecución inicial restringida de clean verify abrió el contexto y H2, pero se detuvo por acceso denegado al canal de instrumentación Java. La repetición con permisos autorizados terminó correctamente. El intento restringido de abrir Tomcat también recibió Permission denied al establecer una conexión loopback; la repetición autorizada permitió el arranque y la comprobación HTTP.

OpenJDK emitió una advertencia de class sharing durante la instrumentación de pruebas; la suite terminó con cero errores. No fue necesario cambiar la configuración de la aplicación para resolver los bloqueos del entorno.

Los logs locales .verificacion/red.log, .verificacion/green.log, .verificacion/verify.log y .verificacion/arranque-jar.log se excluyen de Git por su extensión. Los informes de JUnit permanecen en target/surefire-reports, también excluidos.

## Límites del bloque

El resultado anterior corresponde al bloque inicial de arranque. Las verificaciones siguientes lo amplían. Colección Postman, datos de demostración y pruebas Oracle reales continúan pendientes.

## Verificación de registro

| Comprobación | Resultado observado |
|---|---|
| Prueba antes del endpoint | Fallo esperado: 201 requerido, 404 recibido |
| Suite completa final | 22 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Compilación y empaquetado | clean verify, BUILD SUCCESS, código 0 |
| Contraseñas | Hash BCrypt de 60 caracteres, matches correcto y salt diferente entre cuentas |
| Límites | Rechazo fuera de 8–72 bytes UTF-8; aceptación de 72 bytes multibyte |
| Correo | Normalización y unicidad persistente |
| Concurrencia | Dos altas del mismo correo: una 201, otra 409 y una sola fila |
| Campos reservados | rol, id, activo y passwordHash rechazados con 400 |
| JSON | Malformado y coerciones numéricas/booleanas rechazados con 400 |
| HTTP real del JAR | Registro 201 y Location; duplicado 409; validaciones 400 |
| Revisión independiente | Sin hallazgos confirmados en Task 2 |
| Oracle | SQL preparado; ejecución y CRUD real pendientes |

La suite usa H2 y no dobles del repositorio ni del encoder. La prueba concurrente tiene transacciones independientes; el error de unicidad de Hibernate en el log es intencional y su respuesta 409 está verificada.

Durante el desarrollo se detectó que desactivar coerciones escalares generales no impedía convertir números a String. Un test devolvió 201 para password numérico cuando debía devolver 400; JsonConfig configura específicamente los tipos textuales para rechazar Integer, Float y Boolean. La suite completa posterior confirmó la corrección.

La prueba HTTP externa inició el JAR en loopback y puerto aleatorio, envió cinco solicitudes y cerró el proceso al terminar. Los datos y contraseñas usados son solo de prueba y la base H2 se descarta al cerrar la aplicación.

Para reproducir: ejecutar clean verify y utilizar el POST documentado en README. Evidencia local: .verificacion/registro-red.log, .verificacion/registro-verify.log y .verificacion/registro-http.log; informes JUnit en target/surefire-reports/. Los archivos generados no se versionan.

## Verificación de autenticación

| Comprobación | Resultado observado |
|---|---|
| Suite completa final | 54 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Distribución | AplicacionTest 1, RegistroApiTest 21, AuthApiTest 18, JwtServiceTest 14 |
| Compilación | clean verify: BUILD SUCCESS y JAR ejecutable, código 0 |
| Login | Correo normalizado, BCrypt, accessToken Bearer y expiresIn=900 |
| Credenciales incorrectas | Mismo error 401 para contraseña incorrecta y cuenta inexistente/inactiva |
| Identidad | me devuelve solo id/nombre/correo/rol y consulta el rol vigente |
| Cuenta desactivada o eliminada | Token anterior rechazado con 401 |
| JWT | Firma, issuer, audiencia, iat, exp, sujeto y vencimiento comprobados |
| Tolerancia temporal | Aceptación a exp+29 s y rechazo a exp+31 s con Clock fijo |
| Clave | Base64 válida de mínimo 32 bytes; casos inválidos y cortos rechazados |
| HTTP real del JAR | Registro 201 → login 200 → me 200; errores 401 comprobados |
| Revisión | Un hallazgo P2 corregido: ausencia de aud ya produce 401 |
| Oracle | Pendiente de conexión y pruebas reales |

Se añadieron pruebas antes del código de autenticación. El primer fallo evidenció que el starter de seguridad bloqueaba el registro por defecto; la configuración explícita dejó públicos solo registro/login. Más adelante una prueba dirigida reprodujo el NullPointerException con aud ausente. La corrección fue seguida de la suite completa y de una prueba HTTP con JWT firmado sin audiencia, que devolvió 401 ProblemDetail.

La comprobación HTTP usó una clave aleatoria de 32 bytes generada en memoria por el script de diagnóstico local, sin escribirla ni mostrarla. Se comprobaron además Bearer en minúsculas, ausencia de cookie de sesión, rechazo de firma modificada y rechazo de expiración. El proceso se cerró al terminar. La identidad de cuentas desactivadas y los cambios de rol se comprobaron mediante integración con H2/JDBC en AuthApiTest; no se afirman como operaciones administrativas HTTP todavía.

La autoconfiguración de usuarios en memoria de Spring Boot está excluida; la autenticación consulta USUARIOS. Los tokens no se registran en logs. La clave de fixture bajo src/test/resources es pública, exclusiva de tests y no se empaqueta en el JAR.

Evidencia local: .verificacion/auth-red.log, .verificacion/auth-audience-red.log, .verificacion/auth-verify.log y .verificacion/auth-http.log. El script de diagnóstico y los logs permanecen excluidos de Git dentro de .verificacion/. La entrega no incorpora esas claves ni procesos locales.

## Verificación de cuentas y roles

| Comprobación | Resultado observado |
|---|---|
| Suite completa | 85 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Compilación | clean verify: BUILD SUCCESS, JAR generado, código 0 |
| Acceso a cuentas | Propietario y administrador autorizados; otro usuario recibe 403 |
| Listado | Solo administrador, cuentas activas y respuestas de cuatro campos |
| Edición | Nombre y correo normalizados; contraseña omitida o null conserva el hash |
| Cambio de contraseña | Credencial anterior rechazada, nueva aceptada; JWT previo conserva vigencia acordada |
| Cambio de rol | Solo administrador; rol vigente se refleja con tokens ya emitidos |
| Baja lógica | 204, fila ACTIVO=0 conservada; login/token rechazados, consulta 404 |
| Último administrador | Baja y despromoción rechazadas con 409 |
| Concurrencia H2 | Dos bajas, dos despromociones, operación mixta y edición frente a cambio de rol comprobadas |
| Inicialización | Deshabilitada por defecto; habilitación explícita; repetición sin duplicar ni cambiar password |
| Credenciales iniciales | Entradas inválidas impiden inicialización; no se promueve una cuenta existente |
| HTTP real | Inicializador, CRUD, permisos, roles y desactivación comprobados sobre el JAR |
| Revisión independiente | Sin defectos bloqueantes identificados |
| Oracle | Conexión, CRUD y concurrencia real pendientes |

Distribución: AplicacionTest 1, RegistroApiTest 21, AuthApiTest 18, JwtServiceTest 14, UsuariosApiTest 21, AdministradorConcurrenteTest 4, AdministradorInitializerTest 5 y BootstrapDeshabilitadoTest 1.

La prueba concurrente de dos bajas conserva exactamente un administrador activo; otra hace lo mismo con dos despromociones, y una tercera combina baja y despromoción. La prueba de edición simultánea con cambio de rol verifica que ambos cambios persistan sin restaurar el rol anterior. Todas se ejecutaron contra H2, con transacciones independientes, sin simular el repositorio.

La prueba HTTP inicia el JAR con administrador de prueba creado por el inicializador y credenciales aleatorias recibidas por entorno. Registra dos cuentas normales, comprueba listado administrativo y acceso propio, rechazo de acceso ajeno, edición, correo duplicado, cambio de rol con un token previo, pérdida inmediata de privilegio administrativo y protección del último administrador. Después desactiva una cuenta y comprueba 401 para su token y 404 para su consulta. El servidor se cierra al terminar.

La revisión señaló límites de cobertura, sin bugs demostrados. Se añadieron los casos de contraseña null, despromoción concurrente, operación mixta y credenciales inválidas de inicialización. Los arranques administrativos simultáneos siguen fuera de la garantía: con el mismo correo uno puede fallar por unicidad. Se documenta usar una instancia para la inicialización. La validación de los bloqueos en Oracle sigue pendiente y no se infiere de H2.

Evidencia local: .verificacion/usuarios-red.log, .verificacion/usuarios-verify.log y .verificacion/usuarios-http.log; informes JUnit en target/surefire-reports/. No se generaron remotos ni se publicó código.

## Preparación de Oracle y Postman — 31 de octubre de 2026

- clean verify offline ejecutado de nuevo desde cada raíz: usuarios 85 y contenido 138 pruebas, todas aprobadas, sin fallos/errores/omitidas. Ambos JAR propios generados. Log en cada repositorio: .verificacion/preparacion-entrega-verify.log.
- Colecciones y entornos independientes preparados, con 31 solicitudes de usuarios y 54 de contenido. Contraseñas de administrador vacías en los archivos; tokens y contraseñas generadas solo en pm.variables durante la ejecución. Las operaciones de baja y borrado se limitan a registros creados por el recorrido; se mantienen mínimos de demostración.
- JSON leído y scripts compilados con Node/vm: 32 scripts de usuarios y 55 de contenido (incluyen preparación inicial). No se ejecutó el runtime Postman ni sus aserciones.
- Reproductor diagnóstico HTTP: 31 + 54 solicitudes tomadas de los archivos de colección, estados esperados comprobados contra ambos JAR con H2. Además comprobó búsqueda literal combinada, autoría/fechas y mínimos de datos. Los IDs/tokens se extrajeron de respuestas reales. Los scripts Postman no se ejecutaron en este reproductor; sus aserciones requieren Collection Runner.
- La descarga de Newman fue rechazada y no se instaló; no se intentó instalarlo por otra vía. Se usaron únicamente herramientas existentes. Logs HTTP en contenido: .verificacion/postman-usuarios-http.log y .verificacion/postman-contenido-http.log. Diagnósticos, scripts de generación/reproducción y logs permanecen ignorados por Git.
- Se preparó database/03_verificacion.sql, solo lectura y sin mostrar hashes, junto con documentación de esquemas, matrices y guion. Existe un Wallet local, pero no hay credenciales Oracle configuradas en esta sesión ni confirmación de instancia/esquemas. No se inició sesión ni se ejecutó DDL o carga sobre Oracle.
- Oracle real, bloqueos, persistencia tras reiniciar, runtime/aserciones Postman, enlaces remotos, video y entrega final permanecen pendientes. No se asignan puntos de la pauta por anticipado ni se marca cerrado ese bloque.

Actualización: BIBLIOTECA activa según el usuario; FORO_USUARIOS y FORO_CONTENIDO aún sin crear. La revisión de colecciones identificó la parada incompleta ante aserciones; corregida junto con el escape JSON de credenciales. Ocho pruebas focalizadas en Node reprodujeron rol/estructura/JSON inválidos y contraseña con comillas/barras en ambas colecciones, y aprobaron tras corregir. Usan un doble mínimo de pm.test/variables y no sustituyen ejecutar Postman. Recompilación de scripts y 85 flujos HTTP finales aprobados. Oracle no se ha conectado ni modificado.

## Evidencia aportada: Postman usuarios sobre Oracle

31 de octubre de 2026: el usuario confirmó esquemas y tablas creados, cinco categorías cargadas y ambos servidores iniciados con perfil oracle. La consulta de USUARIOS mostró al administrador activo con ID 1. Posteriormente aportó el resumen real del Runner de usuarios: 31 solicitudes, 36 aserciones aprobadas, cero errores y cero fallos visibles. Se conserva la captura sin secretos en [evidencias/2026-10-31-postman-usuarios.md](evidencias/2026-10-31-postman-usuarios.md).

La ejecución real de Postman de usuarios deja de estar pendiente. Continúan pendientes la ejecución de contenido, la comprobación SQL final, reinicio/persistencia, concurrencia Oracle y la entrega. La evidencia proviene del usuario; no se afirma ejecución directa por el agente.

## Conteos Oracle confirmados por SQL — antes del reinicio

31 de octubre de 2026, evidencia aportada por el usuario: 7 cuentas: 1 ADMINISTRADOR activo, 4 USUARIO activos y 2 USUARIO inactivos. Total activo: 5. Las bajas lógicas conservan sus filas. Captura conservada en [evidencias/2026-10-31-oracle-conteos-usuarios.md](evidencias/2026-10-31-oracle-conteos-usuarios.md). El siguiente paso es reiniciar ambos servidores conservando sus variables y repetir las consultas sin volver a ejecutar las colecciones, para no agregar datos nuevos. Persistencia tras reinicio, integridad/bloqueos y verificaciones adicionales continúan pendientes.
