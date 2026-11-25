package ar.edu.um.prog2.fzapata.cliente.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import kotlinx.datetime.LocalDate

// Un día con sus horarios libres: el título y las horas una al lado de otra, pasando de renglón cuando no entran
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DaySlots(date: String, slots: List<Slot>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(dayTitle(date), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            slots.forEach { slot ->
                Surface(shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Text(slot.startTime.take(5), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
}

// «Mié 25 de noviembre»
private fun dayTitle(date: String): String {
    val day = LocalDate.parse(date)
    return "${dayLabel(day.dayOfWeek.name)} ${day.day} de ${MONTHS[day.month.ordinal]}"
}

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)
