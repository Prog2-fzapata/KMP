package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AvailabilityViewModelTest {

    private val requests = mutableListOf<HttpRequestData>()

    // El viewModelScope corre en el hilo principal, que en las pruebas no existe: se reemplaza por uno de prueba
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // Responde con los horarios de cada pedido, en orden; con el último repite sin horarios
    private fun viewModelAnswering(vararg bodies: String): AvailabilityViewModel {
        val engine = MockEngine { request ->
            val body = bodies.getOrElse(requests.size) { EMPTY }
            requests += request
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return AvailabilityViewModel(207, TurnosApi(client, AppConfig("http://catalogo", "http://turnos")), ErrorMessages())
    }

    private fun param(request: HttpRequestData, name: String) = LocalDate.parse(request.url.parameters[name]!!)

    @Test
    fun emptyWindowsAreSkippedUntilOneHasSlots() = runTest {
        val viewModel = viewModelAnswering(EMPTY, EMPTY, """{"slots":[{"date":"2027-01-05","startTime":"09:00:00"}]}""")

        val state = viewModel.state.first { !it.loading }

        assertEquals(3, requests.size)
        assertEquals(1, state.slots.size)
        assertTrue(state.hasMore)
        // Cada ventana empieza el día después de que termina la anterior
        assertEquals(param(requests[0], "to").plus(1, DateTimeUnit.DAY), param(requests[1], "from"))
        assertEquals(param(requests[1], "to").plus(1, DateTimeUnit.DAY), param(requests[2], "from"))
    }

    @Test
    fun withoutSlotsItStopsAtTheLastDayOfTheYear() = runTest {
        val viewModel = viewModelAnswering()

        val state = viewModel.state.first { !it.loading }

        // 365 días en ventanas de 30: doce enteras y una de 5; la última no pasa del último día
        assertEquals(13, requests.size)
        assertEquals(param(requests.first(), "from").plus(AvailabilityViewModel.BOOKING_HORIZON_DAYS - 1, DateTimeUnit.DAY), param(requests.last(), "to"))
        assertTrue(state.slots.isEmpty())
        assertFalse(state.hasMore)
    }

    private companion object {
        const val EMPTY = """{"slots":[]}"""
    }
}
