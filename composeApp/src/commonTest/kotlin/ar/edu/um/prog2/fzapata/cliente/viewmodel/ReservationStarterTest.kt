package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.TurnosResponses
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ReservationStarterTest {

    private var posts = 0

    // El POST responde un conflicto y la lista de reservas, lo que se le diga
    private fun starterWith(reservations: String): ReservationStarter {
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Post) {
                posts++
                respond(CONFLICT, HttpStatusCode.Conflict, headersOf(HttpHeaders.ContentType, "application/problem+json"))
            } else {
                respond(reservations, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        val messages = ErrorMessages()
        return ReservationStarter(TurnosApi(client, AppConfig("http://catalogo", "http://turnos")), messages)
    }

    // Un conflicto no dice si la reserva existe: antes de dejar repetir se busca en las reservas del usuario
    @Test
    fun afterAConflictItOpensTheReservationThatAlreadyExists() = runTest {
        val result = starterWith("[${TurnosResponses.CREATED}]").start(207, SLOT)

        assertEquals(1, posts)
        assertEquals("18:00:00", assertIs<StartResult.Created>(result).reservation.startTime)
    }

    // La lista trae también las canceladas: una vieja reserva cancelada de ese horario no es la que se acaba de pedir
    @Test
    fun aCancelledReservationOfTheSameSlotIsNotTheOneJustRequested() = runTest {
        val cancelled = TurnosResponses.CREATED.replace("WAITING_PHONE", "CANCELLED")

        val result = starterWith("[$cancelled]").start(207, SLOT)

        assertIs<StartResult.Failed>(result)
    }

    private companion object {
        val SLOT = Slot("2027-01-25", "18:00:00")

        // Con la forma de un problem+json de turnos, para el caso que no se pudo provocar
        const val CONFLICT = """{"detail":"La reserva cambió mientras se procesaba","status":409,"title":"Conflict","code":"RESERVATION_CONFLICT"}"""
    }
}
