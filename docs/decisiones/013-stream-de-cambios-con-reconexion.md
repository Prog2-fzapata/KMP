# 013. Stream de cambios: una conexión mientras la pantalla mira, reconexión creciente y comprobación de sesión

**Estado:** aceptada

## Contexto

`GET /api/reservations/stream` abre una conexión `text/event-stream` que avisa cada cambio de las reservas del usuario, incluida la llegada del pedido de teléfono. Manda un comentario `:ping` al conectar y cada 25 segundos, no guarda historia (ignora `Last-Event-ID`: un aviso perdido se recupera consultando), admite hasta 5 conexiones por usuario y, cuando vence el JWT, cierra la conexión en el primer latido: la cabecera `200` ya salió, así que no puede responder un `401`. Los servicios no emiten tokens de renovación ([decisión 005](005-cliente-http-errores-y-sesion-vencida.md)), que dejó dos pendientes para esta decisión: el tope de 15 segundos por pedido no sirve para una conexión sin fin, y quedaba por ver si el motor CIO alcanza o hace falta OkHttp.

## Decisión

- Se usa el plugin SSE de Ktor, que viene dentro de `ktor-client-core`, con el motor CIO. No hace falta OkHttp: contra los servicios reales la conexión sigue abierta más de 34 segundos (pasa un `:ping`) y llega el aviso de una reserva.
- El pedido del stream no tiene tope de tiempo (ni el del pedido ni el de silencio entre mensajes); el tope general de 15 segundos no se toca.
- `StreamConnection` abre una conexión y avisa `Connected` al abrirse y `Updated(id)` por cada `reservation-updated`; ignora los `:ping` y cualquier otro evento. El plugin envuelve todo error en una `SSEClientException`: se desenvuelve para que el resto de la app vea el `ApiException` o el corte de siempre.
- El aviso solo aporta el `id`: la reserva se lee por REST, que es la fuente de verdad. Al conectar o reconectar también se lee, porque no hay historia.
- `ReservationStream` reconecta cuando la conexión se cierra o falla, con esperas de 2, 4 y 8 segundos (se queda en 8, y vuelve a 2 al conectar bien). Antes de cada reconexión comprueba la sesión con un pedido normal (`GET /api/reservations`): un `401` vacía la sesión, lo hace el cliente HTTP como en cualquier pedido, y termina el stream. Hace falta porque el `401` del apretón de manos llega envuelto y sin `code`, y por la decisión 005 un `401` sin `UNAUTHORIZED` no vacía la sesión.
- Solo se reintenta lo que esperar puede arreglar: un corte, un timeout o una respuesta de error del servidor. Una cancelación o un error de la app (un motor sin SSE, por ejemplo) no se tapan con reintentos.
- La conexión dura lo que la pantalla que escucha: un `LaunchedEffect` la abre y se cancela al irse la pantalla o al llegar la reserva a un estado final. Verificado en el emulador: una conexión con la reserva abierta y ninguna al salir. Hoy escucha una sola pantalla, así que hay una conexión por app.

## Alternativas descartadas

- Consultar la reserva cada pocos segundos: más pedidos, una espera que se nota y un código que el stream reemplaza.
- Dejar que el plugin reconecte solo (`maxReconnectionAttempts`): no comprueba la sesión ni deja controlar la espera.
- Conectar al iniciar sesión y mantener la conexión siempre: gasta una conexión fuera de una reserva y exige un alcance de aplicación que hoy no se necesita.
- Probar la conexión con el motor falso de Ktor: no soporta SSE. La conexión real se verificó contra los servicios, y la política de reconexión se prueba con conexiones guionadas y tiempo virtual.

## Consecuencias

Con el token vencido, el servidor admite 60 segundos de desfase de reloj: reconectar puede aceptarse y cerrarse otra vez antes de que el `401` llegue. Una respuesta de error que no se arregla esperando (un `400`, por ejemplo) se reintenta cada 8 segundos mientras la pantalla esté abierta. «Mis reservas» también escucha ([decisión 015](015-mis-reservas-y-cancelacion.md)), pero nunca a la vez que la pantalla de la reserva: lo que tapa a una la saca de la pantalla y cierra su conexión, así que no hace falta compartirla.
