package ar.edu.um.prog2.fzapata.cliente.viewmodel.validation

import ar.edu.um.prog2.fzapata.cliente.network.ApiException
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages

// Errores del servicio que corresponden a un campo del formulario; vacío si no son de un campo
internal fun fieldErrorsOf(error: Throwable, messages: ErrorMessages): Map<String, String> {
    if (error !is ApiException) return emptyMap()
    return when (error.code) {
        "LOGIN_ALREADY_USED" -> mapOf("login" to messages.forError(error))
        "EMAIL_ALREADY_USED" -> mapOf("email" to messages.forError(error))
        else -> error.fieldErrors.associate { it.field to it.message }
    }
}
