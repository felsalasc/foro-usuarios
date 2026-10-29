# Diseño propuesto del foro para Semana 2

Fecha: 29 de octubre de 2026.

Este documento fija el alcance propuesto para implementar paso a paso la evaluación de DSY2205. La propuesta selecciona los microservicios de usuarios y foro. No representa una implementación ni evidencia de cumplimiento. Las decisiones técnicas indicadas son propuestas del asistente; las instrucciones oficiales siguen siendo la autoridad sobre la evaluación.

## Fuentes y restricciones oficiales

Fuentes: los tres DOCX ubicados en SEMANA 2: instrucciones específicas B, pauta sumativa y guía de aprendizaje.

- Caso: sistema de foros en línea, sin frontend.
- Implementar dos de los tres microservicios indicados.
- Usar Java, Spring Boot, Oracle, REST, Git y Postman.
- Incluir al menos cinco categorías y dos roles con privilegios diferentes aplicados por el backend.
- Manipular al menos tres registros por tabla.
- Entregar código fuente y script SQL en ZIP o RAR, enlaces Git y grabación en AVA.
- Presentar en Kaltura durante 7 a 10 minutos: diseño, funcionamiento, pruebas Postman y almacenamiento real en Oracle.
- No integrar pagos.
- La pauta pondera Git 10, CRUD Oracle 20, consultas de los servicios 30, JSON y Postman 25, video 15.

## Alcance elegido en la propuesta

1. Usuarios: registro, consulta, edición, eliminación lógica e inicio de sesión.
2. Foro: categorías, búsqueda y visualización de temas, publicación y comentarios; edición y eliminación del contenido propio.

No se construirá el tercer microservicio de moderación ni se incluirán baneos. Tampoco se requieren frontend, pagos, gateway, descubrimiento de servicios o despliegue público para este alcance.

## Roles y permisos propuestos

Los roles serán un atributo del usuario, no una tabla adicional.

| Acción | Usuario | Administrador |
|---|---|---|
| Consultar y modificar su cuenta | Sí | Sí |
| Eliminar su cuenta mediante desactivación | Sí | Sí |
| Consultar y gestionar otras cuentas | No | Sí |
| Modificar roles | No | Sí |
| Consultar categorías, temas y comentarios | Sí | Sí |
| Crear, editar y eliminar categorías | No | Sí |
| Publicar temas y comentarios | Sí | Sí |
| Editar y eliminar sus temas y comentarios | Sí | Sí |
| Modificar contenido de otros usuarios | No | No, fuera de este alcance |

El registro público asigna siempre USUARIO; el cliente no puede otorgarse ADMINISTRADOR. La cuenta inicial de administrador se prepara mediante un comando de inicialización que recibe la contraseña desde el entorno y usa el mismo algoritmo de hash que el registro. Las contraseñas y sus hashes no se incluyen en las respuestas JSON. No se permite desactivar ni quitar el rol al último administrador activo.

## Estructura acordada

El usuario exige dos aplicaciones completamente separadas en dos repositorios distintos. No se creará un proyecto Maven padre ni un repositorio Git contenedor. BIBLIOTECA se conserva como referencia técnica.

- SEMANA 2/foro-usuarios: repositorio Git propio, aplicación Spring Boot, paquete cl.duoc.forousuarios, puerto configurable con valor predeterminado 8081.
- SEMANA 2/foro-contenido: repositorio Git propio, aplicación Spring Boot, paquete cl.duoc.forocontenido, puerto configurable con valor predeterminado 8082.

Cada repositorio contendrá su propio pom.xml, Maven Wrapper, .gitignore, README, código, perfiles local y Oracle, pruebas, scripts SQL en database/, colección Postman en postman/ y documentación en docs/. Ninguno dependerá de archivos, paquetes, compilados o configuración del otro repositorio, ni de BIBLIOTECA. Las herramientas locales de verificación pueden reutilizar el JDK y la caché Maven disponibles; no serán dependencias del proyecto publicado.

Cada aplicación se compila, se prueba y se inicia por separado. Tendrá credenciales de su propio esquema Oracle. La única integración funcional entre aplicaciones será HTTP. La independencia de compilación no implica que el foro pueda autorizar operaciones si el servicio de identidad está caído.

La documentación central ubicada fuera de ambos repositorios coordina la evaluación. Antes de publicar, se incluirán en cada repositorio copias suficientes de su diseño y contrato para poder comprenderlo al clonarlo por separado. Los repositorios remotos y la publicación se prepararán posteriormente con autorización del usuario.

Cada aplicación tendrá controlador, DTO, servicio, repositorio, entidades, configuración de seguridad y manejo de errores. Usará Java 17 y partirá de las versiones del proyecto local como referencia, verificando su resolución y compatibilidad antes de fijarlas. Spring Boot, Oracle JDBC, JPA, Validation y Security se gestionarán desde Maven.

No se requiere migrar ni reutilizar las entidades Libro. Se pueden adaptar los patrones de validación, perfiles Oracle y excepciones del proyecto anterior.

## Identidad entre los servicios

Decisión técnica propuesta: usuarios emite un JWT firmado con duración de 15 minutos después de comprobar la contraseña con BCrypt. La clave se recibe desde una variable de entorno, sin valores de producción predeterminados. El identificador de usuario es el sujeto del token.

El foro reenvía el Bearer token a GET /api/auth/me en usuarios para comprobar firma, vencimiento, cuenta activa y rol vigente antes de autorizar una petición protegida. El foro obtiene autor y rol de esa respuesta y nunca confía en un autorId o rol enviado en el cuerpo. No necesita conocer la clave de firma ni acceder a tablas de usuarios. La consulta de identidad usa un tiempo de espera configurable; una caída de usuarios impide escrituras y devuelve 503, sin aceptar la operación.

El registro y el login son públicos. El resto de las operaciones requieren identidad válida. Un token inexistente, inválido, vencido o de cuenta inactiva produce 401; identidad válida sin permiso produce 403. No se añade tabla de sesiones ni lista de revocación. Desactivar una cuenta bloquea sus tokens mediante la comprobación de estado; cambiar contraseña no revoca tokens ya emitidos antes de su vencimiento, limitación que debe quedar documentada.

## Modelo de datos

Usuarios es dueño de USUARIOS: id, nombre, correo único, password_hash, rol y activo. El correo se normaliza de forma coherente antes de verificar unicidad. Su eliminación lógica mantiene la autoría histórica y rechaza nuevos accesos.

Foro es dueño de CATEGORIAS, TEMAS y COMENTARIOS. Una categoría tiene id, nombre único y descripción. Un tema tiene id, título, contenido, categoria_id, autor_id y fecha de creación. Un comentario tiene id, contenido, tema_id, autor_id y fecha de creación.

Las claves foráneas entre categorías, temas y comentarios se mantienen en la base del foro. No se crean claves foráneas entre servicios ni se consulta la tabla USUARIOS desde foro. Cada servicio tendrá credenciales y tablas bajo su responsabilidad; puede usarse una sola Autonomous Database con dos esquemas de aplicación.

Decisiones de eliminación: categoría con temas devuelve 409; tema con comentarios se elimina junto con sus comentarios en una transacción, solo por su autor; comentario solo puede eliminarlo su autor. La categoría inexistente al publicar y el tema inexistente al comentar producen 404. El autor de un tema no puede editar comentarios de otra persona.

## Contrato HTTP propuesto

| Servicio | Rutas y operaciones |
|---|---|
| Usuarios | POST /api/auth/registro; POST /api/auth/login; GET /api/auth/me |
| Usuarios | GET /api/usuarios; GET, PUT, DELETE /api/usuarios/{id}; PUT /api/usuarios/{id}/rol |
| Foro | GET, POST /api/categorias; GET, PUT, DELETE /api/categorias/{id} |
| Foro | GET, POST /api/temas; GET, PUT, DELETE /api/temas/{id} |
| Foro | GET, POST /api/temas/{temaId}/comentarios; GET, PUT, DELETE /api/comentarios/{id} |

El listado de usuarios es exclusivo del administrador. Consultar, actualizar o desactivar una cuenta requiere ser su propietario o administrador; la actualización ordinaria no permite cambiar rol. La ruta de rol requiere administrador. Las cuentas inactivas no aparecen en el listado ordinario y su consulta por id devuelve 404.

GET /api/temas admite texto y categoriaId, combinables. Busca sin distinguir mayúsculas sobre título y contenido; aplica además la categoría si se proporciona. Orden por fecha descendente e id descendente para desempatar. Sin coincidencias devuelve 200 con un arreglo vacío. Una categoría inexistente como filtro devuelve un arreglo vacío; en creación o edición del tema produce 404. Un tema solo cambia de categoría si la nueva categoría existe.

Creación: 201 y Location. Consulta y actualización: 200 y JSON. Eliminación: 204 sin cuerpo. Errores coherentes en JSON: 400 para entradas inválidas, 401, 403, 404, 409 y 503 según lo indicado. Las consultas listadas devuelven arreglos; la paginación se deja fuera del alcance inicial. Validaciones mínimas propuestas: nombre de usuario 1–100, correo válido hasta 254, contraseña 8–72 bytes UTF-8; categoría 1–100, descripción hasta 500; título 1–200; contenido de tema 1–10000 y comentario 1–2000 caracteres. Los textos obligatorios no aceptan solo espacios. El límite de bytes se verifica antes de usar BCrypt.

## Datos y Oracle

Categorías iniciales propuestas: Programación, Bases de datos, Tecnología, Videojuegos y Consultas académicas. Son decisiones de demostración y pueden cambiarse.

Preparar al menos tres usuarios activos, incluyendo un administrador y dos usuarios; cinco categorías; tres temas y tres comentarios con relaciones válidas. El video debe mostrar modificaciones sin dejar las tablas por debajo del mínimo: crear registros adicionales para las demostraciones de eliminación.

Los scripts SQL crean tablas, restricciones e índices y permiten cargar datos reproducibles. La carga de usuarios utiliza hashes generados localmente por la aplicación con credenciales ingresadas por entorno; no se versionan contraseñas reales. Los scripts se ejecutan en esquemas dedicados, sin eliminar LIBROS ni otras tablas existentes.

El perfil Oracle recibe URL, usuario y contraseña por entorno; usa validación del esquema, sin creación automática destructiva. Wallet y credenciales permanecen fuera de Git y del ZIP. H2 solo es una ayuda de desarrollo. La conexión Oracle actual debe comprobarse antes de decidir si es necesario crear una instancia nueva.

## Verificación y evidencia

Pruebas automatizadas: CRUD, correo duplicado, validaciones, contraseña protegida, login correcto e incorrecto, ausencia y vencimiento de tokens, cuenta inactiva, permisos entre roles, acceso a contenido ajeno, búsqueda combinada, relaciones inexistentes, reglas de eliminación y dependencia de identidad caída.

Pruebas reales con Oracle y Postman: arrancar ambos servicios, registrar e iniciar sesión, demostrar permisos, crear/consultar/editar/eliminar, buscar y comentar. Verificar en SQL los datos y sus cantidades; reiniciar aplicaciones y comprobar persistencia. Una prueba H2 exitosa no acredita Oracle.

Registrar para cada criterio su componente, evidencia, estado y observaciones. El ZIP final contendrá código, scripts y documentación de ejecución; se excluyen secretos, Wallet, cachés y compilados innecesarios. Preparar un guion de 7 a 10 minutos que cubra ambos servicios, permisos, consultas, Postman, Oracle y Git. La publicación de código y la entrega al AVA requieren autorización del usuario.

## Próximo paso

Revisar esta definición de alcance. Después preparar el plan de implementación por bloques, con archivos y verificaciones concretas; ejecutar primero usuarios y autenticación, luego foro, Oracle y evidencias. Este documento no autoriza por sí mismo una publicación ni demuestra que la solución esté construida.
