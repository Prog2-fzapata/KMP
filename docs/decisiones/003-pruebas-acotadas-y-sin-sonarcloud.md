# 003. Pruebas acotadas y sin SonarCloud

**Estado:** aceptada

## Contexto

Las pruebas de la app son opcionales en el alcance del proyecto, y una interfaz hecha con Compose casi no se prueba de forma automática. SonarCloud, con su Quality Gate de cobertura sobre el código nuevo, exigiría pruebas que no aportan o excluir las pantallas del cálculo.

## Decisión

El workflow de CI compila la app y ejecuta `check` (pruebas y lint). No se usa SonarCloud. Se escriben pocas pruebas, solo para lógica pura que codifica el contrato con los servicios o reglas pequeñas cuya rotura pasaría inadvertida: la traducción de los errores `problem+json`, las validaciones de los formularios, la lectura de los eventos SSE y el mapeo de estados a mensajes. Las pantallas no se prueban.

## Alternativas descartadas

- SonarCloud con las pantallas excluidas de la cobertura: agrega un proyecto, un secret y la medición con Kover para medir muy poco.
- Pruebas de interfaz con Compose UI test: requieren un emulador y cubren poco para su costo.

## Consecuencias

La app no publica métricas de cobertura; la cobertura se mide solo en los servicios. Las pruebas que existan corren en cada pull request con `check`.
