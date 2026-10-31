# Oracle y demostración de usuarios

Preparación del 31 de octubre de 2026. La instancia BIBLIOTECA está activa según confirmación del usuario y faltan los dos esquemas. Hay Wallet local, pero esta sesión no dispone de credenciales Oracle. No se intentó iniciar sesión ni modificar la base. El siguiente paso es crear FORO_USUARIOS y FORO_CONTENIDO como usuarios distintos.

## Esquema exclusivo

Confirmar la instancia y Wallet vigentes. Si el esquema todavía no existe, un administrador de la base puede crearlo. Propuesta para Autonomous Database, donde el tablespace de aplicación es DATA; ajustar si la instancia usa otro tablespace. Reemplazar la clave privadamente por una que cumpla la política de la instancia, sin guardarla en el repositorio:

```sql
CREATE USER FORO_USUARIOS IDENTIFIED BY "<CLAVE_PRIVADA>"
  DEFAULT TABLESPACE DATA QUOTA 20M ON DATA;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO FORO_USUARIOS;
```

No ejecutar si el usuario ya existe. No dar permisos DBA ni acceso a tablas del esquema de contenido. Conectarse como FORO_USUARIOS y ejecutar database/01_estructura.sql una vez sobre el esquema vacío; comprobar SESSION_USER con database/03_verificacion.sql. No ejecutar la estructura como ADMIN ni modificar LIBROS. Hibernate valida el esquema y no ejecuta DDL automáticamente.

Referencia de administración: [crear usuarios en Autonomous Database](https://docs.public.content.oci.oraclecloud.com/iaas/autonomous-database-shared/doc/manage-users-create.html). El SQL anterior es una propuesta de privilegios para esta aplicación y no se ha ejecutado.

## Arranque y carga de cuentas

Usar JDK 17 y las instrucciones del README para definir ORACLE_URL, ORACLE_USERNAME, ORACLE_PASSWORD y JWT_SECRET_BASE64 privadamente en la terminal del servicio. Seleccionar perfil oracle; conservar la misma clave de firma mientras se comprueban reinicios. Wallet y contraseña quedan fuera del repositorio.

Habilitar una sola vez el administrador inicial mediante BOOTSTRAP_ADMIN_ENABLED y sus datos privados, como explica README. Esperar a que el login funcione: el puerto puede abrir antes de terminar el inicializador. El registro público crea las dos cuentas USUARIO restantes y calcula sus hashes BCrypt. Se elige esta carga por inicializador + API en vez de INSERT manual con hashes; es la alternativa prevista por el plan. No se entregan contraseñas ni hashes predefinidos.

Importar la colección y entorno de postman/ y ejecutar Collection Runner. Crea dos cuentas activas nuevas y una cuenta temporal que desactiva, conservando al menos tres activas junto con el administrador. Si se carga por solicitudes manuales, registrar dos usuarios y confirmar sus IDs/roles. Ejecutar database/03_verificacion.sql sin mostrar PASSWORD_HASH.

## Evidencia real pendiente

1. Ejecutar CRUD, login, me y restricciones 400/401/403/404/409 en Postman sobre el perfil Oracle; guardar resumen del Runner y capturas sin secretos.
2. Consultar las filas desde Oracle; detener y reiniciar solo la aplicación conservando esquema, variables y clave de firma. Repetir login y consulta de los mismos IDs: los registros deben persistir.
3. Probar desactivaciones/cambios de rol concurrentes con cuentas de prueba y sesiones separadas; debe quedar al menos un administrador activo. No usar como objetivo la única cuenta administrativa que permite recuperar el entorno.
4. Registrar versión de base, usuario de esquema, fecha, cantidades y resultados observados; sin contraseña, Wallet, hash o token. Las pruebas H2 no acreditan Oracle.

Los enlaces remotos, ZIP final y video se completan tras validar ambos servicios. No se publica automáticamente desde este trabajo.
