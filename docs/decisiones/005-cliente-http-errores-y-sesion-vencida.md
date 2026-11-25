# 005. Cliente HTTP: motor, errores y sesión vencida

**Estado:** aceptada

## Contexto

La app llama a los servicios de catálogo y turnos. Ambos responden sus errores como `application/problem+json` con un `code`. Un `401` puede significar dos cosas distintas: `UNAUTHORIZED` (el token falta, venció o es inválido) o `INVALID_CREDENTIALS` (clave incorrecta al iniciar sesión). Los servicios no distinguen «vencido» de «inválido» a propósito.

## Decisión

- Ktor con el motor CIO y JSON (`ignoreUnknownKeys`), con 10 segundos para conectar y 15 por pedido.
- El token sale de la sesión en cada pedido (`defaultRequest`), no una sola vez al crear el cliente.
- Toda respuesta no exitosa se convierte en `ApiException(status, code, fieldErrors)`. Si el cuerpo no es un `problem+json` válido, queda solo el estado HTTP.
- El `code` es un conjunto abierto. `ErrorMessages` traduce los códigos de las tablas de errores de los contratos y, ante uno desconocido, decide por el estado HTTP con un mensaje genérico.
- El `failureReason` de una reserva también es texto abierto y no un enumerado (si la cátedra rechaza o vence un proceso, es el texto que ella informa): se muestra un mensaje genérico si no se reconoce. Desde la [decisión 016](016-estados-de-error-y-vencimiento.md) los motivos documentados tienen su mensaje, solo para elegir el texto.
- Un `401` vacía la sesión solo si el código es `UNAUTHORIZED` y el pedido llevaba token.
- No se instala el plugin de logs de Ktor: el JWT viaja en una cabecera y en el cuerpo de `POST /api/authenticate`, y sin logs no puede filtrarse.

## Alternativas descartadas

- El plugin `Auth` de Ktor con bearer: está pensado para renovar tokens y los servicios no emiten tokens de renovación.
- OkHttp como motor: no hay una diferencia comprobada que nos afecte; se evalúa al implementar el stream SSE.
- El plugin `Logging` de Ktor, aun filtrando cabeceras: exige acordarse de ocultar cada campo sensible.

## Consecuencias

El timeout de 15 segundos por pedido no sirve para el stream SSE: se ajustará por pedido cuando se implemente. Un código de error nuevo de un servicio se muestra con el mensaje genérico hasta que se agregue a `ErrorMessages`.
