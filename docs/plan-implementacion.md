# Foro Usuarios Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. El usuario pide avanzar paso a paso; ejecutar únicamente el bloque correspondiente al paso actual. No publicar automáticamente.

**Goal:** Construir el repositorio independiente foro-usuarios con registro, autenticación y gestión de cuentas y roles persistidos en Oracle.

**Architecture:** Una aplicación Spring Boot independiente. Controladores, DTO, servicios, repositorios y seguridad dentro de cl.duoc.forousuarios. Expone identidad por HTTP a contenido sin compartir código o tablas.

**Tech Stack:** Java 17, Spring Boot 3.5.16 como versión inicial ya disponible localmente, Maven Wrapper 3.9.14, Web, Data JPA, Validation, Security, OAuth2 JOSE, Oracle JDBC, H2 para desarrollo, JUnit y MockMvc. Confirmar resolución de las dependencias al preparar la base.

**Spec:** docs/diseno.md y docs/contrato-identidad.md, incluidos en este repositorio. Las rutas se interpretan desde la raíz del repositorio.

## Global Constraints

- Raíz nueva: C:/Users/fasc8/OneDrive/Documentos/DUOC/DESARROLLO DE SOFTWARE III/SEMANA 2/foro-usuarios. Todas las rutas siguientes son relativas a esta raíz.
- Repositorio Git independiente, sin proyecto padre ni dependencia de foro-contenido o BIBLIOTECA. No crear remotos ni hacer push sin autorización.
- Dos roles: USUARIO y ADMINISTRADOR; no crear tabla de roles.
- BCrypt, JWT de 900 segundos, contrato HTTP de identidad exacto según el spec.
- Desde Task 3 todas las pruebas de contexto activan perfiles local y test, para que application-test.properties proporcione solo la clave de prueba; no introducir una clave predeterminada en recursos de producción.
- Datos privados por entorno. Oracle ddl-auto=validate. H2 no acredita Oracle.
- Tres usuarios activos como mínimo para la demostración. Conservar al último administrador activo.
- Git, Postman y evidencia real Oracle. No incluir Wallet, secretos, target o cachés en la entrega.

## Review Focus

1. Un registro con rol administrativo nunca escala privilegios: prueba registroRechazaRol.
2. La desactivación o cambio de rol debe reflejarse con tokens ya emitidos: pruebas cuentaDesactivadaPierdeAcceso y meDevuelveRolVigente.
3. Correos equivalentes y altas concurrentes no duplican cuentas: pruebas correoNormalizadoEsUnico y registroConcurrenteCorreoUnico.
4. Dos administradores que se desactivan concurrentemente no dejan cero administradores: prueba conservaAdministradorConcurrente.
5. Password UTF-8 mayor a 72 bytes no se trunca silenciosamente: prueba rechazaPasswordMultibyteExcesivo.

## Task 1: Base autónoma y configuración

**Files:** Crear pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties, .gitignore, README.md; src/main/java/cl/duoc/forousuarios/ForoUsuariosApplication.java; src/main/resources/application.properties, application-local.properties, application-oracle.properties; src/test/java/cl/duoc/forousuarios/AplicacionTest.java; docs/diseno.md; docs/contrato-identidad.md.

**Interfaces:** Produce aplicación foro-usuarios, puerto predeterminado 8081; perfil local H2 propio; Oracle desde ORACLE_URL, ORACLE_USERNAME, ORACLE_PASSWORD. No expone todavía funcionalidad de negocio. Git se inicializa en esta raíz en una rama de desarrollo.

- [x] Crear repositorio y Wrapper propio; pom con Java 17 y dependencias básicas Web, JPA, Validation, Oracle, H2 y Test. Security y JOSE se agregan en Task 3, al implementar sus filtros.
- [x] Escribir AplicacionTest con @SpringBootTest y @ActiveProfiles("local"), que compruebe arranque y conexión JDBC mediante SELECT 1. Ejecutar .\mvnw.cmd test; verificar fallo por ausencia de configuración Spring Boot antes de crear la clase principal.
- [x] Crear clase principal y perfiles. H2 local: memoria y create-drop; Oracle: validate, sin inicialización automática. PORT configurable. Guardar las copias de diseño y contrato.
- [x] Ejecutar .\mvnw.cmd clean verify. Esperado: BUILD SUCCESS y JAR ejecutable propio. Revisar git status y exclusiones antes de cualquier commit.
- [x] Registrar resultado y limitación: solo arranque local, sin funcionalidad ni Oracle verificado. Commit local del bloque después de verificar; sin push.

## Task 2: Registro y persistencia segura

**Files:** Crear bajo src/main/java/cl/duoc/forousuarios/: model/Usuario.java, model/Rol.java, repository/UsuarioRepository.java, dto/RegistroRequest.java, dto/UsuarioResponse.java, service/UsuarioService.java, controller/AuthController.java, exception/ApiExceptionHandler.java; tests RegistroApiTest.java en src/test/java/cl/duoc/forousuarios/; database/01_estructura.sql. Agregar spring-security-crypto al pom si aún no llega transitivamente.

**Interfaces:** UsuarioResponse(Long id, String nombre, String correo, Rol rol). UsuarioService.registrar(RegistroRequest): UsuarioResponse. POST /api/auth/registro según contrato. USUARIOS con correo único y activo; password_hash solo interno.

- [x] Escribir registroPersisteHashYDevuelve201: POST válido → 201 y Location, respuesta sin password/passwordHash, SQL con un hash BCrypt que valida con PasswordEncoder.matches. Ejecutar .\mvnw.cmd '-Dtest=RegistroApiTest' test: fallo por registro aún inexistente.
- [x] Implementar entidad, enum, DTO, repositorio, servicio, controlador y errores. Nombre 1–100, correo válido hasta 254, contraseña 8–72 bytes UTF-8. Normalizar correo. Rechazar campos JSON desconocidos; nunca aceptar rol desde registro.
- [x] Agregar y ejecutar antes de cada cambio registroRechazaRol (400 sin fila), correoNormalizadoEsUnico (409), registroConcurrenteCorreoUnico (una sola fila), rechazaPasswordMultibyteExcesivo (400), obligatoriosInvalidos (400 sin fila). Proteger unicidad con constraint y traducir el conflicto.
- [x] Crear SQL Oracle USUARIOS con identity, tamaños compatibles, rol restringido, activo y correo único. Ninguna instrucción elimina tablas ajenas.
- [x] Ejecutar .\mvnw.cmd test y registrar resultado; commit local del bloque verificado.

## Task 3: Login, JWT e identidad vigente

**Files:** Crear dto/LoginRequest.java, dto/LoginResponse.java, service/AuthService.java, security/JwtService.java, security/BearerTokenFilter.java, security/SecurityConfig.java, security/ApiSecurityErrorWriter.java; modificar controller/AuthController.java y pom.xml; crear tests AuthApiTest.java y JwtServiceTest.java; src/test/resources/application-test.properties con clave solo de prueba.

**Interfaces:** AuthService.login(LoginRequest): LoginResponse. JwtService.emitir(Long): String y validar(String): Long. GET /api/auth/me produce UsuarioResponse vigente. Spring Security solo permite registro y login anónimos; sin sesiones HTTP, con CSRF desactivado para esta API Bearer.

- [x] Escribir loginEmiteTokenYMeDevuelveIdentidad: registrar, login 200 con expiresIn=900 y Bearer, luego GET me 200 con id y rol. Ejecutar .\mvnw.cmd '-Dtest=AuthApiTest' test y observar el fallo antes de implementar.
- [x] Agregar Security y JOSE; implementar HS256, issuer, audience, exp y tolerancia de 30 segundos como define el contrato; clave obligatoria de al menos 32 bytes decodificados en ejecución. Inyectar Clock para probar el tiempo sin esperas.
- [x] Probar previamente y después: credencialesIncorrectas (401 genérico), firmaManipulada (401), tokenVencido (401), expAusente (401), issuerOAudienciaIncorrectos (401), sujetoInvalido (401) y meNoExponeSecretos.
- [x] Comprobar estado activo y rol en base de datos por petición; añadir cuentaDesactivadaPierdeAcceso y meDevuelveRolVigente. Errores de seguridad ProblemDetail, sin logs de secretos. Tests de registro mantienen endpoints públicos y clave exclusiva de prueba.
- [x] Ejecutar .\mvnw.cmd clean verify; registrar evidencia y commit local.

## Task 4: Gestión de cuentas y privilegios

**Files:** Crear controller/UsuarioController.java, dto/ActualizarUsuarioRequest.java, dto/ActualizarRolRequest.java; ampliar service/UsuarioService.java y repository/UsuarioRepository.java; crear bootstrap/AdministradorInitializer.java, tests UsuariosApiTest.java y AdministradorConcurrenteTest.java.

**Interfaces:** listar(): List<UsuarioResponse> solo ADMINISTRADOR; obtener/actualizar/desactivar con Long id y principal autenticado; cambiarRol(Long id, Rol rol, principal). PUT de cuenta recibe nombre, correo y password opcional; password omitido conserva el hash; no acepta rol ni activo. DELETE es desactivación persistente. Cuenta inactiva es 404 para consulta por id.

- [x] Escribir usuarioNoPuedeGestionarOtraCuenta (403), administradorPuedeListar (200), usuarioPuedeEditarSuCuenta (200) y usuarioPuedeDesactivarSuCuenta (204 seguido de 401 con token anterior); ejecutar antes de añadir endpoints.
- [x] Implementar permisos del spec. Añadir pruebas rolSoloLoCambiaAdministrador (403), conservaUltimoAdministrador (409), conservaAdministradorConcurrente (nunca cero administradores), cuentaInexistente (404), correoDuplicadoAlEditar (409) y JSON inválido (400). Serializar operaciones que puedan quitar administradores con bloqueo transaccional consistente sobre administradores activos; verificación de concurrencia H2 realizada. La comprobación en Oracle queda pendiente en el siguiente punto.
- [x] Implementar inicializador deshabilitado por defecto, habilitable explícitamente por entorno para crear administrador si no existe. Requiere nombre, correo y contraseña desde entorno; no los imprime y no ejecuta en arranque normal. Prueba: no crea sin habilitarse; al repetirse no duplica cuentas.
- [x] Ejecutar .\mvnw.cmd test y registrar resultado; commit local.

- [ ] Verificar concurrencia y bloqueos en Oracle real junto con Task 5. Pendiente; los resultados H2 no acreditan esta comprobación.

## Task 5: Oracle, Postman y evidencia del servicio

**Files:** Crear database/02_datos_demostracion.sql, database/03_verificacion.sql, postman/foro-usuarios.postman_collection.json, postman/local.postman_environment.json, docs/verificacion.md y docs/matriz-cumplimiento.md; completar README.md.

**Interfaces:** Repositorio ejecutable y documentado sin requerir contenido. Colección publica userId y accessToken como variables de ejecución, sin valores secretos guardados. SQL demo usa variables bind para hashes y datos de cuentas, documentando cómo generarlos localmente; el estado activo y el rol son explícitos.

- [x] Ejecutar .\mvnw.cmd clean verify: suite verde y JAR propio.
- [ ] Comprobar instancia disponible; preparar esquema dedicado de usuarios y ejecutar SQL sin modificar LIBROS. Generar hashes mediante la aplicación y cargar tres cuentas activas, o usar inicializador más registro según README. Verificar COUNT y roles.
- [ ] Ejecutar Postman real contra perfil Oracle: CRUD, login, me, 400/401/403/404/409; reiniciar y comprobar persistencia. Si Oracle no está accesible, registrar pendiente sin declarar cumplido.
- [ ] Comprobar funcionamiento del JAR desde su repositorio, revisar git diff --check y git status --short. Documentar comandos y resultados reales; commit local verificado. No crear remoto ni subir ZIP automáticamente.

## Self-review

Cobertura: Task 2 CRUD creación y seguridad de datos; Task 3 login e identidad; Task 4 roles y resto del CRUD; Task 5 Oracle, registros, Postman y evidencias. Task 1 asegura separación y compilación. Review Focus cubierto por tests nombrados en Tasks 2–4. La comprobación integrada con contenido pertenece al segundo plan; video final cubre ambos servicios.
