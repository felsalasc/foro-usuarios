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

## Registro de Task 4

Plan: docs/plan-implementacion.md, Task 4: Gestión de cuentas y privilegios.

Pre-flight: se consume el principal UsuarioResponse generado por el filtro y la tabla USUARIOS de Task 2. Los roles de cada petición se obtienen de la base mediante Task 3. No cambia el contrato de identidad ni el esquema SQL. Sigue siendo un repositorio Git independiente, sin proyecto Maven padre ni remoto configurado.

- Pruebas de permisos escritas antes de crear UsuarioController. Fallo inicial: 403 esperado al consultar otra cuenta, 404 recibido porque la ruta no existía. Log: .verificacion/usuarios-red.log.
- Se implementaron listado administrativo, consulta propia o administrativa, actualización, baja lógica y cambio administrativo de rol. Se conservan DTO de salida reducidos. Password omitido o null mantiene el hash; si se envía debe ser válido y no blanco. Los roles numéricos se rechazan al deserializar.
- Las operaciones que pueden quitar un administrador bloquean primero administradores activos en orden de ID, después el destino y realizan un recuento SQL actual. La edición de perfil bloquea y refresca el destino antes de modificarlo, para no restaurar roles desactualizados. Las operaciones administrativas sobre otras cuentas vuelven a comprobar el privilegio vigente tras esperar.
- Se añadió inicializador deshabilitado por defecto con credenciales desde entorno y validadas con las reglas del registro. No cambia cuentas existentes ni contraseñas al repetirse. La inicialización concurrente de múltiples instancias no está garantizada; puede producir un conflicto de unicidad, sin duplicar cuentas. README indica ejecutarla con una instancia a la vez.
- Revisión independiente de solo lectura: sin bugs bloqueantes identificados. Se amplió la cobertura de password null, despromociones simultáneas, operaciones mixtas y credenciales de inicialización inválidas. La revalidación del actor durante una espera está implementada; no se agregó una prueba con instrumentación especial para controlar ese instante.
- Suite final: clean verify, BUILD SUCCESS, código 0; 85 pruebas, cero fallos/errores/omitidas. Nuevas pruebas: UsuariosApiTest 21, AdministradorConcurrenteTest 4, AdministradorInitializerTest 5 y BootstrapDeshabilitadoTest 1. Se conservan las 54 anteriores. Log: .verificacion/usuarios-verify.log.
- HTTP real del JAR en H2: administrador inicial, usuarios normales, listado y edición, permisos 403, cambios de rol con tokens anteriores, conflicto 409 del último administrador, desactivación 204, token posterior 401 y cuenta inactiva 404. Claves y passwords de prueba aleatorios en memoria, sin impresión ni persistencia; proceso cerrado. Log: .verificacion/usuarios-http.log.
- Ruling: la comprobación de concurrencia en Oracle se mantiene pendiente para Task 5, junto con la conexión y evidencia Oracle. El bloque actual solo acredita código y pruebas H2; no se marca ese requisito como ejecutado.

Task 4: implementación y verificación local completas; concurrencia en Oracle pendiente. Cambios guardados en el commit local del bloque, sin publicación.

Task 5 sigue pendiente: Oracle real, colección Postman, datos de demostración y matriz. La base independiente de foro-contenido puede prepararse a continuación sin depender de esa comprobación; cada repositorio conservará su propia configuración y sus scripts. Ninguno accederá directamente a las tablas del otro.

## Preparación parcial de Task 5 — 31 de octubre de 2026

Se prepararon colección/entorno Postman propios, SQL de verificación, documentación Oracle, matriz y guion del video. Ver docs/verificacion.md para resultados: compilación independiente verde (85 usuarios / 138 contenido), 85 flujos HTTP locales verificados y scripts JSON/JS revisados. Se eligió carga de usuarios mediante inicializador + registro, alternativa prevista por el plan, en lugar de SQL con hashes predefinidos.

Newman no se instaló porque su descarga fue rechazada. El reproductor HTTP local no se presenta como ejecución del runtime Postman. Wallet existente sin credenciales ni confirmación de instancia/esquemas; se solicitó esa información sin pedir contraseñas por chat. No se conectó ni modificó Oracle. Este avance se registra en commit local propio; el bloque sigue incompleto y no se publican remotos ni una entrega final.

Actualización de preparación: el usuario confirmó BIBLIOTECA activa y ambos esquemas pendientes. Las instrucciones SQL como administrador están preparadas en docs/oracle-y-entrega.md; no se ejecutaron. La revisión independiente encontró que una aserción fallida con HTTP esperado no detenía la colección. Se reprodujo y corrigió: las guardas ahora detienen el recorrido ante aserciones, JSON o estructura inválidos. También se reprodujo y corrigió el escape de comillas/barras en la contraseña del login administrativo mediante JSON.stringify. Ocho casos Node focalizados pasaron RED→GREEN sobre esos scripts; son dobles mínimos del contrato de callbacks/variables, no el runtime Postman. Los 85 flujos HTTP se volvieron a verificar con los archivos finales. No hubo cambios de Java ni necesidad de repetir la suite Maven ya aprobada después de esos ajustes de colección.
