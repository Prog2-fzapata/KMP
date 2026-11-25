package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.TurnosResponses
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.isSuccess
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
import kotlin.test.assertNull
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
        return viewModelFor(client)
    }

    // Contesta cada pedido con lo que decide el guion: los errores salen como problem+json, como en los servicios
    private fun viewModelServing(script: (HttpRequestData) -> Pair<HttpStatusCode, String>): AvailabilityViewModel {
        val engine = MockEngine { request ->
            requests += request
            val (status, body) = script(request)
            val type = if (status.isSuccess()) "application/json" else "application/problem+json"
            respond(body, status, headersOf(HttpHeaders.ContentType, type))
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return viewModelFor(client)
    }

    private fun viewModelFor(client: HttpClient): AvailabilityViewModel {
        return AvailabilityViewModel(TurnosApi(client, AppConfig("http://catalogo", "http://turnos")), 207, ErrorMessages())
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

    @Test
    fun aTakenSlotRefreshesTheSlotsAndShowsANotice() = runTest {
        var availabilityRequests = 0
        val viewModel = viewModelServing { request ->
            when {
                request.method == HttpMethod.Post -> HttpStatusCode.Conflict to TurnosResponses.TAKEN
                availabilityRequests++ == 0 -> HttpStatusCode.OK to TWO_SLOTS
                else -> HttpStatusCode.OK to ONE_SLOT
            }
        }
        viewModel.state.first { !it.loading }

        viewModel.onSlotSelected(SLOT)
        viewModel.confirmReservation()
        val state = viewModel.state.first { it.notice != null && !it.loading && !it.reserving }

        assertEquals(listOf("18:30:00"), state.slots.map { it.startTime })
        assertTrue(state.notice!!.contains("ya no está disponible"))
        assertNull(state.selected)
    }

    @Test
    fun aSecondTapWhileReservingDoesNotSendAnotherRequest() = runTest {
        var posts = 0
        val viewModel = viewModelServing { request ->
            if (request.method == HttpMethod.Post) {
                posts++
                HttpStatusCode.Created to TurnosResponses.CREATED
            } else {
                HttpStatusCode.OK to TWO_SLOTS
            }
        }
        viewModel.state.first { !it.loading }

        viewModel.onSlotSelected(SLOT)
        viewModel.confirmReservation()
        viewModel.confirmReservation()
        val state = viewModel.state.first { it.reservation != null }

        assertEquals(1, posts)
        assertEquals("2027-01-25", state.reservation!!.appointmentDate)
    }

    @Test
    fun aMissingNameOpensTheNameFormAndKeepsTheSlot() = runTest {
        val viewModel = viewModelServing { request ->
            if (request.method == HttpMethod.Post) HttpStatusCode.BadRequest to TurnosResponses.NAME_REQUIRED else HttpStatusCode.OK to TWO_SLOTS
        }
        viewModel.state.first { !it.loading }

        viewModel.onSlotSelected(SLOT)
        viewModel.confirmReservation()
        val state = viewModel.state.first { it.needsPatientName }

        assertEquals(SLOT, state.selected)
        assertFalse(state.reserving)
    }

    private companion object {
        const val EMPTY = """{"slots":[]}"""
        const val TWO_SLOTS = """{"slots":[{"date":"2027-01-25","startTime":"18:00:00"},{"date":"2027-01-25","startTime":"18:30:00"}]}"""
        const val ONE_SLOT = """{"slots":[{"date":"2027-01-25","startTime":"18:30:00"}]}"""
        val SLOT = Slot("2027-01-25", "18:00:00")

    }
}
