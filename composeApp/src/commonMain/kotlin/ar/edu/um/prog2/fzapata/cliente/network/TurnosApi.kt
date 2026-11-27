package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.Availability
import ar.edu.um.prog2.fzapata.cliente.network.model.CancelReservationRequest
import ar.edu.um.prog2.fzapata.cliente.network.model.Page
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import ar.edu.um.prog2.fzapata.cliente.network.model.SendPhoneRequest
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import ar.edu.um.prog2.fzapata.cliente.network.model.StartReservationRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.datetime.LocalDate

// Un método por endpoint del servicio de turnos; los errores salen como ApiException
class TurnosApi(private val client: HttpClient, private val config: AppConfig) {

    // Los horarios libres entre dos días, ambos incluidos; el contrato no promete el orden, así que se ordena acá
    suspend fun availability(professionalId: Long, from: LocalDate, to: LocalDate): List<Slot> =
        client.get("${config.turnosUrl}/api/availability") {
            parameter("professionalId", professionalId)
            parameter("from", from)
            parameter("to", to)
        }.body<Availability>().slots.sortedWith(compareBy({ it.date }, { it.startTime }))

    // 201 con la reserva esperando el teléfono
    suspend fun startReservation(request: StartReservationRequest): Reservation =
        client.post("${config.turnosUrl}/api/reservations") {
            contentType(ContentType.Application.Json)
            setBody(request)
            // Si la cátedra no responde, turnos puede tardar más que el tiempo general; cortar antes dejaría una reserva sin que la app lo sepa
            timeout { requestTimeoutMillis = START_RESERVATION_TIMEOUT_MS }
        }.body()

    // Una página de las reservas del usuario, de la más lejana a la más antigua. Sin status no trae las vencidas ni las fallidas;
    // con status trae exactamente las de ese estado
    suspend fun reservations(status: String? = null, page: Int = 0): Page<Reservation> =
        client.get("${config.turnosUrl}/api/reservations") {
            parameter("page", page)
            parameter("size", RESERVATION_PAGE_SIZE)
            status?.let { parameter("status", it) }
        }.toPage(page, RESERVATION_PAGE_SIZE)

    // Una reserva ajena responde 404 igual que una inexistente
    suspend fun reservation(id: Long): Reservation = client.get("${config.turnosUrl}/api/reservations/$id").body()

    // 202 con la reserva en PHONE_QUEUED: la cátedra la confirma o la rechaza después
    suspend fun sendPhone(id: Long, phoneNumber: String): Reservation =
        client.post("${config.turnosUrl}/api/reservations/$id/phone") {
            contentType(ContentType.Application.Json)
            setBody(SendPhoneRequest(phoneNumber))
        }.body()

    // 200 con la reserva en CANCELLED; solo se cancela una CONFIRMED. Sin motivo se envía sin cuerpo
    suspend fun cancel(id: Long, reason: String?): Reservation =
        client.post("${config.turnosUrl}/api/reservations/$id/cancel") {
            reason?.let {
                contentType(ContentType.Application.Json)
                setBody(CancelReservationRequest(it))
            }
        }.body()

    companion object {
        const val RESERVATION_PAGE_SIZE = 20
        private const val START_RESERVATION_TIMEOUT_MS = 45_000L
    }
}
