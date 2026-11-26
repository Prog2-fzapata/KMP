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

// status es texto: el contrato puede sumar estados. El profesional y el turno son los de la reserva, para reconocerla en la lista.
// canSubmitPhone es true cuando la cátedra ya pidió el teléfono, y vuelve a serlo si lo rechaza.
// failureReason es texto abierto: solo trae un valor cuando la reserva falló o venció
@Serializable
data class Reservation(
    val id: Long,
    val status: String,
    val professionalId: Long,
    val professionalFirstName: String,
    val professionalLastName: String,
    val appointmentDate: String,
    val startTime: String,
    val canSubmitPhone: Boolean,
    val failureReason: String? = null
)

// El aviso del stream trae un resumen de la reserva; la app toma el id y consulta la reserva, que es la fuente de verdad
@Serializable
data class ReservationUpdate(
    val id: Long
)

// El motivo es opcional: sin motivo la cancelación se envía sin cuerpo
@Serializable
data class CancelReservationRequest(
    val reason: String
)

// El número viaja limpio (sin espacios ni guiones), pero con el + si lo tiene
@Serializable
data class SendPhoneRequest(
    val phoneNumber: String
)
