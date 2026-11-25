package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import kotlinx.datetime.LocalDate

// La lista partida por día. Las que vienen, de hoy en adelante, de la más cercana a la más lejana; las anteriores, de la más reciente
// a la más vieja. La fecha del teléfono solo decide en qué sección va cada reserva, nunca qué se puede hacer con ella.
data class ReservationSections(
    val upcoming: List<Reservation>,
    val past: List<Reservation>
)

// Recibe las reservas en el orden del servicio (de la más lejana a la más antigua)
fun sectionsOf(reservations: List<Reservation>, today: LocalDate): ReservationSections {
    // Divide reservations en past y upcoming a partir del día de hoy
    val (upcoming, past) = reservations.partition { LocalDate.parse(it.appointmentDate) >= today }
    // Retorna ambas secciones ordenadas de la más cercana a la más lejana
    return ReservationSections(upcoming.sortedWith(compareBy({ it.appointmentDate }, { it.startTime })), past)
}
