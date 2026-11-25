package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.network.UserApi
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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    // El viewModelScope corre en el hilo principal, que en las pruebas no existe: se reemplaza por uno de prueba
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val requests = mutableListOf<HttpRequestData>()
    private var accountStatus = HttpStatusCode.OK
    private var saveStatus = HttpStatusCode.OK
    private var accountBody = ACCOUNT_WITHOUT_NAME
    private var saveBody = ""

    // El GET devuelve la cuenta (o el error que se haya fijado) y el POST responde lo que se haya fijado
    private fun newViewModel(): ProfileViewModel {
        val engine = MockEngine.create {
            // Sin otro hilo la respuesta llega en el momento: el estado ya está actualizado al comprobarlo
            dispatcher = Dispatchers.Unconfined
            addHandler { request ->
                requests += request
                if (request.method == HttpMethod.Get) {
                    respond(if (accountStatus == HttpStatusCode.OK) accountBody else "{}", accountStatus, jsonHeaders)
                } else {
                    respond(saveBody, saveStatus, jsonHeaders)
                }
            }
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return ProfileViewModel(UserApi(client, AppConfig("http://catalogo", "http://turnos")), ErrorMessages())
    }

    private fun ProfileViewModel.fillIn(firstName: String, lastName: String, email: String) {
        onFirstNameChange(firstName)
        onLastNameChange(lastName)
        onEmailChange(email)
    }

    @Test
    fun theAccountFillsTheFormAndAMissingNameIsFlagged() {
        val state = newViewModel().state.value

        assertFalse(state.loading)
        assertEquals("kmp21perfil", state.saved?.login)
        assertEquals("kmp21perfil@example.com", state.email)
        assertEquals("", state.firstName)
        assertTrue(state.missingName)
    }

    @Test
    fun aLastNameAloneMissingIsFlaggedToo() {
        accountBody = ACCOUNT_WITHOUT_NAME.replace(""""firstName":null""", """"firstName":"Ana"""")

        assertTrue(newViewModel().state.value.missingName)
    }

    @Test
    fun ifTheAccountCannotBeReadItShowsTheErrorAndRetryReadsItAgain() {
        accountStatus = HttpStatusCode.ServiceUnavailable
        val viewModel = newViewModel()
        assertNotNull(viewModel.state.value.loadError)
        assertFalse(viewModel.state.value.loading)

        accountStatus = HttpStatusCode.OK
        viewModel.retry()

        assertNull(viewModel.state.value.loadError)
        assertEquals("kmp21perfil", viewModel.state.value.saved?.login)
    }

    @Test
    fun invalidDataIsNotSentAndEachFieldShowsItsError() {
        val viewModel = newViewModel()
        viewModel.fillIn("A", "Gómez", "sin-arroba")

        viewModel.save()

        assertEquals(setOf("firstName", "email"), viewModel.state.value.fieldErrors.keys)
        assertEquals(1, requests.size) // solo el GET de la cuenta

        // Al escribir, los errores se borran: se calculan de nuevo al guardar
        viewModel.onFirstNameChange("Ana")
        assertTrue(viewModel.state.value.fieldErrors.isEmpty())
    }

    @Test
    fun savingSendsTheThreeFieldsWithoutSpacesAndTheMissingNameFlagGoesAway() {
        val viewModel = newViewModel()
        viewModel.fillIn("  Ana ", "Gómez", " ana@example.com ")

        viewModel.save()

        assertEquals("""{"firstName":"Ana","lastName":"Gómez","email":"ana@example.com"}""", (requests.last().body as TextContent).text)
        val state = viewModel.state.value
        assertTrue(state.justSaved)
        assertFalse(state.missingName)
        assertEquals("Ana", state.firstName)
        assertEquals("ana@example.com", state.saved?.email)
    }

    @Test
    fun anEmailUsedByAnotherUserShowsUnderTheEmailAndNothingIsSaved() {
        saveStatus = HttpStatusCode.BadRequest
        saveBody = """{"detail":"El email ya está en uso","instance":"/api/account","status":400,"title":"Bad Request","path":"/api/account","code":"EMAIL_ALREADY_USED"}"""
        val viewModel = newViewModel()
        viewModel.fillIn("Ana", "Gómez", "kmp21otro@example.com")

        viewModel.save()

        val state = viewModel.state.value
        assertEquals("Ese email ya está en uso.", state.fieldErrors["email"])
        assertNull(state.error)
        assertFalse(state.justSaved)
        assertTrue(state.missingName)
    }

    @Test
    fun aFailureThatIsNotOfAFieldShowsAsAMessageAndNothingIsSaved() {
        saveStatus = HttpStatusCode.ServiceUnavailable
        val viewModel = newViewModel()
        viewModel.fillIn("Ana", "Gómez", "ana@example.com")

        viewModel.save()

        val state = viewModel.state.value
        assertNotNull(state.error)
        assertTrue(state.fieldErrors.isEmpty())
        assertFalse(state.justSaved)
        assertFalse(state.saving)
    }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

        // Respuesta real del catálogo para una cuenta recién registrada
        const val ACCOUNT_WITHOUT_NAME = """{"id":28,"login":"kmp21perfil","firstName":null,"lastName":null,"email":"kmp21perfil@example.com","imageUrl":null,"langKey":"es","authorities":["ROLE_USER"]}"""
    }
}
