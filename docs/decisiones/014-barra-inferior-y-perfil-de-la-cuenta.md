# 014. Barra inferior con pestañas y perfil de la cuenta

**Estado:** aceptada

## Contexto

Hasta ahora la app abría la lista de profesionales y «Cerrar sesión» estaba en su barra superior. Se suma una barra inferior para llegar a las secciones de la app y una sección donde el usuario ve y modifica los datos de su cuenta, por ejemplo para cargar el nombre y el apellido cuando faltan. El catálogo ya ofrece `GET /api/account` y `POST /api/account`, que cambia solo los campos que llegan y responde `EMAIL_ALREADY_USED` o `VALIDATION_ERROR`. El token no lleva nombre ni apellido: se leen de la cuenta. El [ADR 009](009-navegacion-con-voyager-y-pantallas-en-clases.md) preveía `Tab` y `TabNavigator` para cuando existiera la barra.

## Decisión

- `MainScreen`, la pantalla de quien tiene sesión, contiene un `TabNavigator` de Voyager con **Profesionales** y **Mi perfil** y una `NavigationBar` de Material 3. Empieza en la primera de la lista. «Mis reservas» se suma como tercera pestaña con su issue.
- Los horarios y la reserva se abren con el `Navigator` de afuera y tapan la barra: una reserva en curso no es lugar para saltar a otra sección. Dentro de una pestaña `LocalNavigator` es el navegador de las pestañas, y apilar una pantalla ahí cierra la app (no es una pestaña), así que las pantallas de las pestañas apilan con su `parent`.
- Mi perfil muestra el usuario sin poder editarlo y permite modificar nombre, apellido y email. Sin imagen, porque la app no la usa, y sin idioma, porque el servicio lo guarda pero no cambia nada con él.
- Guardar envía los tres campos sin espacios en los bordes. Nombre y apellido se validan como en el diálogo de reservar (2 a 50 caracteres, `PatientNameValidator`) y el email con `EmailValidator`, que comparte con el registro; con errores no se envía nada. Los errores del servicio salen debajo de su campo (`EMAIL_ALREADY_USED`, `VALIDATION_ERROR`) y los demás como un mensaje general.
- Mientras a la cuenta guardada le falte el nombre o el apellido, la pantalla pide completarlos para poder reservar. Se mira lo guardado y no lo escrito, así que el aviso dura hasta que el nombre queda en la cuenta.
- El diálogo del nombre al reservar ([decisión 011](011-iniciar-una-reserva-sin-duplicados.md)) se mantiene: no saca al usuario de lo que estaba haciendo. Comparte validador y `UserApi.updateAccount` con el perfil.
- «Cerrar sesión» está en Mi perfil, fuera del bloque que carga la cuenta, para poder cerrar la sesión aunque la cuenta no cargue.
- La reanudación de la reserva en curso ([decisión 012](012-ingreso-del-telefono-sin-cuenta-regresiva.md)) vive en `MainScreen`: se dispara una vez al abrir la app con sesión y no cada vez que se vuelve a una pestaña.
- Voyager da a cada pantalla su propio almacén de `ViewModel` y lo descarta al salir de la pila. Verificado en el emulador: lo escrito en el perfil se conserva al cambiar de pestaña y, tras cerrar sesión y entrar con otro usuario, el perfil muestra la cuenta nueva.

## Alternativas descartadas

- Una barra propia con un índice guardado, sin `TabNavigator`: habría que escribir y mantener el estado de la pestaña activa que Voyager ya resuelve, y el ADR 009 había elegido `Tab`.
- Dejar la barra visible en los horarios y en la reserva: invita a salir de una reserva en curso, y la reanudación ya cubre volver a ella.
- Editar solo nombre y apellido: el email es el dato que una persona sí cambia.
- Reemplazar el diálogo de reservar por una invitación a ir al perfil: cambia de pantalla en medio de la reserva para algo que se resuelve en dos campos.
- Mostrar y editar el idioma: no cambia nada en la app ni en los servicios.

## Consecuencias

Quien solo quiere cambiar el email tiene que tener cargados nombre y apellido, porque se aplica la regla que exige turnos para anotar el turno. El email nuevo se guarda al instante, sin confirmación. Al cambiar de pestaña se descarta la pantalla y se arma de nuevo: lo escrito se conserva en su `ViewModel`, pero no se probó que se conserve la posición del scroll de la lista.
