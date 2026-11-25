package ar.edu.um.prog2.fzapata.cliente.network

import io.ktor.sse.ServerSentEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreamConnectionTest {

    @Test
    fun theUpdateNoticeCarriesTheReservationIdAndAnyOtherEventIsIgnored() {
        val update = ServerSentEvent(data = TurnosResponses.STREAM_UPDATE, event = "reservation-updated", id = "10-1")

        assertEquals(StreamEvent.Updated(10), StreamConnection.eventOf(update))
        assertNull(StreamConnection.eventOf(ServerSentEvent(data = TurnosResponses.STREAM_UPDATE, event = "otro")))
        assertNull(StreamConnection.eventOf(ServerSentEvent(comments = "ping")))
    }
}
