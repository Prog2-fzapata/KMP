# 015. Mis reservas: dos secciones por día, lista al día con el stream y cancelación con un camino por código

**Estado:** aceptada

## Contexto

La app necesita listar las reservas del usuario, ver una y cancelarla si está confirmada. `GET /api/reservations` devuelve páginas de 20, siempre de la reserva con el turno más lejano a la más antigua (el `sort` se ignora), sin filtro por fecha, y trae `X-Total-Count`. Sin `status` deja afuera las vencidas y las fallidas; con `status` filtra por un solo estado. `POST /api/reservations/{id}/cancel` acepta un motivo opcional de hasta 500 caracteres y responde `409 RESERVATION_NOT_CANCELLABLE` si la reserva no está confirmada ni cancelada, `409 RESERVATION_CONFLICT`, `404 RESERVATION_NOT_FOUND` y `503 CATEDRA_UNAVAILABLE` (la reserva no cambia); cancelar una ya cancelada devuelve su estado sin error.

## Decisión

- «Mis reservas» es la tercera pestaña de la barra ([decisión 014](014-barra-inferior-y-perfil-de-la-cuenta.md)). Cada fila muestra el profesional, el día y la hora, y el estado con un nombre que dice qué falta: **Teléfono pendiente**, **Esperando confirmación**, **Confirmada** o **Cancelada** (sale del `ReservationStep` de la [decisión 012](012-ingreso-del-telefono-sin-cuenta-regresiva.md)).
- Filtro por un chip por estado: **Todas**, **Confirmadas** y **Canceladas**. El filtro del servicio admite un estado, así que no hay un chip para las que están en curso, y las vencidas y fallidas no se ofrecen porque nunca llegaron a ser una reserva.
- La lista tiene dos secciones por día: **Próximas** (de hoy en adelante, de la más cercana a la más lejana) y **Anteriores** (de la más reciente a la más vieja). Como el servicio pone primero todas las futuras, se leen páginas hasta la primera anterior, o hasta no haber más, y recién entonces se muestra la lista: así el orden de las próximas no cambia al llegar otra página. Las anteriores siguen con carga de más al llegar al final. La fecha del teléfono solo decide en qué sección va cada reserva y si se ofrece cancelar, y se mira el día, no la hora.
- Una reserva confirmada o cancelada abre su detalle; una que falta confirmar abre la pantalla de la reserva, que completa el teléfono o espera la confirmación. Si se llegó desde la lista, cancelar el diálogo del teléfono vuelve a la lista y no a los horarios del profesional.
- La lista escucha el stream mientras se ve. Al conectar y ante cada aviso vuelve a leer las páginas que ya cargó y reemplaza lo que había, así no salta; si esa lectura falla, la lista queda como estaba. Lo que tapa a la lista (el detalle, la reserva) o un cambio de pestaña la saca de la pantalla y cierra la conexión, así que nunca hay dos a la vez y no hace falta compartir una.
- En el detalle, «Cancelar turno» se ofrece solo en una reserva confirmada de hoy en adelante. Abre un diálogo de confirmación con un motivo opcional en un campo de 3 a 5 líneas, con un contador y un tope de 500 caracteres en el campo, así que el servicio no llega a rechazarlo por largo. Sin motivo no se envía cuerpo.
- Errores de la cancelación, uno por código:
  - `RESERVATION_NOT_CANCELLABLE`: se lee la reserva, el diálogo se cierra y el detalle muestra su estado real con el mensaje.
  - `RESERVATION_CONFLICT` o un corte de red: la cancelación pudo llegar, así que se lee la reserva antes de dejar repetir; si ya figura cancelada no hay error, y si no, el diálogo queda abierto con el mensaje.
  - `RESERVATION_NOT_FOUND`, también al abrir el detalle: se vuelve a la lista, que se actualiza y saca la reserva.
  - `503 CATEDRA_UNAVAILABLE` y el resto: el mensaje dentro del diálogo y se puede repetir; en el `503` no se lee la reserva porque el contrato dice que no cambia.

## Alternativas descartadas

- Mostrar el orden del servicio sin secciones: el turno de mañana quedaría debajo de los del año que viene.
- Cargar todas las reservas y ordenarlas en la app: deja de ser paginado, y la mayoría de las veces alcanza con una o dos páginas.
- Usar la pantalla de la reserva como detalle de todas: al ver una cancelada se iría sola a los horarios del profesional.
- Un chip «En curso»: el filtro del servicio no admite varios estados.
- Refrescar la lista solo al volver: no vería una reserva en curso que se confirma mientras tanto.
- Ofrecer cancelar según la hora del teléfono: puede estar corrida varios minutos; se compara solo el día, y la decisión final es del servicio.

## Consecuencias

La primera carga puede leer más de una página (dos si hay más de 20 reservas por venir). Una reserva cancelada de un día futuro aparece en «Próximas» con su etiqueta: para verlas juntas está el chip «Canceladas». Un teléfono con la fecha corrida pone una reserva en la sección vecina y, un día de diferencia, esconde o muestra el botón de cancelar. Una reserva que desaparece vuelve a la lista sin aviso. La cancelación contra la cátedra real no se pudo probar porque la cátedra no respondía: se cubrió con pruebas sobre las respuestas del contrato.
