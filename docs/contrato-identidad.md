# Contrato HTTP de identidad entre usuarios y contenido

Fecha: 29 de octubre de 2026. Complementa [el diseño del sistema](diseno.md). Son decisiones técnicas para implementar el alcance aprobado, no requisitos textuales adicionales de la pauta.

## Responsabilidades

foro-usuarios emite y verifica tokens; es la única aplicación con acceso a USUARIOS. foro-contenido reenvía el Bearer token a usuarios y obtiene identidad y rol vigentes. No comparte claves de firma, tablas ni bibliotecas propias con usuarios.

## Registro y login

POST /api/auth/registro recibe JSON con nombre, correo y password. Rechaza campos desconocidos con 400, incluyendo rol e id. Asigna USUARIO y activo=true en el servidor. Normaliza correo quitando espacios exteriores y convirtiendo a minúsculas con Locale.ROOT. Responde 201 y Location: /api/usuarios/{id}, con id, nombre, correo y rol. Correo duplicado: 409. La unicidad también se protege mediante restricción de base de datos.

POST /api/auth/login recibe correo y password; responde 200:

```json
{
  "accessToken": "<token generado en ejecución>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "usuario": {
    "id": 1,
    "nombre": "Nombre de demostración",
    "correo": "usuario@example.test",
    "rol": "USUARIO"
  }
}
```

Cuenta inexistente, inactiva o contraseña incorrecta: 401 con mensaje genérico, sin revelar cuál de las tres condiciones ocurrió. No se registran passwords ni tokens en logs.

## Identidad vigente

GET /api/auth/me requiere Authorization: Bearer <token>. Responde 200 application/json con exactamente id (entero positivo), nombre, correo y rol (USUARIO o ADMINISTRADOR). No devuelve hash, password ni clave de firma. El sujeto del JWT es el id como cadena; la respuesta toma nombre, correo y rol de la base, no de datos enviados por el cliente.

JWT: firma HS256, issuer foro-usuarios, audiencia foro-api, iat y exp; duración 900 segundos y tolerancia temporal 30 segundos. JWT_SECRET_BASE64 contiene una clave aleatoria de al menos 32 bytes después de decodificar. Es obligatoria en ejecución y no se versiona. Las pruebas usan una clave exclusiva de prueba definida bajo src/test. Tokens sin exp, de otra audiencia o issuer, firma inválida o sujeto inválido producen 401. Usuarios comprueba que el sujeto corresponda a una cuenta activa en cada petición autenticada. No habrá refresh token en esta entrega.

## Integración en contenido

USUARIOS_BASE_URL configura la dirección del servicio. El cliente agrega /api/auth/me y solo reenvía Authorization; no recibe una URL desde la solicitud entrante. Tiempo de conexión predeterminado: 2 segundos; tiempo de lectura: 3 segundos, configurables. No hay reintentos automáticos ni caché de identidad en el alcance inicial.

- Falta Bearer o usuarios responde 401: contenido devuelve 401.
- Usuarios devuelve 200 válido: contenido crea su principal local con id y rol.
- La operación exige otro permiso: contenido devuelve 403.
- Conexión fallida, timeout, respuesta inesperada, JSON incompleto o rol desconocido: contenido devuelve 503; no ejecuta la operación.

Los endpoints del foro requieren autenticación también para las consultas. Ningún DTO de publicación o comentario acepta autorId; se obtiene exclusivamente del principal validado. Los clientes de pruebas simulan HTTP de usuarios con un servidor local de prueba; la compilación y la suite automatizada de contenido no necesitan que el otro repositorio exista o esté iniciado.

## Errores y cambios

Errores en application/problem+json con status, title y detail; en validación se agrega errors como objeto campo→mensaje. Los filtros de seguridad usan el mismo formato. DELETE exitoso devuelve 204 sin JSON.

Si se cambia la ruta de identidad, sus campos, sus roles o los códigos HTTP, actualizar las copias de este contrato y las pruebas de contrato de ambos repositorios en el mismo bloque de trabajo. Su integración se comprueba además con ambas aplicaciones reales antes de la entrega.
