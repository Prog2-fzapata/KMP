# KMP: app de turnos

App Android hecha con Kotlin Multiplatform y Compose Multiplatform. Consume dos servicios: el de catálogo (`ServicioCatalogo`, puerto `8081`) y el de turnos (`ServicioTurnos`, puerto `8082`).

## Qué hace

- Crear una cuenta, iniciar sesión (con la opción de mantenerla abierta) y cerrarla; la sesión vencida avisa en el login.
- Buscar profesionales por nombre y categoría, con los filtros «Solo habilitados» y «Solo con horarios», y ver los horarios disponibles de cada uno.
- Reservar un horario: la cátedra pide el teléfono, la app lo envía y la reserva se confirma, vence o falla; la app sigue esos cambios en tiempo real.
- Ver las reservas propias (próximas y anteriores, con filtro por estado) y cancelar una confirmada, con un motivo opcional.
- Ver y editar los datos de la cuenta (nombre, apellido y email).
- Mostrar un mensaje claro en cada falla (sin conexión, servidor lento, servicio caído, reserva vencida) y dejar reintentar.

## Documentos

| Documento | Qué explica |
| --- | --- |
| [Arquitectura](docs/arquitectura.md) | El sistema completo (app, servicios, bases e integraciones con la cátedra), la app por dentro, el flujo de una reserva y el resumen de decisiones |
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

## Pruebas

```bash
./gradlew check
```

Corre las pruebas de `commonTest` (`kotlin-test` y el motor falso de Ktor) y el análisis de lint. Cubren la traducción de los errores `problem+json`, las validaciones de los formularios, la lectura de los eventos del stream y las reglas de cada pantalla (los `ViewModel`). Las pruebas de interfaz no se hacen ([decisión 003](docs/decisiones/003-pruebas-acotadas-y-sin-sonarcloud.md)). El mismo comando corre en cada push y pull request ([`.github/workflows/build.yml`](.github/workflows/build.yml)).

## Probar de punta a punta

1. **Levantar los servicios.** Hace falta la VPN de la cátedra: el catálogo lee su Redis y su Kafka, y turnos usa su Kafka. Cada servicio trae su `docker-compose.yml` y su `.env` (ver su README). Con los `docker-compose.yml` separados, en Linux el contenedor de turnos no alcanza al de catálogo; en el repo `Documentacion`, carpeta `infra`, hay un compose conjunto que lo resuelve. Con los dos arriba, `http://localhost:8081/actuator/health` y `http://localhost:8082/actuator/health` responden `UP`. Al primer arranque el catálogo se sincroniza con la cátedra y los profesionales aparecen cuando termina.
2. **Instalar la app** en el emulador o el teléfono, como se explica en «Ejecutar» (o con el botón de ejecutar de Android Studio).
3. **Recorrer el flujo:**
   - «Crear una cuenta» con usuario, email y contraseña, «Registrarme» y después «Ingresar».
   - En **Mi perfil**, cargar nombre y apellido: sin ellos el servicio no deja reservar (la app también los pide al reservar).
   - En **Profesionales**, buscar y filtrar; tocar uno para ver sus horarios y tocar un horario para reservar.
   - Pasados unos segundos la cátedra pide el teléfono: escribirlo y esperar el «Turno confirmado».
   - En **Mis reservas**, ver la reserva en «Próximas», abrirla y usar «Cancelar turno» (el motivo es opcional).
   - Para ver los errores, detener un servicio (por ejemplo `docker compose stop turnos`) y repetir una acción: la app muestra un mensaje según la falla y ofrece reintentar.

## Estructura

```
composeApp/src/commonMain/    código compartido
    config/                   URLs de los servicios
    network/                  cliente HTTP, errores y sus mensajes
    session/                  el token de la sesión
    viewmodel/                el estado de cada pantalla y las reglas que usa
        validation/           validadores de formularios y errores de campo del servicio
    screens/                  pantallas
        components/           piezas de pantalla que se reutilizan (campo de texto, casilla, horarios de un día, diálogos)
composeApp/src/androidMain/   MainActivity, ClienteApplication, almacenamiento cifrado del token y recursos de Android
composeApp/src/debug/         manifiesto extra solo de debug (texto plano)
composeApp/src/commonTest/    pruebas
docs/                         arquitectura y decisiones de diseño
```

Paquete y `applicationId`: `ar.edu.um.prog2.fzapata.cliente`.
