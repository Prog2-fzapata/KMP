# 006. ViewModel por pantalla y pantalla de inicio según la sesión

**Estado:** aceptada

## Contexto

Las pantallas solo dibujan: el estado y las llamadas a los servicios van en un `ViewModel`. Voyager serializa las pantallas, así que no pueden llevar como campo el cliente HTTP ni la sesión. Además, la app tiene que volver al login cuando se cierra la sesión o un `401` la vacía, desde cualquier pantalla.

## Decisión

- Cada pantalla con lógica tiene su `ViewModel` de `androidx.lifecycle` (versión multiplataforma de JetBrains), que expone un `StateFlow` con el estado completo (campos, errores, cargando). La pantalla lo observa y le avisa los eventos; el `ViewModel` no conoce Compose ni Voyager.
- Las pantallas toman las piezas compartidas de `LocalAppContainer`, un `CompositionLocal` como `LocalAppConfig`.
- Cada pantalla pide su `ViewModel` con la clave de la propia pantalla (`viewModel(key = key)`): un login nuevo, tras cerrar sesión, arranca con el estado vacío.
- Al iniciar sesión, el `ViewModel` solo guarda el token. La raíz observa el token de `SessionManager` y arma el `Navigator` de nuevo con la pantalla que corresponde: inicio con sesión, login sin ella. Lo mismo ocurre al cerrar sesión o ante un `401`, y no se puede volver con «atrás».
- Al registrarse, la app inicia sesión sola con las mismas credenciales. Si el registro sale bien y el inicio falla, el mensaje dice que la cuenta ya existe: repetir el registro daría `LOGIN_ALREADY_USED`.
- Tras un inicio de sesión correcto se borra la contraseña del estado del `ViewModel`.
- El registro no envía `imageUrl`: la app no tiene dónde elegir una imagen y el servicio solo recibe una dirección de texto (no hay subida de archivos), que el usuario no tendría cómo obtener. Es un campo opcional del contrato, así que se puede omitir.
- La validación local del registro vive en `RegisterValidator`, con los límites del contrato. El patrón del login lo valida solo el servicio y vuelve como error del campo.

## Alternativas descartadas

- `ScreenModel` de Voyager: se libera con la pantalla, pero ata los `ViewModel` a esa librería y el MVVM habitual de MVVM usa el de `androidx`.
- Una pantalla raíz con `replaceAll` en cada pantalla de login y cierre: cada una tendría que acordarse de navegar; observar la sesión en un solo lugar cubre también el `401`.
- Dejar la validación al servicio: el usuario esperaría la red para saber que el login es corto.

## Consecuencias

El `ViewModel` de una pantalla ya cerrada queda en memoria hasta que se cierra la actividad: por eso no conserva la contraseña. Los códigos de error de campo que no sean de registro llegan como `fieldErrors` del servicio y se muestran con su mensaje.
