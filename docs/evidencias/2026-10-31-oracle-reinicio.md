# Persistencia Oracle después del reinicio

Fecha: 31 de octubre de 2026.

Referencia previa: [conteos antes del reinicio](2026-10-31-oracle-conteos-usuarios.md).

Resultado previo y posterior reportado: 7 cuentas: 1 administrador activo, 4 usuarios activos y 2 usuarios inactivos.

Tras las instrucciones de detener y volver a iniciar ambos JAR con perfil oracle en sus terminales originales, sin volver a ejecutar las colecciones, el usuario confirmó: «LAS DOS CONSULTAS RESPONDEN LO MISMO», aclarando «que anteriormente».

La comprobación posterior está respaldada por confirmación textual del usuario; no se ha recibido una captura posterior ni se afirma que el agente haya ejecutado el reinicio. La evidencia acredita conservación de los conteos al reiniciar las aplicaciones, no un reinicio de la instancia Oracle ni comparación de cada valor de cada fila. Para el video conviene mostrar las consultas antes y después.

Pendientes adicionales: integridad referencial consultada por SQL, búsqueda CLOB larga, 503 sin escrituras al detener identidad y concurrencia/bloqueos Oracle.
