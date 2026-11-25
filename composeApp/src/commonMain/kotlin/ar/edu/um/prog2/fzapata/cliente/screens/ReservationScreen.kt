package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.um.prog2.fzapata.cliente.screens.components.dayTitle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

// Destino provisorio de una reserva recién iniciada: KMP-07 la completa con el ingreso del teléfono
class ReservationScreen(
    private val professionalName: String,
    private val date: String,
    private val startTime: String
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class) // TopAppBar aún es experimental
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Reserva") },
                    navigationIcon = { TextButton(onClick = navigator::pop) { Text("Volver") } }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Reserva iniciada", style = MaterialTheme.typography.titleMedium)
                Text(professionalName)
                Text("${dayTitle(date)}, ${startTime.take(5)}")
                Text("El turno está bloqueado. Para confirmarlo hace falta cargar tu teléfono: es el siguiente paso de la app.")
            }
        }
    }
}
