package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class TurnosApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun apiAnswering(body: String): TurnosApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
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

    // Respuesta real de turnos (2026-11-24, profesional 207, un día); trae también campos que la app ignora
    private companion object {
        const val AVAILABILITY = """{"professionalId":207,"from":"2026-11-25","to":"2026-11-25","slots":[{"date":"2026-11-25","startTime":"12:30:00","endTime":"13:00:00"},{"date":"2026-11-25","startTime":"13:00:00","endTime":"13:30:00"},{"date":"2026-11-25","startTime":"13:30:00","endTime":"14:00:00"},{"date":"2026-11-25","startTime":"14:00:00","endTime":"14:30:00"},{"date":"2026-11-25","startTime":"14:30:00","endTime":"15:00:00"}]}"""
    }
}
