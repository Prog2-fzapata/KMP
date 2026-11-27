package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.TurnosResponses
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.utils.EmptyContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationDetailViewModelTest {

    // El viewModelScope corre en el hilo principal, que en las pruebas no existe: se reemplaza por uno de prueba
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val requests = mutableListOf<HttpRequestData>()

    // Lo que leer la reserva devuelve (la reserva 17, confirmada, con turno el 2027-02-03) y lo que responde cancelarla
    private var current = TurnosResponses.CONFIRMED_ONE
    private var readStatus = HttpStatusCode.OK
    private var cancelStatus = HttpStatusCode.OK
    private var cancelBody = TurnosResponses.CANCELLED_ONE
    private var cancelFails = false

    private fun newViewModel(today: LocalDate = LocalDate(2026, 11, 25)): ReservationDetailViewModel {
        val engine = MockEngine.create {
            // Sin otro hilo la respuesta llega en el momento: el estado ya está actualizado al comprobarlo
            dispatcher = Dispatchers.Unconfined
            addHandler { request ->
                requests += request
                val json = headersOf(HttpHeaders.ContentType, "application/json")
                when {
                    request.method == HttpMethod.Get -> respond(if (readStatus == HttpStatusCode.OK) current else error("RESERVATION_NOT_FOUND", 404), readStatus, json)
                    cancelFails -> throw IOException("sin red")
                    else -> respond(cancelBody, cancelStatus, json)
                }
            }
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return ReservationDetailViewModel(TurnosApi(client, AppConfig("http://catalogo", "http://turnos")), 17, ErrorMessages(), today)
    }

    private fun error(code: String, status: Int) =
        """{"detail":"x","instance":"/api/reservations/17/cancel","status":$status,"title":"x","path":"/api/reservations/17/cancel","code":"$code"}"""

    private fun ReservationDetailViewModel.cancelWith(reason: String = "") {
        onCancelClick()
        onReasonChange(reason)
        confirmCancel()
    }

    private val cancels get() = requests.filter { it.method == HttpMethod.Post }

    @Test
    fun aConfirmedReservationFromTodayOnCanBeCancelledAndAPastOrUnconfirmedOneCannot() {
        assertTrue(newViewModel().state.value.canCancel)
        assertTrue(newViewModel(today = LocalDate(2027, 2, 3)).state.value.canCancel) // el mismo día
        assertFalse(newViewModel(today = LocalDate(2027, 2, 4)).state.value.canCancel)

        current = TurnosResponses.CANCELLED_ONE
        assertFalse(newViewModel().state.value.canCancel)
        current = TurnosResponses.ASKING_FOR_PHONE
        assertFalse(newViewModel().state.value.canCancel)
    }

    @Test
    fun cancellingSendsTheReasonWithoutSpacesAndLeavesTheReservationCancelled() {
        val viewModel = newViewModel()

        viewModel.cancelWith("  No puedo asistir ")

        assertEquals("""{"reason":"No puedo asistir"}""", (cancels.single().body as TextContent).text)
        val state = viewModel.state.value
        assertEquals("CANCELLED", state.reservation?.status)
        assertFalse(state.asking)
        assertFalse(state.canCancel)
    }

    @Test
    fun withoutAReasonNoBodyIsSentAndTheReasonCannotPassWhatTheServiceAdmits() {
        val viewModel = newViewModel()
        viewModel.onCancelClick()
        viewModel.onReasonChange("a".repeat(600))
        assertEquals(500, viewModel.state.value.reason.length)

        viewModel.onReasonChange("   ")
        viewModel.confirmCancel()

        assertTrue(cancels.single().body is EmptyContent)
    }

    @Test
    fun ifTheReservationIsNoLongerCancellableItIsReadAgainAndTheDialogClosesWithTheMessage() {
        cancelStatus = HttpStatusCode.Conflict
        cancelBody = error("RESERVATION_NOT_CANCELLABLE", 409)
        current = TurnosResponses.CONFIRMED_ONE.replace("\"status\":\"CONFIRMED\"", "\"status\":\"FAILED\"")
        val viewModel = newViewModel()

        viewModel.cancelWith()

        val state = viewModel.state.value
        assertEquals("FAILED", state.reservation?.status)
        assertFalse(state.asking)
        assertEquals("La reserva no se puede cancelar.", state.notice)
    }

    @Test
    fun afterAConflictOrACutTheReservationIsReadBeforeLettingItRepeat() {
        cancelStatus = HttpStatusCode.Conflict
        cancelBody = error("RESERVATION_CONFLICT", 409)
        val viewModel = newViewModel()

        // La cancelación no llegó: la reserva sigue confirmada, el diálogo queda abierto con el mensaje y se puede repetir
        viewModel.cancelWith()
        assertEquals(3, requests.size) // la primera lectura, la cancelación y la lectura de después
        assertTrue(viewModel.state.value.asking)
        assertNotNull(viewModel.state.value.cancelError)

        // Sí llegó: la lectura de después la trae cancelada y no hay error
        current = TurnosResponses.CANCELLED_ONE
        viewModel.confirmCancel()
        assertEquals("CANCELLED", viewModel.state.value.reservation?.status)
        assertNull(viewModel.state.value.cancelError)
        assertFalse(viewModel.state.value.asking)
    }

    @Test
    fun aCutAlsoReadsTheReservationAndKeepsTheDialogOpenWithTheMessage() {
        cancelFails = true
        val viewModel = newViewModel()

        viewModel.cancelWith()

        assertEquals(3, requests.size) // la primera lectura, el intento de cancelar y la lectura de después
        assertTrue(viewModel.state.value.asking)
        assertEquals(ErrorMessages.NO_CONNECTION, viewModel.state.value.cancelError)
        assertFalse(viewModel.state.value.cancelling)
    }

    @Test
    fun whenTheCatedraDoesNotAnswerTheReservationDoesNotChangeAndItIsNotReadAgain() {
        cancelStatus = HttpStatusCode.ServiceUnavailable
        cancelBody = TurnosResponses.CATEDRA_DOWN
        val viewModel = newViewModel()

        viewModel.cancelWith()

        assertEquals(2, requests.size) // la primera lectura y el intento de cancelar
        val state = viewModel.state.value
        assertEquals("CONFIRMED", state.reservation?.status)
        assertEquals("El servicio de la cátedra no responde.", state.cancelError)
        assertTrue(state.asking)
    }

    @Test
    fun aReservationThatNoLongerExistsSendsTheScreenBackToTheList() {
        readStatus = HttpStatusCode.NotFound
        assertTrue(newViewModel().state.value.gone)

        readStatus = HttpStatusCode.OK
        cancelStatus = HttpStatusCode.NotFound
        cancelBody = error("RESERVATION_NOT_FOUND", 404)
        val viewModel = newViewModel()
        viewModel.cancelWith()
        assertTrue(viewModel.state.value.gone)
    }
}
