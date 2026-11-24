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
import ar.edu.um.prog2.fzapata.cliente.network.model.Professional

// Un profesional en la lista: nombre, categoría y resumen de horarios
@Composable
fun ProfessionalCard(
    professional: Professional,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(professional.fullName(), style = MaterialTheme.typography.titleMedium)
            Text(professional.categoryName)
            Text(professional.schedulesText(), style = MaterialTheme.typography.bodySmall)
            if (!professional.enabled) {
                Text("No habilitado", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun Professional.fullName() = "$firstName $lastName"

// «Mar 13:00–16:00 · Jue 11:00–14:00». Un día que no se conoce se muestra como llegó
private fun Professional.schedulesText(): String =
    if (weeklySchedules.isEmpty()) "Sin horarios"
    else weeklySchedules.joinToString(" · ") { "${dayLabel(it.dayOfWeek)} ${it.startTime.take(5)}–${it.endTime.take(5)}" }

private fun dayLabel(day: String) = when (day) {
    "MONDAY" -> "Lun"
    "TUESDAY" -> "Mar"
    "WEDNESDAY" -> "Mié"
    "THURSDAY" -> "Jue"
    "FRIDAY" -> "Vie"
    "SATURDAY" -> "Sáb"
    "SUNDAY" -> "Dom"
    else -> day
}
