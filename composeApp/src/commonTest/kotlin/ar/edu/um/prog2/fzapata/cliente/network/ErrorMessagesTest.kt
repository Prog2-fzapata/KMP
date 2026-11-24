package ar.edu.um.prog2.fzapata.cliente.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ErrorMessagesTest {

    private val messages = ErrorMessages()

    // Los code de las tablas de errores de docs/contrato.md de catálogo y turnos
    private val documentedCodes = listOf(
        "VALIDATION_ERROR", "BAD_REQUEST", "LOGIN_ALREADY_USED", "EMAIL_ALREADY_USED", "UNAUTHORIZED",
        "INVALID_CREDENTIALS", "FORBIDDEN", "USER_NOT_FOUND", "PROFESSIONAL_NOT_FOUND", "RESERVATION_NOT_FOUND",
        "NOT_FOUND", "METHOD_NOT_ALLOWED", "NOT_ACCEPTABLE", "UNSUPPORTED_MEDIA_TYPE", "REQUEST_ERROR",
        "INTERNAL_ERROR", "INVALID_DATE_RANGE", "PROFESSIONAL_DISABLED", "INVALID_SLOT", "PATIENT_NAME_REQUIRED",
        "SLOT_UNAVAILABLE", "RESERVATION_EXPIRED", "RESERVATION_NOT_CANCELLABLE", "INVALID_RESERVATION_STATE",
        "RESERVATION_CONFLICT", "CATALOG_UNAVAILABLE", "CATEDRA_UNAVAILABLE", "DATABASE_UNAVAILABLE"
    )

    @Test
    fun everyDocumentedCodeHasItsOwnMessage() {
        documentedCodes.forEach { code ->
            // Con un estado que no existe, el único mensaje posible es el del propio code
            assertNotEquals(ErrorMessages.GENERIC, messages.forCode(code, 0), code)
        }
    }

    @Test
    fun anUnknownCodeFallsBackToTheHttpStatus() {
        assertEquals("El servicio no está disponible por ahora.", messages.forCode("CODIGO_NUEVO", 503))
        assertEquals("No se encontró lo pedido.", messages.forCode(null, 404))
    }
}
