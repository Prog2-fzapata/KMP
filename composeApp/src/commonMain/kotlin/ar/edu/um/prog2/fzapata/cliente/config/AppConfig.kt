package ar.edu.um.prog2.fzapata.cliente.config

import androidx.compose.runtime.staticCompositionLocalOf

class AppConfig(catalogUrl: String, turnosUrl: String) {

    // Sin barra final, para poder escribir "$catalogUrl/api/..." sin duplicarla
    val catalogUrl: String = catalogUrl.trimEnd('/')
    val turnosUrl: String = turnosUrl.trimEnd('/')
}

// Las pantallas la leen de aquí: así no la llevan como campo (Voyager serializa las pantallas)
val LocalAppConfig = staticCompositionLocalOf<AppConfig> {
    error("AppConfig no está disponible: falta App.Content()")
}
