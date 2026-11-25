package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.StartReservationRequest
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TurnosApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun apiAnswering(body: String, status: HttpStatusCode = HttpStatusCode.OK): TurnosApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return TurnosApi(client, AppConfig("http://catalogo", "http://turnos"))
    }

    @Test
    fun theSlotsAndTheRangeComeFromARealResponse() = runTest {
        val slots = apiAnswering(AVAILABILITY).availability(207, LocalDate(2026, 11, 25), LocalDate(2026, 11, 30))

        assertEquals(listOf("12:30:00", "13:00:00", "13:30:00", "14:00:00", "14:30:00"), slots.map { it.startTime })
        val sent = requests.single().url
        assertEquals("http://turnos/api/availability", "${sent.protocol.name}://${sent.host}${sent.encodedPath}")
        assertEquals("207", sent.parameters["professionalId"])
        assertEquals("2026-11-25", sent.parameters["from"])
        assertEquals("2026-11-30", sent.parameters["to"])
    }

    @Test
    fun theSlotsAreSortedByDayAndHourEvenIfTheyComeShuffled() = runTest {
        val shuffled = """{"slots":[
            {"date":"2026-11-30","startTime":"18:00:00"},
            {"date":"2026-11-25","startTime":"13:00:00"},
            {"date":"2026-11-25","startTime":"12:30:00"}
        ]}"""

        val slots = apiAnswering(shuffled).availability(207, LocalDate(2026, 11, 25), LocalDate(2026, 11, 30))

        assertEquals(listOf("2026-11-25 12:30:00", "2026-11-25 13:00:00", "2026-11-30 18:00:00"), slots.map { "${it.date} ${it.startTime}" })
    }

    @Test
    fun startingAReservationSendsTheSlotAndReadsTheReservation() = runTest {
        val api = apiAnswering(TurnosResponses.CREATED, HttpStatusCode.Created)

        val reservation = api.startReservation(StartReservationRequest(207, "2027-01-25", "18:00:00"))

        val sent = requests.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("/api/reservations", sent.url.encodedPath)
        assertEquals("""{"professionalId":207,"date":"2027-01-25","startTime":"18:00:00"}""", (sent.body as TextContent).text)
        assertEquals("WAITING_PHONE", reservation.status)
        assertEquals("Diego Alvarez", "${reservation.professionalFirstName} ${reservation.professionalLastName}")
        assertEquals("2027-01-25 18:00:00", "${reservation.appointmentDate} ${reservation.startTime}")
    }

    @Test
    fun aReservationIsReadByItsIdAndTellsIfThePhoneCanBeSent() = runTest {
        val reservation = apiAnswering(TurnosResponses.ASKING_FOR_PHONE).reservation(9)

        val sent = requests.single()
        assertEquals(HttpMethod.Get, sent.method)
        assertEquals("/api/reservations/9", sent.url.encodedPath)
        assertEquals(9, reservation.id)
        assertEquals("WAITING_PHONE", reservation.status)
        assertTrue(reservation.canSubmitPhone)
    }

    @Test
    fun sendingThePhoneSendsTheNumberAndReadsTheQueuedReservation() = runTest {
        val reservation = apiAnswering(TurnosResponses.PHONE_QUEUED, HttpStatusCode.Accepted).sendPhone(9, "+5492613124578")

        val sent = requests.single()
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("/api/reservations/9/phone", sent.url.encodedPath)
        assertEquals("""{"phoneNumber":"+5492613124578"}""", (sent.body as TextContent).text)
        assertEquals("PHONE_QUEUED", reservation.status)
        assertFalse(reservation.canSubmitPhone)
    }

    // Respuesta real de turnos (2026-11-24, profesional 207, un día); trae también campos que la app ignora
    private companion object {
        const val AVAILABILITY = """{"professionalId":207,"from":"2026-11-25","to":"2026-11-25","slots":[{"date":"2026-11-25","startTime":"12:30:00","endTime":"13:00:00"},{"date":"2026-11-25","startTime":"13:00:00","endTime":"13:30:00"},{"date":"2026-11-25","startTime":"13:30:00","endTime":"14:00:00"},{"date":"2026-11-25","startTime":"14:00:00","endTime":"14:30:00"},{"date":"2026-11-25","startTime":"14:30:00","endTime":"15:00:00"}]}"""
    }
}
