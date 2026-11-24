package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.config.LocalAppConfig
import cafe.adriel.voyager.core.screen.Screen

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val config = LocalAppConfig.current
        val container = LocalAppContainer.current

        Column(
            modifier = Modifier.fillMaxSize().safeContentPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Turnos", style = MaterialTheme.typography.headlineMedium)
            Text(text = "Catálogo: ${config.catalogUrl}")
            Text(text = "Turnos: ${config.turnosUrl}")
            // Vaciar la sesión hace que la app vuelva al login
            Button(onClick = container.session::expire) { Text("Cerrar sesión") }
        }
    }
}
