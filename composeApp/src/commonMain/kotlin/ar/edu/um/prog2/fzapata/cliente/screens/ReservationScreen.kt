package ar.edu.um.prog2.fzapata.cliente.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.screens.components.Centered
import ar.edu.um.prog2.fzapata.cliente.screens.components.PhoneDialog
import ar.edu.um.prog2.fzapata.cliente.screens.components.RetryMessage
import ar.edu.um.prog2.fzapata.cliente.screens.components.dayTitle
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ReservationStep
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ReservationViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

// Una reserva en curso: espera que la cátedra pida el teléfono, lo pide al usuario y espera la confirmación
class ReservationScreen(private val reservationId: Long) : Screen {

    @OptIn(ExperimentalMaterial3Api::class) // TopAppBar aún es experimental
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel(key = key) {
            ReservationViewModel(container.turnosApi, reservationId, container.reservationStream, container.errorMessages)
        }
        val state by viewModel.state.collectAsState()
        val reservation = state.reservation

        // Escucha los avisos mientras la reserva puede cambiar: al irse la pantalla o terminar la reserva, la conexión se cierra
        if (!state.finished) {
            LaunchedEffect(viewModel) { viewModel.watch() }
        }

        // Al salir, los horarios del profesional reemplazan a esta pantalla
        LaunchedEffect(state.leave) {
            val leave = state.leave ?: return@LaunchedEffect
            val current = reservation ?: return@LaunchedEffect
            navigator.replace(AvailabilityScreen(current.professionalId, "${current.professionalFirstName} ${current.professionalLastName}", leave.notice))
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
                        Text("${reservation.professionalFirstName} ${reservation.professionalLastName}", style = MaterialTheme.typography.titleMedium)
                        Text("${dayTitle(reservation.appointmentDate)}, ${reservation.startTime.take(5)}")
                        when {
                            state.step == ReservationStep.CONFIRMED -> Text("Turno confirmado", style = MaterialTheme.typography.titleMedium)
                            state.slow -> RetryMessage("La cátedra tarda más de lo normal. Tu horario sigue bloqueado unos minutos.", viewModel::retry)
                            state.step == ReservationStep.WAITING_FOR_REQUEST -> Waiting("Pidiendo tu turno a la cátedra…")
                            state.step == ReservationStep.WAITING_FOR_RESULT -> Waiting("Enviando tu teléfono…")
                        }
                    }
                    error != null -> RetryMessage(error, viewModel::retry)
                    else -> Waiting("Cargando la reserva…")
                }
            }
        }

        if (state.step == ReservationStep.ASKING_PHONE && state.leave == null) {
            PhoneDialog(
                phone = state.phone,
                error = state.phoneError,
                sending = state.sending,
                onPhoneChange = viewModel::onPhoneChange,
                onSend = viewModel::sendPhone,
                onCancel = viewModel::onPhoneCancelled
            )
        }
    }
}

// El círculo de carga con lo que se está esperando
@Composable
private fun Waiting(text: String) {
    Centered {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator()
            Text(text)
        }
    }
}
