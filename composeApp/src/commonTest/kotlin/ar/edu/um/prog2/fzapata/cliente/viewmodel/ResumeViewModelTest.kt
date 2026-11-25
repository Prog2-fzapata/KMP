package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.TurnosResponses
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ResumeViewModelTest {

    // El viewModelScope corre en el hilo principal, que en las pruebas no existe: se reemplaza por uno de prueba
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModelAnswering(body: String, status: HttpStatusCode = HttpStatusCode.OK): ResumeViewModel {
        val engine = MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return ResumeViewModel(TurnosApi(client, AppConfig("http://catalogo", "http://turnos")))
    }

    @Test
    fun theReservationInProgressIsTheOneToResumeAndAConfirmedOneIsNot() = runTest {
        // La lista real trae una reserva que pide el teléfono (18) y otra ya confirmada (17)
        assertEquals(18L, viewModelAnswering(TurnosResponses.IN_PROGRESS_AND_CONFIRMED).findInProgress())
    }

    @Test
    fun withNothingInProgressOrWithTheServiceDownThereIsNothingToResume() = runTest {
        assertNull(viewModelAnswering(TurnosResponses.ONLY_CONFIRMED).findInProgress())
        assertNull(viewModelAnswering("[]").findInProgress())
        assertNull(viewModelAnswering("{}", HttpStatusCode.ServiceUnavailable).findInProgress())
    }
}
