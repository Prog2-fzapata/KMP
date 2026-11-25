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

// El usuario de la reserva sale del token, no del cuerpo
@Serializable
data class StartReservationRequest(
    val professionalId: Long,
    val date: String,
    val startTime: String
)

// status es texto: el contrato puede sumar estados. El profesional y el turno son los de la reserva, para reconocerla en la lista
@Serializable
data class Reservation(
    val status: String,
    val professionalId: Long,
    val professionalFirstName: String,
    val professionalLastName: String,
    val appointmentDate: String,
    val startTime: String
)
