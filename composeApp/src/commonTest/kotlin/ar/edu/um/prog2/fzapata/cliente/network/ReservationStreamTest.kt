package ar.edu.um.prog2.fzapata.cliente.network

import io.ktor.client.plugins.sse.SSEClientException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReservationStreamTest {

    private var attempts = 0
    private var checks = 0

    // La conexión se abre y avisa un cambio, y después el servidor la cierra
    private val ending: () -> Flow<StreamEvent> = { flowOf(StreamEvent.Connected, StreamEvent.Updated(10)) }

    private fun failing(error: Exception): () -> Flow<StreamEvent> = { flow { throw error } }

    // Cada intento de conexión hace lo que le toca, en orden; con el último se repite
    private fun streamOf(vararg connections: () -> Flow<StreamEvent>, checkSession: suspend () -> Unit = { checks++ }) =
        ReservationStream({ connections[minOf(attempts++, connections.lastIndex)]() }, checkSession)

    @Test
    fun itChecksTheSessionAndReconnectsWithGrowingWaitsKeepingTheLastOne() = runTest {
        val stream = streamOf(
            ending,
            failing(IOException("corte de red")),
            failing(ApiException(503, "CATEDRA_UNAVAILABLE", emptyList())),
            failing(IOException("corte de red")),
            failing(ApiException(401, null, emptyList())),
            ending
        )

        val events = stream.events().take(6).toList()

        assertEquals(List(3) { listOf(StreamEvent.Connected, StreamEvent.Updated(10)) }.flatten(), events)
        // Esperas de 2 (tras cerrarse), 4, 8, 8 y 8 segundos: la espera no pasa de 8. Al reconectar bien vuelve a empezar: otros 2
        assertEquals(32_000, testScheduler.currentTime)
        assertEquals(7, attempts)
        assertEquals(6, checks)
    }

    @Test
    fun anExpiredSessionEndsTheStreamInsteadOfReconnecting() = runTest {
        val stream = streamOf(failing(IOException("corte de red")), checkSession = { throw ApiException(401, "UNAUTHORIZED", emptyList()) })

        assertEquals(emptyList(), stream.events().toList())

        assertEquals(1, attempts)
    }

    @Test
    fun aFailureOfTheAppIsNotHiddenByTheRetries() = runTest {
        // Un motor sin SSE falla sin respuesta del servidor: es un error de la app y tiene que verse
        listOf<Exception>(SSEClientException(message = "Engine doesn't support SSECapability"), IllegalStateException("error de la app")).forEach { error ->
            attempts = 0

            assertFailsWith<Exception> { streamOf(failing(error)).events().collect() }

            assertEquals(1, attempts, error.toString())
        }
    }
}
