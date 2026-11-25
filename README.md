# KMP: app de turnos

App Android hecha con Kotlin Multiplatform y Compose Multiplatform. Permite registrarse, buscar profesionales, ver su disponibilidad y reservar turnos. Consume dos servicios: el de catálogo (`ServicioCatalogo`, puerto `8081`) y el de turnos (`ServicioTurnos`, puerto `8082`).

Estado: proyecto base (KMP-01), cliente HTTP con sesión y manejo de errores (KMP-02), registro e inicio de sesión (KMP-03), la lista de profesionales con filtros (KMP-04) y los turnos disponibles de cada profesional (KMP-05). Las demás pantallas se agregan issue por issue.

## Documentos

| Documento | Qué explica |
| --- | --- |
| [Decisiones de diseño](docs/decisiones/README.md) | Cada decisión técnica, con sus alternativas descartadas y consecuencias |

## Requisitos

- JDK 21. Gradle 8.14.3 no arranca con el JDK 25.
- Android SDK. La primera compilación instala la plataforma 36 y las herramientas que falten.
- Un emulador de Android o un teléfono con depuración USB.
- Los dos servicios levantados, para las pantallas que los usan (ver el README de cada uno).

## Configuración

Las URLs de los servicios están en `gradle.properties`:

| Propiedad | Descripción | Por defecto |
| --- | --- | --- |
| `catalogUrl` | URL base del servicio de catálogo | `http://10.0.2.2:8081` |
| `turnosUrl` | URL base del servicio de turnos | `http://10.0.2.2:8082` |

`10.0.2.2` es la dirección con la que el emulador ve a la computadora donde corre. Con un teléfono real hay que usar la IP de esa computadora en la red local, sin tocar el código:

```bash
./gradlew :composeApp:installDebug -PcatalogUrl=http://192.168.1.20:8081 -PturnosUrl=http://192.168.1.20:8082
```

El texto plano (`http://`) solo está permitido en la compilación de debug.

## Ejecutar

```bash
export JAVA_HOME=/ruta/al/jdk-21
./gradlew :composeApp:assembleDebug        # compila el APK
./gradlew :composeApp:installDebug         # lo instala en el emulador o teléfono conectado
./gradlew check                            # corre las pruebas y el análisis de lint
```

## Estructura

```
composeApp/src/commonMain/    código compartido
    config/                   URLs de los servicios
    network/                  cliente HTTP, errores y sus mensajes
    session/                  el token de la sesión
    viewmodel/                el estado de cada pantalla y las reglas que usa
        validation/           validadores de formularios y errores de campo del servicio
    screens/                  pantallas
        components/           piezas de pantalla que se reutilizan (campo de texto, casilla, horarios de un día)
composeApp/src/androidMain/   MainActivity, ClienteApplication, almacenamiento cifrado del token y recursos de Android
composeApp/src/debug/         manifiesto extra solo de debug (texto plano)
composeApp/src/commonTest/    pruebas
```

Paquete y `applicationId`: `ar.edu.um.prog2.fzapata.cliente`.
