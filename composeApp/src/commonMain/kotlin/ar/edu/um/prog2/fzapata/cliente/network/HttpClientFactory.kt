package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.network.model.ProblemDetail
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class HttpClientFactory(private val session: SessionManager) {

    private val json = Json { ignoreUnknownKeys = true }

    // El motor se puede reemplazar en las pruebas por uno falso
    fun create(engine: HttpClientEngine = CIO.create()): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        // Se evalúa en cada pedido: el token sale de la sesión del momento
        defaultRequest {
            session.token()?.let { bearerAuth(it) }
        }
        HttpResponseValidator {
            validateResponse { response ->
                if (!response.status.isSuccess()) throw toApiException(response)
            }
        }
    }

    private suspend fun toApiException(response: HttpResponse): ApiException {

        // Sin cuerpo, o con uno que no es un problem+json, queda solo el estado HTTP
        val problem = runCatching { response.body<ProblemDetail>() }.getOrNull()

        val error = ApiException(response.status.value, problem?.code, problem?.fieldErrors.orEmpty())

        // Un UNAUTHORIZED a un pedido sin token no es una sesión vencida: no hay sesión que vaciar
        if (error.isUnauthorized() && response.request.headers.contains(HttpHeaders.Authorization)) {
            session.expire()
        }
        return error
    }

    companion object {
        const val CONNECT_TIMEOUT_MS = 10_000L
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
