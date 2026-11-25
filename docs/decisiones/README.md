# Decisiones de diseño: app KMP

Registro de las decisiones técnicas de la app, una por archivo (formato ADR): contexto, decisión, alternativas descartadas y consecuencias.

Estados posibles: `aceptada`, `reemplazada por NNN` y `obsoleta`.

| N.º | Decisión | Estado |
|---|---|---|
| 001 | [Un solo módulo y la pila de versiones con AGP 8.13](001-un-modulo-y-pila-de-versiones.md) | aceptada |
| 002 | [Configurar las URLs de los servicios por tipo de build](002-urls-por-tipo-de-build.md) | aceptada |
| 003 | [Pruebas acotadas y sin SonarCloud](003-pruebas-acotadas-y-sin-sonarcloud.md) | aceptada |
| 004 | [Guardar el JWT cifrado con el Android Keystore](004-jwt-cifrado-con-el-keystore.md) | aceptada |
| 005 | [Cliente HTTP: motor, errores y sesión vencida](005-cliente-http-errores-y-sesion-vencida.md) | aceptada |
| 006 | [ViewModel por pantalla y pantalla de inicio según la sesión](006-viewmodel-y-pantalla-segun-la-sesion.md) | aceptada |
| 007 | [Lista de profesionales: filtros, páginas y pedidos cancelables](007-lista-de-profesionales-filtros-y-paginas.md) | aceptada |
| 008 | [Turnos disponibles: ventanas de 30 días, tope de un año y `kotlinx-datetime`](008-turnos-disponibles-ventanas-de-30-dias.md) | aceptada |
| 009 | [Navegación con Voyager y pantallas como clases con `Content()`](009-navegacion-con-voyager-y-pantallas-en-clases.md) | aceptada |
| 010 | [Paquete de la app, CI y código propio](010-paquete-y-ci-de-la-app.md) | aceptada |
| 011 | [Iniciar una reserva: confirmación, un camino por código y consulta antes de repetir](011-iniciar-una-reserva-sin-duplicados.md) | aceptada |
| 012 | [Ingreso del teléfono: un diálogo cuando la cátedra lo pide, sin cuenta regresiva](012-ingreso-del-telefono-sin-cuenta-regresiva.md) | aceptada |
| 013 | [Stream de cambios: una conexión mientras la pantalla mira, reconexión creciente y comprobación de sesión](013-stream-de-cambios-con-reconexion.md) | aceptada |
