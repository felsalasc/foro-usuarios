# Verificación de la base de Foro Usuarios

Fecha: 29 de octubre de 2026. Alcance: Task 1 del plan, base autónoma y configuración.

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

Con JDK 17 y las dependencias accesibles desde Maven Central o la caché del equipo, ejecutar desde la raíz:

```powershell
.\mvnw.cmd clean verify
java -jar target/foro-usuarios-0.0.1-SNAPSHOT.jar --spring.profiles.active=local --server.address=127.0.0.1 --server.port=0
```

Consultar el puerto indicado por Tomcat y hacer una solicitud a `/`: por ahora corresponde un 404. Detener con Ctrl+C.

La verificación en este equipo se hizo con `mvnw.cmd -o -B -ntp clean verify` y una caché Maven existente seleccionada mediante `-Dmaven.repo.local`. JAVA_HOME apuntó a un JDK 17 ya disponible en el equipo. Estas selecciones solo corresponden al comando de comprobación; el proyecto no declara dependencias de rutas de otros repositorios.

## Incidencias del entorno

La ejecución inicial restringida de clean verify abrió el contexto y H2, pero se detuvo por acceso denegado al canal de instrumentación Java. La repetición con permisos autorizados terminó correctamente. El intento restringido de abrir Tomcat también recibió Permission denied al establecer una conexión loopback; la repetición autorizada permitió el arranque y la comprobación HTTP.

OpenJDK emitió una advertencia de class sharing durante la instrumentación de pruebas; la suite terminó con cero errores. No fue necesario cambiar la configuración de la aplicación para resolver los bloqueos del entorno.

Los logs locales .verificacion/red.log, .verificacion/green.log, .verificacion/verify.log y .verificacion/arranque-jar.log se excluyen de Git por su extensión. Los informes de JUnit permanecen en target/surefire-reports, también excluidos.

## Límites del bloque

La aplicación contiene únicamente la base de arranque. No se han implementado todavía registro, login, roles, tablas de usuarios, colección Postman ni scripts SQL. Este resultado no acredita aún el CRUD ni la conexión Oracle exigidos en la evaluación.
