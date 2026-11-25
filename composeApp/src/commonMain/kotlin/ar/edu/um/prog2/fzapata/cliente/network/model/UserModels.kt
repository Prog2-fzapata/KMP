package ar.edu.um.prog2.fzapata.cliente.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Sin valor por defecto en langKey: Json no escribe los valores por defecto y el contrato pide enviarlo
@Serializable
data class RegisterRequest(
    val login: String,
    val password: String,
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val langKey: String
)

// username es el login o el email
@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
    val rememberMe: Boolean
)

@Serializable
data class TokenResponse(
    @SerialName("id_token") val idToken: String
)

// POST /api/account cambia solo los campos que llegan: con estos dos, el email y el resto quedan como están
@Serializable
data class UpdateNameRequest(
    val firstName: String,
    val lastName: String
)
