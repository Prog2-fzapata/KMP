package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

// Una casilla con su texto: se puede tocar en cualquier parte de la fila, no solo en la casilla
@Composable
fun LabeledCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // El toque lo recibe la fila; la casilla solo dibuja el estado
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        Text(label)
    }
}
