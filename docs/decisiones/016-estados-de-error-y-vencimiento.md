# 016. Estados de error y vencimiento: un mensaje por tipo de falla, el motivo de la reserva y el aviso de sesión vencida

**Estado:** aceptada

## Contexto

Con las pantallas de los issues anteriores, ninguna espera sin fin: todo pedido corta a los 15 segundos (10 para conectar, 45 para iniciar una reserva), y las esperas largas, el stream y los 42 segundos de la cátedra, tienen su propio camino ([decisión 005](005-cliente-http-errores-y-sesion-vencida.md), [012](012-ingreso-del-telefono-sin-cuenta-regresiva.md) y [013](013-stream-de-cambios-con-reconexion.md)). Quedaban cuatro huecos en lo que el usuario lee:

- cualquier falla que no fuera una respuesta del servicio (un timeout, un corte, una respuesta ilegible) decía «No se pudo conectar: revisá la conexión», aunque el servidor estuviera y solo tardara;
- el `failureReason` de una reserva fallida no se usaba;
- al vencer la sesión la app volvía al login sin decir nada, y «Cerrar sesión» usaba el mismo camino, así que no se distinguía un vencimiento de un cierre voluntario;
- el issue pide que, pasado `expiresAt`, la app reconsulte.

## Decisión

- `ErrorMessages.forError` distingue tres fallas que no son una respuesta del servicio:
  - sin conexión (un corte, o una conexión que no logra abrirse en 10 segundos): «No se pudo conectar con el servidor: hay que revisar la conexión.»;
  - el servidor se alcanzó pero no respondió a tiempo (el tope de 15 segundos del pedido, o un silencio del socket): «El servidor tardó demasiado en responder.»;
  - respondió algo que la app no sabe leer: «La respuesta del servidor no se pudo interpretar.»
- Las pantallas que ya leían la reserva tras un corte (el envío del teléfono, la cancelación) siguen igual: un timeout también puede significar que el pedido llegó, así que se lee antes de dejar repetir.
- `Reservation` suma `failureReason` (opcional, texto abierto). Cuando una reserva termina en `FAILED`, el aviso al volver a los horarios usa un mensaje por motivo, y los que no se conocen caen en «No se pudo completar la reserva.» (el contrato pide eso: tratarlo como texto y mostrar un mensaje genérico si no se reconoce):
  - `SLOT_ALREADY_HELD`, `SLOT_ALREADY_RESERVED`: «Ese horario ya no está disponible.»
  - `PROFESSIONAL_DISABLED`, `PROFESSIONAL_NOT_FOUND`: «El profesional no está disponible.»
  - `CATEDRA_NOT_REACHED`: «No se pudo comunicar con la cátedra: no se reservó nada.»
  - `HOLD_RESULT_UNKNOWN`: «No se pudo confirmar el horario. Si aparece ocupado, se libera solo en unos minutos.» (el bloqueo que la cátedra haya creado vence solo).
  Esto reemplaza lo dicho en las decisiones 005 y 012 sobre no mirar el motivo. El motivo se mira solo para elegir el texto, nunca para decidir qué hacer.
- `SessionManager` separa `logout()` (el usuario cierra la sesión) de `expire()` (el servicio rechazó el token), que además marca `expired`. El login lee esa marca al abrirse y, si venció, muestra «La sesión venció: iniciá sesión de nuevo.»; el aviso se va al intentar entrar de nuevo. La marca vive en memoria: al reiniciar la app el aviso no vuelve.
- «Si pasó `expiresAt` la app reconsulta» no se implementa como un reloj. El vencimiento se resuelve por cuatro caminos que ya existen: el servicio expira por su cuenta los procesos vencidos y el stream avisa del cambio; al enviar el teléfono, `RESERVATION_EXPIRED` vuelve a los horarios con el aviso; a los 42 segundos sin novedades de la cátedra se ofrece «Reintentar» y se vuelve a leer; y al reconectar el stream la reserva se lee de nuevo. Ninguna pantalla queda en un estado que no pueda terminar.

## Alternativas descartadas

- Dejar todos los fallos de red con el mismo texto: confunde cuando el servidor está pero tarda, y el usuario no sabe si conviene esperar o revisar la conexión.
- Mostrar el `failureReason` tal cual: son códigos en inglés pensados para quien programa.
- Un `switch` sobre el motivo para decidir el flujo (por ejemplo, reintentar con `CATEDRA_NOT_REACHED`): el motivo es un conjunto abierto y la cátedra puede sumar valores; el flujo sigue decidiéndose por el estado.
- Guardar la marca de sesión vencida en disco: un aviso de hace días al abrir la app confunde más de lo que ayuda.
- Una relectura única al llegar `expiresAt`: depende del reloj del teléfono; con el reloj adelantado se lee antes de que el servicio haya expirado el proceso y no cambia nada, con el atrasado llega tarde. El servicio y el stream ya cubren el caso.

## Consecuencias

Un timeout y una caída de red se explican distinto, pero el usuario sigue sin saber si su pedido llegó cuando era un envío o una cancelación: por eso esas pantallas leen la reserva antes de dejar repetir. Un motivo nuevo de `failureReason` muestra el mensaje genérico hasta agregarlo a `ErrorMessages`. Un `401` vacía la sesión desde cualquier pantalla y el usuario vuelve al login con el aviso, pero lo que estaba escribiendo se pierde. Faltó probar contra fallas reales de red (un timeout y la falta de conexión): se cubrió con pruebas sobre las excepciones de Ktor.
