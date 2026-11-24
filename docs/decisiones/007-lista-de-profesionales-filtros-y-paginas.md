# 007. Lista de profesionales: filtros, páginas y pedidos cancelables

**Estado:** aceptada

## Contexto

`GET /api/professionals` admite filtros por categoría, nombre, habilitado y disponibilidad, y pagina con `page` y `size` (tope 100), con el total en el encabezado `X-Total-Count`. La app tiene que mostrar la lista, filtrarla y cargarla por partes, y no dejar que una respuesta lenta y vieja pise a una nueva.

## Decisión

- La lista es la pantalla de inicio con sesión y reemplaza al inicio de prueba de KMP-01. «Cerrar sesión» pasa a la barra superior. No hay barra de pestañas hasta que exista una segunda sección (KMP-09).
- Filtros: la categoría con chips (con «Todas»), el nombre con un campo que busca al tocar «Buscar» del teclado, y dos interruptores: «Solo habilitados» (activo al empezar) y «Solo con horarios» (apagado). Solo se envían los filtros activos: lo que no se envía no filtra.
- Páginas de 20 elementos con scroll infinito: al acercarse al final se pide la siguiente. `X-Total-Count` dice cuándo parar; el encabezado `Link` no se usa. Si el total no llega, se toma lo recibido como todo lo que hay.
- Cambiar un filtro cancela el pedido en curso y empieza otra vez desde la primera página.
- Con un error pendiente la lista no vuelve a pedir sola al llegar al final: espera «Reintentar», que repite el pedido que falló.
- El resumen de horarios es una línea de texto por profesional («Mar 13:00–16:00 · Jue 11:00–14:00»). Los días y las horas se dejan como texto del contrato y un día desconocido se muestra tal cual: no se usa `kotlinx-datetime`.
- Los modelos tienen solo los campos que la app usa; el resto de la respuesta se ignora.
- Tocar un profesional abre `AvailabilityScreen`, provisoria hasta que KMP-05 muestre los turnos.

## Alternativas descartadas

- Categorías en un menú desplegable: el de Material 3 es experimental y pide más código para ocho opciones.
- Buscar mientras se escribe: más pedidos y más código por una mejora pequeña con tan pocos profesionales.
- Un botón «Cargar más»: más simple, pero obliga a un toque por página.
- Leer el encabezado `Link`: repite lo que ya dicen la página actual y el total.
- Una barra de pestañas con una sola pestaña.

## Consecuencias

Si el catálogo agrega un filtro o un campo, la app lo ignora hasta que se use. Cambiar el tamaño de página o los límites de los chips es tocar una constante. Un profesional deshabilitado se ve con una etiqueta cuando se desactiva «Solo habilitados».
