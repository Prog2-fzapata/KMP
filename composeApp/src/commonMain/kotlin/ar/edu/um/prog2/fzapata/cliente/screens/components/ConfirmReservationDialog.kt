package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

// Pide confirmar el horario antes de reservar: la reserva bloquea el horario unos minutos en la cátedra
@Composable
fun ConfirmReservationDialog(
    summary: String,
    reserving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!reserving) onDismiss() },
        title = { Text("¿Reservar este turno?") },
        text = { Text(summary) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !reserving) { Text(if (reserving) "Reservando…" else "Reservar") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !reserving) { Text("Cancelar") } }
    )
}
