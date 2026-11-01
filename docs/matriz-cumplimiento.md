# Matriz de evidencia de usuarios

Actualizada: 31 de octubre de 2026. Pesos de la pauta conjunta; esta tabla no asigna una nota ni acredita por adelantado la evaluación.

| Criterio | Peso | Evidencia disponible | Pendiente |
|---|---:|---|---|
| Git | 10 | Repositorio propio, rama desarrollo e historial local | Enlace remoto autorizado y evidencia de uso |
| CRUD Oracle | 20 | Conexión Oracle, administrador consultado por SQL y recorrido CRUD/roles Postman ejecutado | Conteo SQL final y persistencia real tras reiniciar |
| Servicios y consultas | 30 | Registro, login, identidad, gestión de cuentas/roles y concurrencia local; servicio separado de contenido | Demostración conjunta y bloqueos en Oracle |
| JSON y Postman | 25 | Runner real: 31 solicitudes, 36 aserciones aprobadas y cero errores; captura guardada | Integrar evidencia en grabación y entrega |
| Video | 15 | Guion de 8–9 minutos preparado para ambos servicios | Grabación Kaltura de 7–10 minutos y entrega AVA |

Registros mínimos: inicializador + registro API permiten tres cuentas activas con dos roles. Confirmarlos en Oracle mediante database/03_verificacion.sql. No se entregan hashes/contraseñas de ejemplo. La protección del último administrador tiene pruebas locales; Oracle continúa pendiente.

La ejecución HTTP diagnóstica inicial no usó el runtime Postman; posteriormente el usuario aportó su ejecución real aprobada, conservada en docs/evidencias/. Newman no se instaló por rechazo de su descarga. Ver docs/verificacion.md para resultados y límites de la evidencia.
