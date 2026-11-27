package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.edu.um.prog2.fzapata.cliente.LocalAppContainer
import ar.edu.um.prog2.fzapata.cliente.viewmodel.PatientNameViewModel

// Pide el nombre y el apellido que turnos necesita para anotar el turno; al guardarlos avisa para repetir la reserva
@Composable
fun PatientNameDialog(
    viewModelKey: String,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = viewModelKey) { PatientNameViewModel(container.userApi, container.errorMessages) }
    val state by viewModel.state.collectAsState()

    AlertDialog(
        onDismissRequest = { if (!state.saving) onDismiss() },
        title = { Text("Tus datos para reservar") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("El turno se anota a tu nombre.")
                FormField("Nombre", state.firstName, viewModel::onFirstNameChange, state.fieldErrors["firstName"], enabled = !state.saving)
                FormField("Apellido", state.lastName, viewModel::onLastNameChange, state.fieldErrors["lastName"], enabled = !state.saving)
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.save(onSaved) }, enabled = !state.saving) { Text("Guardar y reservar") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.saving) { Text("Cancelar") } }
    )
}
