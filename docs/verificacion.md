# Verificación de la base de Foro Usuarios

Fecha: 29 de octubre de 2026. Alcance: Tasks 1 y 2 del plan. Se conserva la evidencia histórica del arranque y se agrega la verificación del registro.

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

El resultado anterior corresponde al bloque inicial de arranque. La verificación de registro siguiente lo amplía. Login, JWT, gestión de cuentas y roles, colección Postman y pruebas Oracle reales continúan pendientes.

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
