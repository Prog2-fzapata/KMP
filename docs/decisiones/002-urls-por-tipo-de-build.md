# 002. Configurar las URLs de los servicios por tipo de build

**Estado:** aceptada

## Contexto

La app llama a dos servicios (catálogo y turnos). El emulador ve la computadora de desarrollo como `10.0.2.2`; un teléfono real necesita la IP de esa computadora en la red local. Las URLs no pueden quedar escritas en el código ni cambiar con cada máquina. Android 9 y posteriores bloquean el texto plano (`http://`) por defecto, y los servicios locales no usan HTTPS.

## Decisión

`catalogUrl` y `turnosUrl` viven en `gradle.properties`, con valores por defecto para el emulador (`http://10.0.2.2:8081` y `:8082`). Gradle las convierte en campos de `BuildConfig`, y `MainActivity` las pasa a `AppConfig`; el código compartido solo conoce `AppConfig`. Para un teléfono real se pisan con `-PcatalogUrl=...` y `-PturnosUrl=...`. El texto plano se permite solo en un manifiesto de debug (`src/debug`), así que la compilación de release no lo admite.

## Alternativas descartadas

- Constantes en el código: obligan a editar y subir cambios por cada máquina o red.
- Un archivo `.env`: Gradle no lo lee por sí solo y los valores no son secretos.
- Una pantalla de ajustes que cambie la URL en ejecución: agrega interfaz y almacenamiento que el proyecto no necesita.

## Consecuencias

Cambiar de servidor exige volver a compilar. La compilación de release usaría las mismas propiedades pero sin texto plano, por lo que necesita URLs `https://`; no hay entorno real donde probarlo y queda como limitación.
