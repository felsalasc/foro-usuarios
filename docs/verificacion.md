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
