# 011. Iniciar una reserva: confirmación, un camino por código y consulta antes de repetir

**Estado:** aceptada

## Contexto

`POST /api/reservations` bloquea el horario en la cátedra unos 10 minutos y responde con la reserva esperando el teléfono. No se consulta la disponibilidad antes: el pedido es la verificación y un `409` es un caso normal. Turnos puede tardar hasta unos 30 segundos si la cátedra no responde, y la cátedra no tiene idempotencia: repetir un pedido cuyo resultado se ignora puede crear una reserva doble. Si la cuenta no tiene nombre y apellido de al menos 2 caracteres, turnos responde `PATIENT_NAME_REQUIRED` antes de crear nada.

## Decisión

- Tocar un horario abre un diálogo «¿Reservar este turno?» con el día, la hora y el profesional. Recién al confirmar se envía el pedido.
- Mientras hay un pedido en curso se ignoran los toques y el botón del diálogo queda deshabilitado, así que un segundo toque no envía otra reserva.
- El pedido tiene un timeout propio de 45 segundos (el general es de 15): cortar antes dejaría una reserva que la app no conoce.
- Un camino por cada código:
  - `SLOT_UNAVAILABLE` e `INVALID_SLOT`: aviso arriba de la lista («Ese horario ya no está disponible. Se actualizaron los horarios.») y los horarios se piden de nuevo desde hoy.
  - `PATIENT_NAME_REQUIRED`: diálogo para cargar nombre y apellido; al guardar, se repite la misma reserva una sola vez. Es seguro porque este error sale antes de que el servicio cree nada.
  - `RESERVATION_EXPIRED`: el aviso con su mensaje; el usuario vuelve a tocar el horario.
  - `RESERVATION_CONFLICT`, un corte de red o un timeout, y un `503`: la reserva puede existir aunque la respuesta no llegó. Antes de dejar repetir se consulta `GET /api/reservations` (primera página) y, si hay una reserva del mismo profesional, fecha y hora que no esté cancelada, se abre esa. Si no hay, se avisa con el error original.
  - Profesional deshabilitado o inexistente y servicio caído: el mensaje de `ErrorMessages`, como aviso.
- El nombre y el apellido se validan en la app (entre 2 y 50 caracteres, sin contar espacios de los bordes): el catálogo acepta un solo carácter pero turnos lo rechaza. Se guardan con `UserApi.updateAccount` (`POST /api/account`, del slice de usuarios del catálogo, donde están también el registro y el inicio de sesión) enviando solo esos dos campos: desde la decisión 032 del catálogo actualiza únicamente lo que llega, así que no hace falta leer la cuenta ni reenviar el email.
- Qué hacer con cada respuesta lo decide `ReservationStarter`, una clase sin estado que devuelve un resultado (reserva creada, horario que ya no sirve, falta el nombre o falló); el `ViewModel` solo lo traduce a su estado. Así las reglas se prueban sin dispatchers y los horarios no se mezclan con la reserva.
- El aviso es un texto sobre la lista que se borra al tocar otro horario; no se usa un `Snackbar`.
- Al salir bien se abre `ReservationScreen`, provisoria hasta KMP-07, reemplazando a la pantalla de turnos: «Volver» lleva a la lista y no a horarios desactualizados. Del contrato se toman solo los campos que se usan.
- La hora del pedido se envía como la devuelve la disponibilidad (`HH:mm:ss`), que el servicio acepta.

## Alternativas descartadas

- Reservar al tocar el horario, sin confirmar: un toque accidental bloquea el horario unos minutos.
- Consultar `GET /api/account` antes de guardar el nombre: con la actualización parcial ya no hace falta.
- Mostrar solo el mensaje ante un conflicto o un corte: el usuario no tendría cómo saber si la reserva existe y repetiría el pedido.
- Un `Snackbar` para el aviso: obliga a manejar un evento de una sola vez.
- Abrir la reserva con `push`: al volver se vería la grilla anterior, con el horario ya tomado.

## Consecuencias

La búsqueda de una reserva existente mira solo la primera página de 20, con los turnos más lejanos primero: un usuario con más de 20 reservas de turnos posteriores al buscado podría no encontrarla. Una reserva iniciada y abandonada sin cargar el teléfono bloquea el horario hasta que vence sola. Con la cátedra sin responder, el error puede tardar hasta 45 segundos en mostrarse.
