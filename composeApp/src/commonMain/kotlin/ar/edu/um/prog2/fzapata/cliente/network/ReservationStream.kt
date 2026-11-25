package ar.edu.um.prog2.fzapata.cliente.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Lo que le llega a quien mira el stream
sealed interface StreamEvent {
    // La conexión se abrió: un aviso anterior pudo perderse, así que conviene consultar
    data object Connected : StreamEvent

    // Esta reserva cambió: el detalle se pide por REST
    data class Updated(val reservationId: Long) : StreamEvent
}

// Avisos de cambio de las reservas del usuario. Es una optimización y no la fuente de verdad: no hay Last-Event-ID, así que
// quien lo mira consulta la reserva al conectar y ante cada aviso. Recibe cómo abrir una conexión (StreamConnection::connect) y
// un pedido normal para comprobar la sesión: un 401 de ese pedido la vacía (lo hace el cliente HTTP).
class ReservationStream(
    private val connect: () -> Flow<StreamEvent>,
    private val checkSession: suspend () -> Unit,
    private val waits: List<Duration> = RECONNECT_WAITS
) {

    // Si la conexión se corta o falla, comprueba la sesión y la reabre con una espera que crece hasta el último valor de waits.
    // Termina solo cuando la sesión venció (la app vuelve al login) o con una cancelación
    fun events(): Flow<StreamEvent> = flow { // flow es una lista que se llena con el tiempo
        var failures = 0
        while (true) {
            try {
                connect().collect { event ->
                    if (event == StreamEvent.Connected) failures = 0
                    emit(event) // entrega los eventos
                }
            } catch (e: Exception) { // Atrapa cualquier cosa y canRetry elige
                if (!canRetry(e)) throw e
            }
            delay(waits[minOf(failures, waits.lastIndex)])
            failures++
            // Con el token vencido el servidor cierra el stream y rechaza reconectar con un 401 que el plugin no traduce
            if (sessionExpired()) return@flow // el flujo se acabó
        }
    }

    // Solo se reintenta lo que esperar puede arreglar: un corte, un timeout o una respuesta de error del servidor (incluido un 401,
    // cuya sesión se comprueba antes de reconectar). Una cancelación o un error de la app (por ejemplo, un motor sin SSE) no se tapan
    // true IOException, false ApiException
    private fun canRetry(error: Exception): Boolean = error is IOException || error is ApiException

    private suspend fun sessionExpired(): Boolean =
        try {
            checkSession()
            false
        } catch (e: ApiException) {
            e.isUnauthorized()
        } catch (e: IOException) {
            false
        }

    private companion object {
        val RECONNECT_WAITS = listOf(2.seconds, 4.seconds, 8.seconds)
    }
}
