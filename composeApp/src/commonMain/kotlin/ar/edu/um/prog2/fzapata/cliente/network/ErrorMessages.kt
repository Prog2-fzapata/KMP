package ar.edu.um.prog2.fzapata.cliente.network

// Es el traductor entre lo que dice un servicio y lo que ve el usuario
class ErrorMessages {

    // Recibe la excepción y la traduce por su código, si no, default NO_CONNECTION
    fun forError(error: Throwable): String =
        if (error is ApiException) forCode(error.code, error.status) else NO_CONNECTION

    // El code es un conjunto abierto: si no lo conocemos, decide el estado HTTP
    fun forCode(code: String?, status: Int): String = byCode[code] ?: forStatus(status)

    private fun forStatus(status: Int): String = when {
        status == 401 -> "La sesión venció: hay que iniciar sesión de nuevo."
        status == 403 -> "No hay permiso para esta operación."
        status == 404 -> "No se encontró lo pedido."
        status == 409 -> "No se pudo completar la operación por un conflicto."
        status == 503 -> "El servicio no está disponible por ahora."
        status >= 500 -> "El servicio tuvo un error inesperado."
        status in 400..499 -> "La solicitud no es válida."
        else -> GENERIC
    }

    private val byCode = mapOf(
        "VALIDATION_ERROR" to "Hay datos que no son válidos.",
        "BAD_REQUEST" to "La solicitud no es válida.",
        "LOGIN_ALREADY_USED" to "Ese nombre de usuario ya está en uso.",
        "EMAIL_ALREADY_USED" to "Ese email ya está en uso.",
        "UNAUTHORIZED" to "La sesión venció: hay que iniciar sesión de nuevo.",
        "INVALID_CREDENTIALS" to "Usuario o contraseña incorrectos.",
        "FORBIDDEN" to "No hay permiso para esta operación.",
        "USER_NOT_FOUND" to "No se encontró el usuario.",
        "PROFESSIONAL_NOT_FOUND" to "No se encontró el profesional.",
        "RESERVATION_NOT_FOUND" to "No se encontró la reserva.",
        "NOT_FOUND" to "No se encontró lo pedido.",
        "METHOD_NOT_ALLOWED" to "La app envió una solicitud que el servicio no admite.",
        "NOT_ACCEPTABLE" to "La app pidió un formato que el servicio no ofrece.",
        "UNSUPPORTED_MEDIA_TYPE" to "La app envió un formato que el servicio no admite.",
        "REQUEST_ERROR" to "No se pudo procesar la solicitud.",
        "INTERNAL_ERROR" to "El servicio tuvo un error inesperado.",
        "INVALID_DATE_RANGE" to "El rango de fechas no es válido.",
        "PROFESSIONAL_DISABLED" to "El profesional no está habilitado.",
        "INVALID_SLOT" to "Ese horario no es válido.",
        "PATIENT_NAME_REQUIRED" to "Hace falta cargar nombre y apellido para reservar.",
        "SLOT_UNAVAILABLE" to "Ese horario ya no está disponible.",
        "RESERVATION_EXPIRED" to "El tiempo para confirmar la reserva terminó.",
        "RESERVATION_NOT_CANCELLABLE" to "La reserva no se puede cancelar.",
        "INVALID_RESERVATION_STATE" to "La reserva no está en un estado que permita esa operación.",
        "RESERVATION_CONFLICT" to "La reserva se modificó al mismo tiempo: hay que consultar su estado.",
        "CATALOG_UNAVAILABLE" to "El servicio de catálogo no responde.",
        "CATEDRA_UNAVAILABLE" to "El servicio de la cátedra no responde.",
        "DATABASE_UNAVAILABLE" to "El servicio no puede acceder a sus datos en este momento."
    )

    companion object {
        const val NO_CONNECTION = "No se pudo conectar con el servidor: hay que revisar la conexión."
        const val GENERIC = "No se pudo completar la operación."
    }
}
