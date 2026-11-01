# Colección de usuarios

Importar foro-usuarios.postman_collection.json y local.postman_environment.json. Seleccionar el entorno de usuarios y definir adminCorreo y adminPassword como valores privados locales; el archivo entregado los deja vacíos. La cuenta administradora debe existir mediante el inicializador del servidor. No se necesita foro-contenido para esta colección.

Ejecutar la colección completa y en orden con **Collection Runner**, una iteración. Los tokens, IDs y contraseñas de prueba usan pm.variables: duran la ejecución y no se guardan en la colección o entorno exportado. Send de peticiones sueltas no conserva esas variables entre ejecuciones. No activar la persistencia de variables ni exportar entornos con credenciales. La contraseña de administrador se marca como secreta; no compartir su valor ni mostrar la consola con cabeceras en la grabación.

Las 31 solicitudes incluyen registro, login, identidad, listado, consulta, edición, roles vigentes y baja lógica; comprueban 200/201/204/400/401/403/404/409. Crean dos cuentas activas con correos únicos por ejecución y otra para probar la baja. Solo modifican sus cuentas nuevas; el administrador existente no se desactiva ni cambia de rol. Las cuentas de prueba permanecen en la base; cada repetición agrega registros. La colección no altera LIBROS ni tablas de contenido.

El cambio temporal de rol se aplica a la cuenta de prueba A y luego se restituye a USUARIO. La protección del último administrador se comprueba en JUnit; su demostración Oracle debe hacerse con cuentas y sesiones de prueba controladas, sin arriesgar la cuenta administrativa existente.

31 flujos HTTP se verificaron contra el JAR local sobre H2 usando un reproductor diagnóstico. Se revisaron JSON y sintaxis de scripts. **El runtime y las aserciones Postman aún no se ejecutaron**, y tampoco se acredita Oracle con esa verificación. Newman no se instaló porque su descarga fue rechazada.

Para Oracle: cambiar usuariosBaseUrl a la aplicación que corre con perfil oracle y esquema exclusivo de usuarios; ejecutar la colección, guardar el resumen del Runner y luego database/03_verificacion.sql. Reiniciar el servidor conservando esquema y clave de firma, iniciar sesión nuevamente y comprobar los IDs persistidos. Si se exportan evidencias, retirar cabeceras y valores secretos.

Referencias: [variables locales de Postman](https://learning.postman.com/docs/use/send-requests/variables/variables) y [ejecución con Newman](https://learning.postman.com/docs/collections/using-newman-cli/installing-running-newman). Newman es opcional; importar y ejecutar desde Postman no requiere instalarlo.

El recorrido programa su parada ante estados inesperados, aserciones fallidas o respuestas JSON/estructura inválidas. El login de administrador construye su cuerpo con JSON.stringify para conservar comillas y barras de la contraseña. Esas guardas tienen ocho comprobaciones Node focalizadas compartidas entre las dos colecciones; la verificación en el sandbox real sigue pendiente.

Actualización del 31 de octubre de 2026: el usuario ejecutó esta colección en el Runner real, con los servidores iniciados en perfil oracle: 36 aserciones aprobadas y cero errores/fallos visibles. Ver [evidencia del Runner](../docs/evidencias/2026-10-31-postman-usuarios.md). Las menciones previas a Postman pendiente describen la preparación anterior. Persistencia tras reiniciar y comprobaciones Oracle adicionales continúan pendientes.
