package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.ProfessionalFilters
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CatalogApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val requests = mutableListOf<HttpRequestData>()

    private fun apiAnswering(body: String, total: String?): CatalogApi {
        val headers = if (total == null) jsonHeaders else headersOf(
            HttpHeaders.ContentType to listOf("application/json"),
            "X-Total-Count" to listOf(total)
        )
        val engine = MockEngine { request ->
            requests += request
            respond(body, HttpStatusCode.OK, headers)
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return CatalogApi(client, AppConfig("http://catalogo", "http://turnos"))
    }

    @Test
    fun theListAndTheTotalCountComeFromARealResponse() = runTest {
        val page = apiAnswering(PROFESSIONALS, total = "24").professionals(ProfessionalFilters(), page = 0)

        assertEquals(24, page.total)
        assertEquals(listOf("Acosta", "Alvarez"), page.items.map { it.lastName })
        assertEquals("TUESDAY", page.items.first().weeklySchedules.first().dayOfWeek)
    }

    @Test
    fun withoutTheTotalHeaderThereAreNoMorePages() = runTest {
        val page = apiAnswering(PROFESSIONALS, total = null).professionals(ProfessionalFilters(), page = 1)

        assertEquals(CatalogApi.PROFESSIONAL_PAGE_SIZE + 2, page.total)
    }

    @Test
    fun onlyTheActiveFiltersAreSent() = runTest {
        val api = apiAnswering(PROFESSIONALS, total = "2")

        api.professionals(ProfessionalFilters(onlyEnabled = false), page = 0)
        val none = requests.last().url.parameters
        assertEquals("0", none["page"])
        assertEquals("${CatalogApi.PROFESSIONAL_PAGE_SIZE}", none["size"])
        assertNull(none["enabled"])
        assertNull(none["categoryId"])
        assertNull(none["name"])
        assertNull(none["available"])

        api.professionals(ProfessionalFilters(categoryId = 103, name = " ana ", onlyAvailable = true), page = 2)
        val all = requests.last().url.parameters
        assertEquals("2", all["page"])
        assertEquals("103", all["categoryId"])
        assertEquals("ana", all["name"])
        assertEquals("true", all["enabled"])
        assertEquals("true", all["available"])
    }

    // Respuesta real del catálogo (2026-11-24, size=2); trae también campos que la app ignora
    private companion object {
        const val PROFESSIONALS = """[{"id":218,"categoryId":107,"categoryName":"Ginecologia","firstName":"Romina","lastName":"Acosta","enabled":true,"weeklySchedules":[{"dayOfWeek":"TUESDAY","startTime":"13:00:00","endTime":"16:00:00","slotDurationMinutes":30},{"dayOfWeek":"THURSDAY","startTime":"11:00:00","endTime":"14:00:00","slotDurationMinutes":30}]},{"id":207,"categoryId":103,"categoryName":"Clinica medica","firstName":"Diego","lastName":"Alvarez","enabled":true,"weeklySchedules":[{"dayOfWeek":"MONDAY","startTime":"18:00:00","endTime":"21:00:00","slotDurationMinutes":30},{"dayOfWeek":"WEDNESDAY","startTime":"12:00:00","endTime":"15:00:00","slotDurationMinutes":30}]}]"""
    }
}
