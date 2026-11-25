package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.ReservationUpdate
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.timeout
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Una sola conexión al stream de reservas: termina cuando el servidor la cierra (por ejemplo, token vencido) y lanza si falla
class StreamConnection(private val client: HttpClient, private val config: AppConfig) {

    // Avisa que la conexión se abrió y después cada reserva que cambió; los :ping no llegan porque el plugin oculta los comentarios
    fun connect(): Flow<StreamEvent> = flow {
        try {
            client.sse( // abre la conexión
                "${config.turnosUrl}/api/reservations/stream",
                request = {
                    // El stream no tiene fin: el servidor manda un :ping cada 25 segundos y el tope general de 15 lo cortaría
                    timeout {
                        requestTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                        socketTimeoutMillis = HttpTimeoutConfig.INFINITE_TIMEOUT_MS
                    }
                }
            ) {
                emit(StreamEvent.Connected)
                incoming.collect { sse -> eventOf(sse)?.let { emit(it) } }
            }
        } catch (e: SSEClientException) {
            // El plugin envuelve cualquier error (un 401, un corte) en una SSEClientException: se desenvuelve para que el resto de la app
            // vea el ApiException o el corte de siempre. Sin causa es un error del plugin o del servidor (motor sin SSE, tipo de contenido)
            throw e.cause ?: e
        }
    }

    companion object {
        private const val UPDATED_EVENT = "reservation-updated"

        // Solo importa el aviso de cambio; cualquier otro evento se ignora
        internal fun eventOf(sse: ServerSentEvent): StreamEvent? {
            val data = sse.data ?: return null
            if (sse.event != UPDATED_EVENT) return null
            return StreamEvent.Updated(appJson.decodeFromString<ReservationUpdate>(data).id)
        }
    }
}
