# Progreso de ejecución de Foro Usuarios

Plan: docs/plan-implementacion.md, Task 1: Base autónoma y configuración.

Fecha: 29 de octubre de 2026. El usuario solicita avanzar paso a paso; este bloque abarca únicamente la base de foro-usuarios.

## Decisiones de ejecución

- Repositorio nuevo e independiente en foro-usuarios, rama desarrollo. No se necesita derivar un worktree de BIBLIOTECA: no se modifica ese proyecto ni se comparte su historial.
- Ejecución directa en este chat. Registro de pasos y resultados en este archivo; logs de comandos en .verificacion/*.log, excluidos de Git por su extensión.
- Para verificar en este equipo se reutiliza temporalmente el JDK 17 y la caché de artefactos disponibles localmente. Ninguna ruta hacia BIBLIOTECA forma parte de pom.xml, recursos o instrucciones de instalación del proyecto.
- No se ha configurado remoto ni se ha publicado código.

## Revisión previa de interfaces

Task 1 produce arranque, perfiles, nombre de aplicación y puerto; Task 2 consumirá esa base. La clave JWT se agrega en Task 3 junto con su configuración de prueba. El perfil Oracle mantiene los nombres de variables acordados. Sin conflictos de interfaces para este bloque.

## Registro de pasos

1. Repositorio inicializado, Wrapper propio y dependencias declaradas.
2. AplicacionTest escrito antes de la clase principal. Ejecución inicial: fallo esperado por ausencia de @SpringBootConfiguration, código de salida 1. Log local: .verificacion/red.log.
3. Clase principal y perfiles local/Oracle creados. Documentación de ejecución añadida.
4. clean verify completado fuera del entorno restringido: BUILD SUCCESS; 1 prueba, 0 fallos, 0 errores, 0 omitidas. JAR ejecutable generado. Log local: .verificacion/verify.log.
5. JAR iniciado con Java 17, perfil local y H2 desde este repositorio. HTTP en 127.0.0.1:46282: respuesta 404 prevista en / porque aún no hay endpoints de negocio. Proceso cerrado al terminar. Log: .verificacion/arranque-jar.log.
6. Revisión independiente del bloque: sin hallazgos críticos ni importantes. Se corrigió la referencia local del contrato y se creó el registro de verificación enlazado desde README.

Task 1: complete. Verificación: Maven clean verify, 1/1 prueba aprobada; arranque del JAR y HTTP comprobados. El commit local de este bloque conserva los archivos verificados. Oracle no está comprobado.

La primera ejecución completa dentro del entorno restringido quedó detenida por acceso denegado al canal de instrumentación de Java. Se interrumpió esa sesión y se repitió la verificación con permisos autorizados, sin modificar código para eludir el bloqueo. El primer arranque HTTP restringido también falló por permiso de conexión loopback; la repetición autorizada fue exitosa. Estos incidentes y la advertencia de class sharing de OpenJDK son detalles del entorno, no resultados funcionales de Oracle.

## Registro de Task 2

Plan: docs/plan-implementacion.md, Task 2: Registro y persistencia segura.

Pre-flight: se consume la aplicación y el perfil local de Task 1; se produce POST /api/auth/registro y la tabla USUARIOS. No se añade todavía autenticación HTTP ni JWT. No hay conflicto con el contrato de identidad.

- Dependencia spring-security-crypto 6.5.11 resuelta mediante el BOM de Spring Boot. No estaba en la caché inicial y se descargó con autorización desde Maven Central.
- Pruebas escritas antes de la implementación. Primera prueba ejecutada: 201 esperado y 404 recibido, fallo por endpoint ausente, sin errores de infraestructura. Log local: .verificacion/registro-red.log.
- Se añadieron entidad, rol, repositorio, DTO, validación, servicio transaccional, controlador, BCrypt y manejo de errores. SQL Oracle con identity y restricciones de unicidad, rol y estado. La normalización ocurre antes de la validación.
- Ruling: agregar PasswordConfig, JsonConfig y validación PasswordUtf8 además de los archivos enumerados en el plan. Son unidades separadas para configuración de BCrypt, tipos JSON estrictos y límite de bytes; evita mezclar esas responsabilidades con el controlador. No cambia el contrato público.
- La primera suite completa detectó un defecto real: Jackson aceptó password numérico convirtiéndolo a texto. Se añadió JsonConfig para rechazar coerciones Integer/Float/Boolean hacia texto. El caso que fallaba fue verificado después con la suite verde, junto con variantes numéricas y booleanas.
- clean verify final: BUILD SUCCESS, código 0; 22 pruebas, cero fallos/errores/omitidas, JAR generado. El conflicto de unicidad registrado por Hibernate durante la prueba concurrente es el resultado provocado deliberadamente; la API devuelve 201 y 409 y se conserva una sola fila.
- Comprobación HTTP real del JAR en H2: creación 201 con Location y respuesta sin secretos, correo normalizado, duplicado 409, campo rol 400, contraseña numérica 400 y contraseña corta 400. Proceso cerrado al terminar. Log local: .verificacion/registro-http.log.
- Revisión independiente de solo lectura: sin hallazgos confirmados dentro del alcance de Task 2.

Task 2: complete. Evidencia: clean verify, 22/22 pruebas, HTTP de registro comprobado. Scripts Oracle preparados pero sin ejecución Oracle real. Los cambios se conservan en el commit local de este bloque.

## Registro de Task 3

Plan: docs/plan-implementacion.md, Task 3: Login, JWT e identidad vigente.

Pre-flight: el login consume USUARIOS, su correo normalizado y el PasswordEncoder de Task 2. El filtro produce un principal UsuarioResponse y ROLE_USUARIO o ROLE_ADMINISTRADOR a partir del registro vigente. El contrato HTTP se conserva. Las pruebas de contexto ahora activan local y test; la clave de fixture está solo en src/test/resources.

- Dependencias Spring Security y OAuth2 JOSE resueltas mediante el BOM. Descarga y pruebas con permisos autorizados.
- Prueba de flujo escrita primero. Fallo inicial: registro 201 esperado, 403 recibido después de agregar el starter de seguridad; faltaba la política pública de la API y aún no existían login/me. Log: .verificacion/auth-red.log.
- Se añadieron LoginRequest/LoginResponse, AuthService, JwtService, filtro Bearer, cadena de seguridad y escritura de errores ProblemDetail. Emisión HS256, issuer foro-usuarios, audiencia foro-api, iat/exp, 900 segundos y reloj inyectado. Clave Base64 obligatoria, mínimo 32 bytes decodificados, sin valor predeterminado de producción.
- Solo POST registro y POST login son públicos. Sin sesiones, formulario, Basic ni logout de sesión. Se configura CSRF desactivado para la API Bearer; no se agrega una política CORS abierta. Las credenciales incorrectas, cuentas inexistentes e inactivas tienen la misma respuesta de login y realizan una comparación BCrypt.
- La identidad consulta la cuenta activa y el rol actual en cada petición; no confía en roles del JWT. El login rechaza passwords mayores a 72 bytes antes de compararlos, evitando coincidencias por truncado.
- Hallazgo de revisión P2: JWT firmado sin aud podía causar NullPointerException y 500. La prueba audienciaAusenteEsTokenInvalido reprodujo el defecto antes de corregirlo; se agregó la guarda de ausencia y un caso HTTP en AuthApiTest. La prueba final y la suite completa pasaron después. Log de reproducción: .verificacion/auth-audience-red.log.
- Ruling: excluir UserDetailsServiceAutoConfiguration. La aplicación autentica únicamente contra sus cuentas Oracle/H2; el usuario en memoria autogenerado por Boot era innecesario y producía un secreto temporal en logs. No era un acceso alternativo porque Basic y formulario estaban deshabilitados. La exclusión no cambia el contrato de la API.
- Verificación final: clean verify, BUILD SUCCESS y código 0; 54 pruebas, 0 fallos, 0 errores, 0 omitidas. Incluye las 22 pruebas previas más 18 de API de autenticación y 14 de JWT. Log: .verificacion/auth-verify.log.
- HTTP real del JAR: registro 201, login 200, me 200 con cuatro campos y sin secretos; ausencia de token, credenciales incorrectas, firma manipulada, audiencia ausente y vencimiento devuelven 401. Clave aleatoria solo en memoria, perfil local con H2; proceso cerrado al terminar. Log: .verificacion/auth-http.log.
- Revisión independiente: el único hallazgo fue la audiencia ausente, corregido y verificado. La gestión administrativa y Oracle siguen fuera del alcance de este bloque.

Task 3: complete. Evidencia: 54/54 pruebas, JAR generado y flujo HTTP real comprobado con H2. Documentación actualizada para exigir JWT_SECRET_BASE64 en ejecución. Commit local del bloque sin publicación.

Las tareas 4 y 5 del plan continúan pendientes. Próximo bloque: gestión de cuentas, roles y protección del último administrador.
