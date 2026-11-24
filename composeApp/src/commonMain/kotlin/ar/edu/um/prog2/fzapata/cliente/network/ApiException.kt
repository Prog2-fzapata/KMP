package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.network.model.FieldError

// En caso de error, convierte el error a ApiException, hereda de RuntimeException
class ApiException(
    val status: Int,
    val code: String?,
    val fieldErrors: List<FieldError> = emptyList()
) : RuntimeException("HTTP $status ${code.orEmpty()}".trim()) {

    // Solo UNAUTHORIZED es un token inválido o vencido; INVALID_CREDENTIALS es una clave incorrecta al iniciar sesión
    fun isUnauthorized(): Boolean = status == 401 && code == UNAUTHORIZED

    companion object {
        const val UNAUTHORIZED = "UNAUTHORIZED"
    }
}
