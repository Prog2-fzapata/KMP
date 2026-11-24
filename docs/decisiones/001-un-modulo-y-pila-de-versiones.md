# 001. Un solo módulo y la pila de versiones con AGP 8.13

**Estado:** aceptada

## Contexto

La app solo necesita Android. Con el Android Gradle Plugin (AGP) 9, los plugins `kotlinMultiplatform` y `com.android.application` no pueden aplicarse al mismo módulo: Gradle falla con «not compatible with the org.jetbrains.kotlin.multiplatform plugin since AGP 9.0» y obliga a separar el punto de entrada de Android (`androidApp`) del código compartido (`shared`).

## Decisión

Un solo módulo, `composeApp`, con solo el target Android y esta pila: AGP 8.13.0, Gradle 8.14.3, Kotlin 2.2.20, Compose Multiplatform 1.9.0, Voyager 1.1.0-beta03 (navegación) y JDK 21. Compila el APK, pasa las pruebas y se ejecutó en un emulador con Android 37.

## Alternativas descartadas

- Dos módulos (`androidApp` y `shared`) con AGP 9.4.1, Kotlin 2.4.20 y Compose Multiplatform 1.12.1: compila con Voyager y Ktor, pero es más configuración para una app que solo tiene un target.
- Seguir con AGP 9 en un módulo con `android.builtInKotlin=false` y `android.newDsl=false`: el propio error de Gradle las presenta como una salida temporal.

## Consecuencias

AGP 8 termina en la 8.13.2: pasar a AGP 9 exige separar los módulos. Gradle 8.14.3 no arranca con el JDK 25, así que el proyecto se compila con JDK 21. Los targets iOS, web y escritorio no se incluyen; agregarlos sería un cambio aparte.
