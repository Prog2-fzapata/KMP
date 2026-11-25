package ar.edu.um.prog2.fzapata.cliente.viewmodel

import ar.edu.um.prog2.fzapata.cliente.network.ApiException
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import ar.edu.um.prog2.fzapata.cliente.network.model.StartReservationRequest
import kotlinx.coroutines.CancellationException

// Qué pasó al intentar reservar un horario; el ViewModel lo traduce a su estado
sealed interface StartResult {
    data class Created(val reservation: Reservation) : StartResult

    // El horario ya no sirve: hay que volver a pedir los horarios
    data class SlotGone(val message: String) : StartResult

    data object NeedsName : StartResult

    data class Failed(val message: String) : StartResult
}

// Inicia una reserva y decide qué significa cada respuesta del servicio, sin guardar estado
class ReservationStarter(
    private val api: TurnosApi,
    private val messages: ErrorMessages
) {


    suspend fun start(professionalId: Long, slot: Slot): StartResult =
        try {
            StartResult.Created(api.startReservation(StartReservationRequest(professionalId, slot.date, slot.startTime)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            resultOf(e, professionalId, slot)
        }

    // Cada código tiene su camino; el resto muestra su mensaje
    private suspend fun resultOf(error: Exception, professionalId: Long, slot: Slot): StartResult {
        val code = (error as? ApiException)?.code
        return when {
            code == PATIENT_NAME_REQUIRED -> StartResult.NeedsName
            code == SLOT_UNAVAILABLE || code == INVALID_SLOT -> StartResult.SlotGone(messages.forError(error))
            mayHaveBeenCreated(error) -> findExistingReservation(error, professionalId, slot)
            else -> StartResult.Failed(messages.forError(error))
        }
    }

    // Con un conflicto, un corte o un servicio caído a mitad del pedido, la reserva puede existir aunque la respuesta no llegó
    private fun mayHaveBeenCreated(error: Exception) =
        error !is ApiException || error.code == RESERVATION_CONFLICT || error.status == SERVICE_UNAVAILABLE

    // Antes de dejar que el usuario repita se mira si ya hay una reserva de ese horario; si no hay, se avisa con el error original
    private suspend fun findExistingReservation(error: Exception, professionalId: Long, slot: Slot): StartResult {
        val existing = try {
            api.reservations().firstOrNull { isFor(it, professionalId, slot) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return if (existing != null) StartResult.Created(existing) else StartResult.Failed(messages.forError(error))
    }

    // La lista trae también las canceladas: una reserva cancelada antes del mismo horario no cuenta
    private fun isFor(reservation: Reservation, professionalId: Long, slot: Slot) =
        reservation.professionalId == professionalId &&
            reservation.appointmentDate == slot.date &&
            reservation.startTime == slot.startTime &&
            reservation.status != CANCELLED

    private companion object {
        const val PATIENT_NAME_REQUIRED = "PATIENT_NAME_REQUIRED"
        const val SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE"
        const val INVALID_SLOT = "INVALID_SLOT"
        const val RESERVATION_CONFLICT = "RESERVATION_CONFLICT"
        const val CANCELLED = "CANCELLED"
        const val SERVICE_UNAVAILABLE = 503
    }
}
