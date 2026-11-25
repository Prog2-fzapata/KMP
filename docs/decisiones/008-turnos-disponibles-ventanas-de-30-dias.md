# 008. Turnos disponibles: ventanas de 30 días, tope de un año y `kotlinx-datetime`

**Estado:** aceptada

## Contexto

`GET /api/availability` devuelve los horarios libres de un profesional entre dos fechas, ambas incluidas, con un máximo de 31 días por pedido (`from` más 31 días ya responde `INVALID_DATE_RANGE`). Un profesional con dos días de atención por semana tiene unos 4 días con horarios en 14 días y 9 en 30. La respuesta es una foto: el veredicto real lo da la reserva. El servicio no tiene límite hacia adelante: genera los horarios con la regla semanal de la agenda y devolvió horarios hasta el año 2100. La app tiene que saber qué día es hoy, sumar días y sacar el día de la semana de cada fecha, y el código común de KMP no puede usar `java.time`.

## Decisión

- La pantalla pide ventanas de 30 días seguidas: la primera va de hoy a hoy más 29, la siguiente empieza el día 30, y así. Al acercarse al final de la lista se pide la ventana que sigue (scroll infinito, como en la lista de profesionales).
- Se ofrecen turnos hasta un año hacia adelante (`BOOKING_HORIZON_DAYS = 365`, de hoy a hoy más 364); la última ventana se recorta para no pasar de ahí. Una ventana sin horarios no corta: se sigue con la siguiente hasta encontrar horarios o llegar al último día. Al llegar al último día no hay más y se muestra «No hay más horarios.».
- El día de inicio de la próxima ventana solo avanza si la respuesta llegó, así que «Reintentar» sigue donde falló.
- Los horarios se muestran agrupados por día (encabezado «Mié 25 de noviembre») como etiquetas con la hora de inicio, que pasan de renglón cuando no entran. En esta pantalla no hacen nada al tocarlas: la reserva es KMP-06.
- La app ordena los horarios por fecha y hora: el contrato no promete el orden y el criterio es de la app. Como `date` (`yyyy-MM-dd`) y `startTime` (`HH:mm:ss`) son texto ISO, ordenan igual que como fecha y hora.
- Se suma `kotlinx-datetime` (0.7.1) para el día de hoy, la suma de días y el día de la semana. Las fechas viajan como `LocalDate` hacia el servicio y se leen como texto desde él.
- Los errores se muestran por su `code` con los mensajes que ya tiene `ErrorMessages` (`PROFESSIONAL_DISABLED`, `PROFESSIONAL_NOT_FOUND`, `INVALID_DATE_RANGE`, servicio caído). Si en todo el año no hay horarios se muestra «No hay horarios libres en el próximo año.».
- `AvailabilityScreen` recibe el `id` y el nombre del profesional. La pieza que dibuja un día es `DaySlots`; el pedido de más al final de la lista, el círculo de carga y el error con «Reintentar» pasan a `screens/components/` porque ahora los usan dos pantallas.

## Alternativas descartadas

- Sin tope: el servicio nunca se agota y la lista no terminaría; además cortar en la primera ventana vacía dejaba afuera los horarios de más adelante de un profesional ocupado este mes.
- Una ventana fija de 30 días sin scroll: deja afuera los turnos de más adelante y no hay forma de verlos.
- Dos chips «14 días» y «30 días»: un componente y un estado más, y el usuario tiene que elegir antes de ver nada.
- Calcular la fecha a mano o con código distinto por plataforma (`expect`/`actual`): más código y más riesgo que una librería oficial de Kotlin.
- Confiar en el orden del servicio: la prueba no puede exigirlo si el contrato no lo promete.
- Una fila de texto por horario: la lista es mucho más larga para el mismo contenido.

## Consecuencias

El reloj de Kotlin 2.2 (`kotlin.time.Clock`) todavía es experimental y se acepta con `@OptIn` en un solo lugar. Un profesional sin agenda hace 13 pedidos seguidos (doce ventanas de 30 días y una de 5) antes de mostrar que no hay horarios. Cambiar el horizonte es tocar una constante del `ViewModel`. «Hoy» es el de la hora del teléfono; el servicio descarta por su cuenta los horarios que ya pasaron.
