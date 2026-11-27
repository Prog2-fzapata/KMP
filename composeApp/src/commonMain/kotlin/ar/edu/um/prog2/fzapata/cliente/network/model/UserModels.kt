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

// GET /api/account trae más datos (id, idioma, imagen, roles) que la app no usa. El nombre y el apellido son null mientras no se carguen
@Serializable
data class Account(
    val login: String,
    val firstName: String?,
    val lastName: String?,
    val email: String
)

// POST /api/account cambia solo los campos que llegan: Json no escribe los null por defecto, así que lo que quede en null no viaja
// y el servicio lo deja como está
@Serializable
data class UpdateAccountRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
)
