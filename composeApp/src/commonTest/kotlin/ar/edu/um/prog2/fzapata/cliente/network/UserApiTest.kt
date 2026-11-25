package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.UpdateAccountRequest
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
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun apiAnswering(body: String, status: HttpStatusCode = HttpStatusCode.OK): UserApi {
        val engine = MockEngine { request ->
            requests += request
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val client = HttpClientFactory(SessionManager(FakeSessionStore())).create(engine)
        return UserApi(client, AppConfig("http://catalogo", "http://turnos"))
    }

    @Test
    fun theAccountComesFromARealResponseWithTheNameStillMissing() = runTest {
        val account = apiAnswering(ACCOUNT_WITHOUT_NAME).account()

        assertEquals("kmp21perfil", account.login)
        assertEquals("kmp21perfil@example.com", account.email)
        assertNull(account.firstName)
        assertNull(account.lastName)
        assertEquals("http://catalogo/api/account", requests.single().url.toString())
        assertEquals(HttpMethod.Get, requests.single().method)
    }

    @Test
    fun onlyTheFieldsGivenTravelSoTheServiceKeepsTheRest() = runTest {
        val api = apiAnswering("")

        api.updateAccount(UpdateAccountRequest(firstName = "Ana", lastName = "Gómez"))
        api.updateAccount(UpdateAccountRequest(email = "ana@example.com"))

        assertEquals(HttpMethod.Post, requests.first().method)
        assertEquals("""{"firstName":"Ana","lastName":"Gómez"}""", (requests[0].body as TextContent).text)
        assertEquals("""{"email":"ana@example.com"}""", (requests[1].body as TextContent).text)
    }

    private companion object {
        // Respuesta real del catálogo para una cuenta recién registrada
        const val ACCOUNT_WITHOUT_NAME = """{"id":28,"login":"kmp21perfil","firstName":null,"lastName":null,"email":"kmp21perfil@example.com","imageUrl":null,"langKey":"es","authorities":["ROLE_USER"]}"""
    }
}
