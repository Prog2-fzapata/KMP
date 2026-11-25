package ar.edu.um.prog2.fzapata.cliente

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import ar.edu.um.prog2.fzapata.cliente.screens.LoginScreen
import ar.edu.um.prog2.fzapata.cliente.screens.MainScreen
import cafe.adriel.voyager.navigator.Navigator

class App(private val container: AppContainer) {

    @Composable
    fun Content() {
        // El compose vigila el token, si cambia .Content() se vuelve a ejecutar
        val hasSession = container.session.tokenFlow.collectAsState().value != null

        CompositionLocalProvider(LocalAppContainer provides container) {
            MaterialTheme {
                // Mientras hasSession valga lo mismo, reutiliza lo de adentro, si no, descarta y arma de cero
                key(hasSession) {
                    // Con key, descarta la pila y no puede volver para atrás
                    Navigator(screen = if (hasSession) MainScreen() else LoginScreen())
                }
            }
        }
    }
}
