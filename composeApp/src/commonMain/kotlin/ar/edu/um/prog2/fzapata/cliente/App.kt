package ar.edu.um.prog2.fzapata.cliente

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.config.LocalAppConfig
import ar.edu.um.prog2.fzapata.cliente.screens.HomeScreen
import cafe.adriel.voyager.navigator.Navigator

class App(private val config: AppConfig) {

    @Composable
    fun Content() {
        CompositionLocalProvider(LocalAppConfig provides config) {
            MaterialTheme {
                Navigator(screen = HomeScreen())
            }
        }
    }
}
