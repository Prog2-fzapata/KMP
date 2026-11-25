package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.network.appJson
import ar.edu.um.prog2.fzapata.cliente.network.TurnosResponses
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import ar.edu.um.prog2.fzapata.cliente.screens.components.statusLabel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ReservationSectionsTest {

    // Una reserva con la forma de las reales; solo importan el día y la hora
    private fun reservation(id: Long, date: String, time: String = "10:00:00") = Reservation(id, "CONFIRMED", 207, "Diego", "Alvarez", date, time, false)

    private fun read(json: String) = appJson.decodeFromString<Reservation>(json)

    @Test
    fun theUpcomingOnesGoFromTheNearestAndThePastOnesFromTheMostRecentWithTodayAsUpcoming() {
        // En el orden del servicio: de la más lejana a la más antigua
        val fromTheService = listOf(
            reservation(1, "2027-03-01"),
            reservation(2, "2026-11-26", "16:00:00"),
            reservation(3, "2026-11-26", "09:30:00"),
            reservation(4, "2026-11-25", "08:00:00"),
            reservation(5, "2026-11-24"),
            reservation(6, "2026-10-01")
        )

        val sections = sectionsOf(fromTheService, today = LocalDate(2026, 11, 25))

        assertEquals(listOf(4L, 3L, 2L, 1L), sections.upcoming.map { it.id })
        assertEquals(listOf(5L, 6L), sections.past.map { it.id })
    }

    @Test
    fun theStateIsNamedByWhatIsMissing() {
        assertEquals("Teléfono pendiente", statusLabel(read(TurnosResponses.ASKING_FOR_PHONE)))
        assertEquals("Teléfono pendiente", statusLabel(read(TurnosResponses.CREATED)))
        assertEquals("Esperando confirmación", statusLabel(read(TurnosResponses.PHONE_QUEUED)))
        assertEquals("Confirmada", statusLabel(read(TurnosResponses.CONFIRMED_ONE)))
        assertEquals("Cancelada", statusLabel(read(TurnosResponses.CANCELLED_ONE)))
    }
}
