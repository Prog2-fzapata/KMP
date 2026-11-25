package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation

// Una reserva en la lista: profesional, día y hora, y su estado
@Composable
fun ReservationCard(
    reservation: Reservation,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${reservation.professionalFirstName} ${reservation.professionalLastName}", style = MaterialTheme.typography.titleMedium)
            Text("${dayTitle(reservation.appointmentDate)}, ${reservation.startTime.take(5)}")
            Text(statusLabel(reservation), style = MaterialTheme.typography.bodySmall)
        }
    }
}
