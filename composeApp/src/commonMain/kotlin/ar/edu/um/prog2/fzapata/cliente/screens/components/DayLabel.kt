package ar.edu.um.prog2.fzapata.cliente.screens.components

// El día de la semana abreviado; el contrato lo manda en inglés (MONDAY...). Uno que no se conoce se muestra como llegó
fun dayLabel(day: String) = when (day) {
    "MONDAY" -> "Lun"
    "TUESDAY" -> "Mar"
    "WEDNESDAY" -> "Mié"
    "THURSDAY" -> "Jue"
    "FRIDAY" -> "Vie"
    "SATURDAY" -> "Sáb"
    "SUNDAY" -> "Dom"
    else -> day
}
