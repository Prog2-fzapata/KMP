package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.Account
import ar.edu.um.prog2.fzapata.cliente.network.model.LoginRequest
import ar.edu.um.prog2.fzapata.cliente.network.model.RegisterRequest
import ar.edu.um.prog2.fzapata.cliente.network.model.TokenResponse
import ar.edu.um.prog2.fzapata.cliente.network.model.UpdateAccountRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

// Un método por endpoint de usuarios del servicio de catálogo (registro, sesión y cuenta); los errores salen como ApiException
class UserApi(private val client: HttpClient, private val config: AppConfig) {

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

    suspend fun account(): Account = client.get("${config.catalogUrl}/api/account").body()

    // 200 sin cuerpo
    suspend fun updateAccount(request: UpdateAccountRequest) {
        client.post("${config.catalogUrl}/api/account") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
