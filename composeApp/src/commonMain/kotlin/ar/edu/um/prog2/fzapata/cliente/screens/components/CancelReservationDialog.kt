package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ReservationDetailViewModel

// Pide confirmar la cancelación de un turno, con un motivo opcional de varias líneas
@Composable
fun CancelReservationDialog(
    summary: String,
    reason: String,
    error: String?,
    cancelling: Boolean,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cancelar este turno?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(summary)
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Motivo (opcional)") },
                    supportingText = { Text("${reason.length}/${ReservationDetailViewModel.MAX_REASON}") },
                    minLines = 3,
                    maxLines = 5,
                    enabled = !cancelling,
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !cancelling) { Text("Cancelar turno") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !cancelling) { Text("Volver") } }
    )
}
