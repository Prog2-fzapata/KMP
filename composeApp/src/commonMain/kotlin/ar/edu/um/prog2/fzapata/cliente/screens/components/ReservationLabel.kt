package ar.edu.um.prog2.fzapata.cliente.screens.components

import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import ar.edu.um.prog2.fzapata.cliente.viewmodel.ReservationStep

// Cómo se nombra el estado de una reserva. Mientras falta el teléfono, o su resultado, se dice qué falta
fun statusLabel(reservation: Reservation): String = when (ReservationStep.of(reservation)) {
    ReservationStep.WAITING_FOR_REQUEST, ReservationStep.ASKING_PHONE -> "Teléfono pendiente"
    ReservationStep.WAITING_FOR_RESULT -> "Esperando confirmación"
    ReservationStep.CONFIRMED -> "Confirmada"
    // Las vencidas y las fallidas no llegan a la lista: lo terminado que se ve es una cancelada
    ReservationStep.OVER -> "Cancelada"
}
