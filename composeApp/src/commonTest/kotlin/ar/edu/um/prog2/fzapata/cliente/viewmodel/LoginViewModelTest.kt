package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.UserApi
import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // El viewModelScope corre en el hilo principal, que en las pruebas no existe: se reemplaza por uno de prueba
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val session = SessionManager(FakeSessionStore("jwt"))

    private fun viewModel(): LoginViewModel {
        val client = HttpClientFactory(session).create(MockEngine { respond("", HttpStatusCode.Unauthorized) })
        return LoginViewModel(UserApi(client, AppConfig("http://catalogo", "http://turnos")), session, ErrorMessages())
    }

    @Test
    fun theLoginWarnsOnlyWhenTheSessionExpiredAndNotWhenTheUserClosedIt() {
        session.expire()
        assertEquals(LoginViewModel.SESSION_EXPIRED, viewModel().state.value.notice)

        session.save("jwt")
        session.logout()
        assertNull(viewModel().state.value.notice)
    }

    @Test
    fun theWarningDisappearsWhenTheUserTriesToLogInAgain() {
        session.expire()
        val viewModel = viewModel()
        viewModel.onUsernameChange("prueba")
        viewModel.onPasswordChange("clave")

        viewModel.submit()

        assertNull(viewModel.state.value.notice)
    }
}
