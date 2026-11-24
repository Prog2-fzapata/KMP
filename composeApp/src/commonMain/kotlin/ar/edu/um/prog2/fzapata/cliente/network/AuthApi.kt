package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.LoginRequest
import ar.edu.um.prog2.fzapata.cliente.network.model.RegisterRequest
import ar.edu.um.prog2.fzapata.cliente.network.model.TokenResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

// Un método por endpoint de usuarios del catálogo; los errores salen como ApiException
class AuthApi(private val client: HttpClient, private val config: AppConfig) {

    // 201 sin cuerpo
    suspend fun register(request: RegisterRequest) {
        client.post("${config.catalogUrl}/api/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // Devuelve el JWT
    suspend fun authenticate(request: LoginRequest): String =
        client.post("${config.catalogUrl}/api/authenticate") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<TokenResponse>().idToken
}
