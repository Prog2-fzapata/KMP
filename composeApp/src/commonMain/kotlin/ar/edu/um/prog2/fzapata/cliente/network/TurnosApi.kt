package ar.edu.um.prog2.fzapata.cliente.network

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.model.Availability
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
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
}
