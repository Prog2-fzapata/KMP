package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.ReservationStream
import ar.edu.um.prog2.fzapata.cliente.network.StreamEvent
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.TurnosResponses
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReservationViewModelTest {

    private val requests = mutableListOf<HttpRequestData>()

    // Lo que el servicio responde al leer la reserva; cada prueba lo cambia para contar cómo avanza la cátedra
    private var current = TurnosResponses.ASKING_FOR_PHONE

    // La conexión del stream no termina: cada prueba le manda los avisos a mano
    private val notices = MutableSharedFlow<StreamEvent>()

    // El viewModelScope corre en el hilo principal, que en las pruebas no existe: se reemplaza por uno de prueba
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // Leer la reserva devuelve current; enviar el teléfono la deja en PHONE_QUEUED, como el servicio
    private fun viewModel(): ReservationViewModel {
        val engine = MockEngine.create {
            // Sin otro hilo el pedido queda registrado en el momento: así se puede comprobar que algo no se pidió
            dispatcher = Dispatchers.Unconfined
            addHandler { request ->
                requests += request
                val json = headersOf(HttpHeaders.ContentType, "application/json")
                if (request.method == HttpMethod.Post) respond(TurnosResponses.PHONE_QUEUED, HttpStatusCode.Accepted, json) else respond(current, HttpStatusCode.OK, json)
            }
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        val stream = ReservationStream({ notices }, {})
        return ReservationViewModel(TurnosApi(client, AppConfig("http://catalogo", "http://turnos")), 9, stream, ErrorMessages())
    }

    @Test
    fun thePhoneIsNotSentBeforeTheCatedraAsksForIt() = runTest {
        current = TurnosResponses.CREATED
        val viewModel = viewModel()
        viewModel.state.first { it.reservation != null }

        viewModel.onPhoneChange("2613124578")
        viewModel.sendPhone()

        assertEquals(ReservationStep.WAITING_FOR_REQUEST, viewModel.state.value.step)
        assertFalse(viewModel.state.value.sending)
        assertTrue(requests.none { it.method == HttpMethod.Post })
    }

    @Test
    fun aNumberTheCatedraRejectsBringsTheDialogBackWithItsMessage() = runTest {
        val viewModel = viewModel()
        backgroundScope.launch { viewModel.watch() }
        viewModel.state.first { it.step == ReservationStep.ASKING_PHONE }

        viewModel.onPhoneChange("+54 9 261 312 4578")
        viewModel.sendPhone()
        viewModel.state.first { it.step == ReservationStep.WAITING_FOR_RESULT }
        assertEquals("""{"phoneNumber":"+5492613124578"}""", (requests.last { it.method == HttpMethod.Post }.body as TextContent).text)

        // El aviso de otra reserva no hace leer esta
        notices.subscriptionCount.first { it > 0 }
        val readsBefore = requests.size
        notices.emit(StreamEvent.Updated(99))
        assertEquals(readsBefore, requests.size)

        // La cátedra rechaza el número: el servicio vuelve a pedirlo y el stream avisa
        current = TurnosResponses.ASKING_FOR_PHONE
        notices.emit(StreamEvent.Updated(9))

        val state = viewModel.state.first { it.step == ReservationStep.ASKING_PHONE }
        assertEquals("La cátedra no aceptó el número. Probá con otro.", state.phoneError)
        assertEquals("+54 9 261 312 4578", state.phone)
    }

    @Test
    fun anExpiredReservationLeavesForTheSlotsWithANotice() = runTest {
        current = TurnosResponses.EXPIRED

        val state = viewModel().state.first { it.leave != null }

        assertEquals("El tiempo para confirmar la reserva terminó. Elegí el horario de nuevo.", state.leave?.notice)
        assertTrue(state.finished)
    }

    // La misma reserva vencida, pero fallida por el motivo que se indique
    private fun failedBecause(reason: String) =
        TurnosResponses.EXPIRED.replace("\"status\":\"EXPIRED\"", "\"status\":\"FAILED\"").replace("HOLD_EXPIRED", reason)

    @Test
    fun aFailedReservationLeavesWithItsReasonOrAGenericNotice() = runTest {
        current = failedBecause("SLOT_ALREADY_HELD")
        assertEquals("Ese horario ya no está disponible. Elegí el horario de nuevo.", viewModel().state.first { it.leave != null }.leave?.notice)

        current = failedBecause("MOTIVO_NUEVO")
        assertEquals("No se pudo completar la reserva. Elegí el horario de nuevo.", viewModel().state.first { it.leave != null }.leave?.notice)
    }
}
