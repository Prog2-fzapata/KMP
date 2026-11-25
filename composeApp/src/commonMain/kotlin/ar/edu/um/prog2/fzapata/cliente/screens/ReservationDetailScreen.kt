package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.screens.components.CancelReservationDialog
import ar.edu.um.prog2.fzapata.cliente.screens.components.RetryMessage
import ar.edu.um.prog2.fzapata.cliente.screens.components.Waiting
import ar.edu.um.prog2.fzapata.cliente.screens.components.dayTitle
import ar.edu.um.prog2.fzapata.cliente.screens.components.statusLabel
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ReservationDetailViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

// Una reserva confirmada o cancelada de la lista; si es una confirmada de hoy en adelante se puede cancelar
class ReservationDetailScreen(private val reservationId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class) // TopAppBar aún es experimental
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel(key = key) { ReservationDetailViewModel(container.turnosApi, reservationId, container.errorMessages) }
        val state by viewModel.state.collectAsState()
        val reservation = state.reservation

        // Una reserva que ya no existe: se vuelve a la lista, que se actualiza sola y la saca
        LaunchedEffect(state.gone) {
            if (state.gone) navigator.pop()
        }

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
                val error = state.error
                when {
                    reservation != null -> {
                        val name = "${reservation.professionalFirstName} ${reservation.professionalLastName}"
                        Text(name, style = MaterialTheme.typography.titleMedium)
                        Text("${dayTitle(reservation.appointmentDate)}, ${reservation.startTime.take(5)}")
                        Text(statusLabel(reservation), style = MaterialTheme.typography.titleMedium)
                        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (state.canCancel) {
                            OutlinedButton(onClick = viewModel::onCancelClick) { Text("Cancelar turno") }
                        }
                        if (state.asking) {
                            CancelReservationDialog(
                                summary = "$name, ${dayTitle(reservation.appointmentDate)} a las ${reservation.startTime.take(5)}.",
                                reason = state.reason,
                                error = state.cancelError,
                                cancelling = state.cancelling,
                                onReasonChange = viewModel::onReasonChange,
                                onConfirm = viewModel::confirmCancel,
                                onDismiss = viewModel::onDismiss
                            )
                        }
                    }
                    error != null -> RetryMessage(error, viewModel::retry)
                    else -> Waiting("Cargando la reserva…")
                }
            }
        }
    }
}
