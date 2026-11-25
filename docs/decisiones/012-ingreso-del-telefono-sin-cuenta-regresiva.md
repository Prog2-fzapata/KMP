# 012. Ingreso del teléfono: un diálogo cuando la cátedra lo pide, sin cuenta regresiva

**Estado:** aceptada

## Contexto

Con la reserva iniciada ([decisión 011](011-iniciar-una-reserva-sin-duplicados.md)) el horario queda bloqueado unos 10 minutos en `WAITING_PHONE`, con `canSubmitPhone` en `false` hasta que la cátedra pide el teléfono. Medido con los servicios reales, lo pide entre 1,7 y 3,4 segundos después. Enviarlo antes responde `409 INVALID_RESERVATION_STATE`. Con el teléfono enviado (`202`, reserva en `PHONE_QUEUED`) la cátedra lo confirma (`CONFIRMED`, unos 5 segundos en total) o lo rechaza: la reserva vuelve a `WAITING_PHONE` con `canSubmitPhone` en `true`, sin código ni motivo. El servicio acepta de 7 a 15 dígitos con un `+` opcional al principio, después de quitar espacios, guiones, paréntesis y puntos.

## Decisión

- `ReservationScreen` recibe solo el `id` de la reserva y la lee por REST; sigue sus cambios con el stream ([decisión 013](013-stream-de-cambios-con-reconexion.md)). Dibuja según un paso (`ReservationStep`) que sale del estado y de `canSubmitPhone`, y no según el texto del estado:
  - esperar el pedido de la cátedra: círculo de carga;
  - pedir el teléfono: diálogo;
  - esperar el resultado del envío: círculo de carga;
  - confirmada: «Turno confirmado»;
  - terminada (vencida, fallida o cancelada): se vuelve a los horarios.
- El diálogo aparece solo con `canSubmitPhone` en `true`. No se envía antes ni con un envío en curso, aunque el botón se pudiera tocar.
- `PhoneValidator` aplica la regla del servicio y el número se envía ya limpio, sin los separadores: lo que se validó es lo que viaja. No se completa el código de país: `+54` y `549` son números distintos y adivinar uno sería inventar el dato.
- Un rechazo se reconoce porque la reserva, mientras esperaba el resultado, vuelve a pedir el teléfono. El diálogo reaparece con el número escrito y «La cátedra no aceptó el número. Probá con otro.»; como no hay un motivo, el mensaje es uno solo. Se puede reintentar hasta que la reserva venza.
- «Cancelar» cierra el diálogo y vuelve a los horarios del profesional, ya actualizados. No existe cancelar una reserva sin confirmar, así que el bloqueo vence solo, y no hay un botón para retomar el ingreso.
- No hay cuenta regresiva: el vencimiento lo decide el servicio. `RESERVATION_EXPIRED` al enviar y los estados `EXPIRED`, `FAILED` y `CANCELLED` vuelven a los horarios con un aviso («Elegí el horario de nuevo.»), que `AvailabilityScreen` recibe como aviso inicial. El `failureReason` es texto abierto: no se muestra, el mensaje de `FAILED` es genérico.
- Errores del envío, uno por código:
  - `RESERVATION_EXPIRED`: vuelve a los horarios con el aviso.
  - `INVALID_RESERVATION_STATE`, `RESERVATION_CONFLICT`, un corte de red o un timeout, y un `503`: el número pudo llegar, así que se lee la reserva antes de dejar repetir; mientras tanto el mensaje queda debajo del campo.
  - `VALIDATION_ERROR` y el resto: el mensaje de `ErrorMessages` debajo del campo.
- Si la cátedra no hace lo esperado en 42 segundos (en cualquiera de las dos esperas) se muestra «La cátedra tarda más de lo normal» con «Reintentar», que vuelve a leer la reserva y empieza de nuevo el plazo. Nunca reenvía el teléfono: el servicio respondería `INVALID_RESERVATION_STATE`.
- La última lectura gana: un aviso nuevo cancela la lectura en curso, y la respuesta del envío cancela una lectura anterior que todavía no volvió.
- Al abrir la app con sesión, `ResumeViewModel` consulta la primera página de `GET /api/reservations` y, si hay una reserva que no está confirmada ni terminada, abre su pantalla encima de la lista, una sola vez. Así quien cerró la app, o a quien le venció la sesión a mitad de la reserva, la encuentra donde la dejó. La app no cancela nada: el servicio sigue procesando y, si faltaba el teléfono, el bloqueo vence solo. Si la consulta falla no se muestra nada: el usuario queda en la lista.

## Alternativas descartadas

- Una cuenta regresiva hasta `expiresAt`: el usuario no la necesita, el servicio ya decide el vencimiento, y habría que fiarse del reloj del teléfono.
- Comparar el instante del envío con `expiresAt`: el reloj del teléfono puede estar corrido varios minutos. Es mejor el mismo camino por código que en la decisión 011.
- Reintentar el bloqueo en silencio cuando ya venció: la cátedra no tiene idempotencia y podría duplicar la reserva, el horario pudo ocuparlo otra persona, cambiaría el `id` de la reserva que se está mirando y el usuario no confirmó un horario nuevo.
- Un botón para retomar el ingreso después de cancelar: el usuario volvió a los horarios y puede tocar el horario otra vez.
- Consultar la reserva cada pocos segundos: lo reemplaza el stream, que el issue pide de todos modos.
- Que «Reintentar» reenvíe el teléfono: pondría un número dos veces en la cola de la cátedra.
- Cancelar la reserva sin confirmar al vencer la sesión: el servicio no tiene cómo y no hace falta, porque el bloqueo vence solo.
- Un aviso arriba de la lista («Tenés una reserva en curso») en vez de abrirla: suma una pieza de pantalla y un toque para algo que el usuario ya estaba haciendo.

## Consecuencias

Un usuario que cancela el diálogo deja el horario bloqueado hasta 10 minutos, y no lo ve libre en la lista hasta que venza. Un rechazo no explica el motivo porque el servicio no lo informa. Un `RESERVATION_CONFLICT` o un corte durante el envío se resuelven leyendo la reserva, que puede mostrar el diálogo otra vez si el número no llegó. La reanudación mira solo la primera página (20) de reservas, y la lista no trae las vencidas: una reserva cuyo bloqueo ya pasó pero que el servicio todavía no marcó como vencida abre la pantalla, que enseguida vuelve a los horarios con el aviso.
