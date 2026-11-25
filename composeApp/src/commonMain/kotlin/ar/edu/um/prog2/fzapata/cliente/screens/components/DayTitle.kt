package ar.edu.um.prog2.fzapata.cliente.screens.components

import kotlinx.datetime.LocalDate

// «Mié 25 de noviembre»
fun dayTitle(date: String): String {
    val day = LocalDate.parse(date)
    return "${dayLabel(day.dayOfWeek.name)} ${day.day} de ${MONTHS[day.month.ordinal]}"
}

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
)
