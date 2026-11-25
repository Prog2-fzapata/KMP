# 010. Paquete de la app, CI y código propio

**Estado:** aceptada

## Contexto

La app necesita un paquete raíz y un `applicationId` (con el que Android identifica la app instalada), y un aviso automático cuando un pull request rompe la compilación. Los repos de referencia que se usaron como guía de versiones y estructura tienen licencia AGPL-3.0.

## Decisión

- Paquete y `applicationId`: `ar.edu.um.prog2.fzapata.cliente`, como `ar.edu.um.prog2.fzapata.catalogo` y `ar.edu.um.prog2.fzapata.turnos`.
- Un workflow de GitHub Actions (`.github/workflows/build.yml`, JDK 21) compila la app y ejecuta `check` (pruebas y lint) en cada pull request y en cada push a `main`, `develop` y `testing`.
- Los archivos de Gradle están escritos a mano y el wrapper se generó con Gradle 8.14.3. No se copian archivos ni código de los repos de referencia; las versiones de la pila sí se toman de ellos ([decisión 001](001-un-modulo-y-pila-de-versiones.md)).

## Alternativas descartadas

- `ar.edu.um.programacion2`, el paquete de un repo de ejemplo: no sigue el estilo de los otros dos repos del proyecto.
- Dejar el CI para la documentación final: sin workflow, un pull request puede romper la compilación sin que nadie lo note.

## Consecuencias

Cambiar el paquete después obliga a tocar todos los archivos y el `applicationId`, y una app instalada con el anterior no se actualiza: se instala como otra. El CI no usa SonarCloud ([decisión 003](003-pruebas-acotadas-y-sin-sonarcloud.md)).
