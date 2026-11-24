package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.session.FakeSessionStore
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HttpClientFactoryTest {

    private val url = "http://catalogo/api/account"
    private val problemJson = headersOf(HttpHeaders.ContentType, "application/problem+json;charset=UTF-8")

    private fun sessionWith(token: String?) = SessionManager(FakeSessionStore(token))

    private fun clientAnswering(session: SessionManager, status: HttpStatusCode, body: String): HttpClient =
        HttpClientFactory(session).create(MockEngine { respond(body, status, problemJson) })

    @Test
    fun aProblemDetailBecomesAnApiExceptionWithItsCodeAndFieldErrors() = runTest {
        val client = clientAnswering(sessionWith(null), HttpStatusCode.BadRequest, VALIDATION_ERROR)

        val error = assertFailsWith<ApiException> { client.get(url) }

        assertEquals(400, error.status)
        assertEquals("VALIDATION_ERROR", error.code)
        assertEquals(listOf("email", "password", "login"), error.fieldErrors.map { it.field })
    }

    @Test
    fun aResponseWithoutProblemDetailKeepsTheStatus() = runTest {
        val client = clientAnswering(sessionWith(null), HttpStatusCode.BadGateway, "")

        val error = assertFailsWith<ApiException> { client.get(url) }

        assertEquals(502, error.status)
        assertNull(error.code)
    }

    @Test
    fun unauthorizedWithATokenExpiresTheSession() = runTest {
        val session = sessionWith("jwt-vencido")
        val client = clientAnswering(session, HttpStatusCode.Unauthorized, UNAUTHORIZED)

        assertFailsWith<ApiException> { client.get(url) }

        assertNull(session.token())
    }

    @Test
    fun invalidCredentialsDoesNotExpireTheSession() = runTest {
        val session = sessionWith("jwt")
        val client = clientAnswering(session, HttpStatusCode.Unauthorized, INVALID_CREDENTIALS)

        assertFailsWith<ApiException> { client.get(url) }

        assertEquals("jwt", session.token())
    }

    @Test
    fun theTokenIsSentOnlyWhenThereIsASession() = runTest {
        val sent = mutableListOf<String?>()
        val session = sessionWith(null)
        val client = HttpClientFactory(session).create(MockEngine { request ->
            sent.add(request.headers[HttpHeaders.Authorization])
            respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        })

        client.get(url)
        session.save("jwt-nuevo")
        client.get(url)

        assertEquals(listOf(null, "Bearer jwt-nuevo"), sent)
    }

    // Respuestas reales de catálogo (2026-11-24), con su tipo de contenido application/problem+json
    private companion object {
        const val VALIDATION_ERROR = """{"detail":"Los datos enviados no son válidos","instance":"/api/register","status":400,"title":"Bad Request","path":"/api/register","code":"VALIDATION_ERROR","fieldErrors":[{"objectName":"userRegistrationRequest","field":"email","message":"El email no es válido"},{"objectName":"userRegistrationRequest","field":"password","message":"La contraseña debe tener entre 4 y 100 caracteres"},{"objectName":"userRegistrationRequest","field":"login","message":"El login debe tener entre 3 y 50 caracteres"}]}"""
        const val INVALID_CREDENTIALS = """{"detail":"Credenciales inválidas","instance":"/api/authenticate","status":401,"title":"Unauthorized","path":"/api/authenticate","code":"INVALID_CREDENTIALS"}"""
        const val UNAUTHORIZED = """{"detail":"Falta el token de autenticación o no es válido","status":401,"title":"Unauthorized","path":"/api/account","code":"UNAUTHORIZED"}"""
    }
}
