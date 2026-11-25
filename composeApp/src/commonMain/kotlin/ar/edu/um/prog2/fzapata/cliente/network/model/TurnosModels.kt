package ar.edu.um.prog2.fzapata.cliente.network.model

import kotlinx.serialization.Serializable

// Solo los campos que la app usa; el resto de la respuesta se ignora
@Serializable
data class Availability(
    val slots: List<Slot>
)

// date es yyyy-MM-dd y startTime es HH:mm:ss: como texto ordenan igual que como fecha y hora
@Serializable
data class Slot(
    val date: String,
    val startTime: String
)
