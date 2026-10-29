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

Las tareas 2 a 5 del plan continúan pendientes.
