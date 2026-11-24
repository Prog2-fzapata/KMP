package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.network.model.RegisterRequest

// Lo que escribe el usuario, tal cual
data class RegisterForm(
    val login: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = ""
) {
    // Los opcionales vacíos viajan como null; el servicio no recibe espacios de más
    fun toRequest() = RegisterRequest(
        login = login.trim(),
        password = password,
        firstName = firstName.trim().ifBlank { null },
        lastName = lastName.trim().ifBlank { null },
        email = email.trim(),
        langKey = LANG_KEY
    )

    companion object {
        // Los mensajes del servicio están siempre en español: no hay selector de idioma
        const val LANG_KEY = "es"
    }
}
