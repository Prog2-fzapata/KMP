package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation

// En qué punto está la reserva para quien la mira; la pantalla dibuja según esto y no según el texto del estado
enum class ReservationStep {
    // Falta que la cátedra pida el teléfono
    WAITING_FOR_REQUEST,

    // La cátedra lo pidió: se puede enviar (también cuando rechazó un número)
    ASKING_PHONE,

    // El teléfono se envió: falta que la cátedra lo confirme o lo rechace
    WAITING_FOR_RESULT,

    CONFIRMED,

    // Venció, falló o se canceló: no hay más que hacer con esta reserva
    OVER;

    companion object {
        // status es texto abierto: lo que no se reconoce cuenta como que falta el pedido de la cátedra
        fun of(reservation: Reservation): ReservationStep = when {
            reservation.status == "CONFIRMED" -> CONFIRMED
            reservation.status in OVER_STATUSES -> OVER
            reservation.canSubmitPhone -> ASKING_PHONE
            reservation.status in SENT_STATUSES -> WAITING_FOR_RESULT
            else -> WAITING_FOR_REQUEST
        }

        private val OVER_STATUSES = setOf("EXPIRED", "FAILED", "CANCELLED")
        private val SENT_STATUSES = setOf("PHONE_QUEUED", "PHONE_SUBMITTED")
    }
}
