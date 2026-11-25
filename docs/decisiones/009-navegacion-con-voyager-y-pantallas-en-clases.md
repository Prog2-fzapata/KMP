# 009. Navegación con Voyager y pantallas como clases con `Content()`

**Estado:** aceptada

## Contexto

La app tiene un flujo con pila de pantallas (lista de profesionales, turnos, reserva, teléfono) y, más adelante, una barra inferior con dos secciones: profesionales y «Mis reservas» (KMP-09). Compose permite escribir una pantalla como una función suelta o como una clase, y la navegación puede resolverse con `navigation-compose` (rutas de texto) o con una librería multiplataforma como Voyager.

## Decisión

- Navegación con Voyager 1.1.0-beta03: cada pantalla es una clase que implementa `Screen` y se dibuja en `Content()`. El `Navigator` guarda la pila (`push` para ir a una pantalla, `pop` para volver) y, cuando exista la barra inferior, se usarán `Tab` y `TabNavigator`.
- La raíz (`App`) arma el `Navigator` según haya o no sesión ([decisión 006](006-viewmodel-y-pantalla-segun-la-sesion.md)).
- Una pantalla por archivo, en `screens/`. Las piezas que varias pantallas reutilizan son funciones `@Composable` en `screens/components/`.
- Voyager serializa las pantallas: lo que una pantalla recibe al construirse son datos simples (un id, un texto). Lo compartido (cliente HTTP, sesión) se toma de `LocalAppContainer`.
- El estado y las llamadas a los servicios van en un `ViewModel` por pantalla (decisión 006). Todo en un solo módulo, `composeApp` ([decisión 001](001-un-modulo-y-pila-de-versiones.md)).

## Alternativas descartadas

- `navigation-compose` con rutas y un tipo `AppDestination`: el destino, la ruta y la pantalla se declaran por separado, y son tres cosas que mantener para cada pantalla nueva.
- Pantallas como funciones `@Composable` sueltas: no hay un objeto que lleve sus datos y su identidad; con Voyager cada pantalla tiene una clave propia, que es la que usa el `ViewModel` para no mezclar estados entre una pantalla y la siguiente.

## Consecuencias

Voyager 1.1.0-beta03 es una versión beta, y el nombre `Content()` lo impone la librería. Una pantalla no puede llevar como campo un objeto que no se serialice, por eso existe `LocalAppContainer`. Cambiar de librería de navegación tocaría todas las pantallas, pero no los `ViewModel` ni los servicios.
