package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

// Pide el teléfono del paciente, que hace falta para confirmar el turno
@Composable
fun PhoneDialog(
    phone: String,
    error: String?,
    sending: Boolean,
    onPhoneChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!sending) onCancel() }, // Si el usuario vuelve para atrás o toca fuera
        title = { Text("Teléfono del paciente") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hace falta para confirmar el turno. Podés escribirlo con o sin +, espacios o guiones.")
                FormField("Teléfono", phone, onPhoneChange, error, keyboardType = KeyboardType.Phone, enabled = !sending)
            }
        },
        confirmButton = { TextButton(onClick = onSend, enabled = !sending) { Text(if (sending) "Enviando…" else "Enviar") } },
        dismissButton = { TextButton(onClick = onCancel, enabled = !sending) { Text("Cancelar") } }
    )
}
