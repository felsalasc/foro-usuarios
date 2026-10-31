# Guion conjunto para Kaltura

Objetivo: 8–9 minutos dentro del rango oficial de 7–10. **Preparado, aún no grabado.** Grabar después de verificar Oracle y Postman; sustituir cualquier resultado pendiente por evidencia real antes de presentarlo.

| Tiempo | Mostrar y explicar |
|---|---|
| 0:00–0:45 | Caso del foro, alcance solo backend y selección de usuarios + contenido. Mostrar dos carpetas/repositorios, POM y puertos propios. Integración únicamente HTTP. |
| 0:45–2:15 | Usuarios en Postman: registro normal, login con variables privadas, me, consulta/edición y roles. Mostrar que USUARIO no administra otras cuentas y que el administrador tiene privilegios distintos. |
| 2:15–3:15 | Categorías: cinco registros, consulta autenticada y escritura administrativa. Publicar tema con autor obtenido de identidad, sin autorId en el body. |
| 3:15–4:30 | Buscar temas por texto + categoría; mostrar mayúsculas, acentos y literal %/_. Editar un tema propio y rechazar edición ajena con 403. |
| 4:30–5:30 | Comentar desde otro usuario, listar y editar solo su comentario. El dueño del tema no edita comentario ajeno; borrar un tema temporal elimina sus comentarios. Categoría en uso devuelve 409. |
| 5:30–7:00 | SQL en ambos esquemas Oracle: tablas propias, >=3 usuarios, >=5 categorías, >=3 temas/comentarios y cero huérfanos. Mostrar registros antes/después de reiniciar aplicaciones para acreditar persistencia. Nunca consultar hashes en pantalla. |
| 7:00–8:00 | Cuenta temporal desactivada: token previo recibe 401 en contenido. Usuarios detenido: petición de escritura recibe 503; conteo SQL no cambia. Restablecer el servicio. Mostrar resumen de Runner con resultados. |
| 8:00–8:45 | Historial Git de cada repositorio y enlaces publicados con autorización. Mostrar organización de código/SQL/Postman y entregables finales; confirmar que no se incluyeron secretos. |

Preparar datos extra para las eliminaciones y conservar los mínimos al final. Usar variables en los cuerpos/cabeceras de Postman, contraseña marcada como secreta y consola cerrada. No mostrar Wallet, claves, contraseñas, hashes ni Bearer resueltos. Los tokens/contraseñas de prueba de las colecciones solo duran su ejecución; preparar las peticiones manuales privadas antes de grabar.

Checklist pendiente: conexión Oracle, resumen Postman real, comprobación de persistencia y bloqueos, enlaces Git remotos, grabación, ZIP final y subida AVA. Este guion no acredita ninguno de esos pasos.
