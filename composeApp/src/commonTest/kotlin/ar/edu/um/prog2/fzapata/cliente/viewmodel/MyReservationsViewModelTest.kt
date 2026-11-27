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
import io.ktor.http.HttpStatusCode
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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MyReservationsViewModelTest {

    // El viewModelScope corre en el hilo principal, que en las pruebas no existe: se reemplaza por uno de prueba
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val today = LocalDate(2026, 11, 25)
    private val requests = mutableListOf<HttpRequestData>()
    private val notices = MutableSharedFlow<StreamEvent>()

    // Un servicio de mentira que pagina de a 20 y filtra por estado, con las reservas en su orden: de la más lejana a la más antigua
    private var server: List<Triple<Long, String, String>> = emptyList() // id, estado, día
    private var failing = false

    private fun json(id: Long, status: String, date: String) =
        """{"id":$id,"status":"$status","professionalId":207,"professionalFirstName":"Diego","professionalLastName":"Alvarez","appointmentDate":"$date","startTime":"10:00:00","canSubmitPhone":false}"""

    private fun newViewModel(): MyReservationsViewModel {
        val engine = MockEngine.create {
            // Sin otro hilo la respuesta llega en el momento: el estado ya está actualizado al comprobarlo
            dispatcher = Dispatchers.Unconfined
            addHandler { request ->
                requests += request
                if (failing) return@addHandler respond(TurnosResponses.CATEDRA_DOWN, HttpStatusCode.ServiceUnavailable, headersOf(HttpHeaders.ContentType, "application/json"))
                val page = request.url.parameters["page"]!!.toInt()
                val status = request.url.parameters["status"]
                val matching = server.filter { status == null || it.second == status }
                val body = matching.drop(page * 20).take(20).joinToString(",", "[", "]") { json(it.first, it.second, it.third) }
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType to listOf("application/json"), "X-Total-Count" to listOf(matching.size.toString())))
            }
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return MyReservationsViewModel(TurnosApi(client, AppConfig("http://catalogo", "http://turnos")), ReservationStream({ notices }, {}), ErrorMessages(), today)
    }

    // `future` reservas de hoy en adelante y `past` anteriores, en el orden del servicio
    private fun reservations(future: Int, past: Int, status: String = "CONFIRMED"): List<Triple<Long, String, String>> =
        (0 until future).map { Triple(it + 1L, status, today.plus(future - it, DateTimeUnit.DAY).toString()) } +
            (0 until past).map { Triple(100L + it, status, today.minus(it + 1, DateTimeUnit.DAY).toString()) }

    private val pages get() = requests.map { it.url.parameters["page"] }

    @Test
    fun itReadsPagesUntilTheFirstPastOneSoTheUpcomingOnesAreCompleteAndInOrder() {
        server = reservations(future = 25, past = 10)

        val state = newViewModel().state.value

        assertEquals(listOf("0", "1"), pages) // la primera página tiene solo las que vienen: falta leer la siguiente
        assertFalse(state.loading)
        assertEquals(25, state.sections.upcoming.size)
        assertEquals(today.plus(1, DateTimeUnit.DAY).toString(), state.sections.upcoming.first().appointmentDate)
        assertEquals(today.minus(1, DateTimeUnit.DAY).toString(), state.sections.past.first().appointmentDate)
    }

    @Test
    fun withAllThePagesReadOrWithAPastOneAlreadyThereItDoesNotAskForMore() {
        server = reservations(future = 3, past = 0)
        val viewModel = newViewModel()
        assertEquals(listOf("0"), pages)
        viewModel.loadMore() // no hay más
        assertEquals(listOf("0"), pages)

        requests.clear()
        server = reservations(future = 5, past = 40)
        val withMore = newViewModel()
        assertEquals(listOf("0"), pages) // ya trae una anterior, aunque haya más páginas
        assertTrue(withMore.state.value.hasMore)
    }

    @Test
    fun scrollingDownAddsThePastOnesAndTheFilterStartsOverWithItsStatus() {
        server = reservations(future = 5, past = 40)
        val viewModel = newViewModel()

        viewModel.loadMore()
        assertEquals(40, viewModel.state.value.reservations.size)
        assertEquals(35, viewModel.state.value.sections.past.size)

        requests.clear()
        server = reservations(future = 2, past = 3) + listOf(Triple(500L, "CANCELLED", today.minus(9, DateTimeUnit.DAY).toString()))
        viewModel.onStatusSelected("CANCELLED")

        assertEquals("CANCELLED", requests.single().url.parameters["status"])
        assertEquals("0", requests.single().url.parameters["page"])
        assertEquals(listOf(500L), viewModel.state.value.reservations.map { it.id })
    }

    @Test
    fun aNoticeReadsAgainWhatWasLoadedAndTheListUpdatesWhereItIs() = runTest {
        server = reservations(future = 5, past = 40)
        val viewModel = newViewModel()
        viewModel.loadMore() // dos páginas cargadas
        backgroundScope.launch { viewModel.watch() }
        notices.subscriptionCount.first { it > 0 }
        requests.clear()

        // Una reserva se cancela y el stream avisa: se vuelven a leer las dos páginas y la reserva cambia donde estaba
        server = server.map { if (it.first == 3L) Triple(3L, "CANCELLED", it.third) else it }
        notices.emit(StreamEvent.Updated(3))

        assertEquals(listOf("0", "1"), pages)
        val state = viewModel.state.value
        assertEquals(40, state.reservations.size)
        assertEquals("CANCELLED", state.reservations.first { it.id == 3L }.status)
    }

    @Test
    fun aFailedReadShowsTheErrorAndRetryStartsOverButAFailedNoticeKeepsTheList() = runTest {
        failing = true
        val viewModel = newViewModel()
        assertNotNull(viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)

        failing = false
        server = reservations(future = 2, past = 2)
        viewModel.retry()
        assertNull(viewModel.state.value.error)
        assertEquals(4, viewModel.state.value.reservations.size)

        // Con la lista cargada, un aviso que no se puede atender no la borra ni muestra un error
        backgroundScope.launch { viewModel.watch() }
        notices.subscriptionCount.first { it > 0 }
        failing = true
        notices.emit(StreamEvent.Updated(1))
        assertEquals(4, viewModel.state.value.reservations.size)
        assertNull(viewModel.state.value.error)
    }
}
