package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.ApiException
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class ReservationDetailState(
    // La última lectura de la reserva; null hasta que llega la primera
    val reservation: Reservation? = null,
    val today: LocalDate,
    // Un error de la primera lectura, con «Reintentar»
    val error: String? = null,
    // El diálogo de confirmación está abierto, con el motivo que se escribió
    val asking: Boolean = false,
    val reason: String = "",
    val cancelling: Boolean = false,
    // Un error de la cancelación, dentro del diálogo
    val cancelError: String? = null,
    // Lo que pasó cuando el diálogo se cerró sin cancelar, por ejemplo que la reserva ya no se puede cancelar
    val notice: String? = null,
    // La reserva ya no existe: la pantalla vuelve a la lista
    val gone: Boolean = false
) {
    // Solo se cancela una confirmada, y no una de un día que ya pasó
    val canCancel: Boolean
        get() = reservation?.let { ReservationStep.of(it) == ReservationStep.CONFIRMED && LocalDate.parse(it.appointmentDate) >= today } == true
}

// Una reserva de la lista, con la opción de cancelarla si está confirmada
@OptIn(ExperimentalTime::class)
class ReservationDetailViewModel(
    private val api: TurnosApi,
    private val reservationId: Long,
    private val messages: ErrorMessages,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationDetailState(today = today))
    val state: StateFlow<ReservationDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        _state.update { it.copy(error = null) }
        load()
    }

    fun onCancelClick() = _state.update { it.copy(asking = true, cancelError = null, notice = null) }

    // El campo admite hasta lo que admite el servicio: no hay un error de largo
    fun onReasonChange(reason: String) = _state.update { it.copy(reason = reason.take(MAX_REASON), cancelError = null) }

    // Cerrar el diálogo descarta el motivo; con una cancelación en curso se ignora
    fun onDismiss() {
        if (_state.value.cancelling) return
        _state.update { it.copy(asking = false, reason = "", cancelError = null) }
    }

    fun confirmCancel() {
        val current = _state.value
        if (current.cancelling || !current.asking) return

        _state.update { it.copy(cancelling = true, cancelError = null) }
        viewModelScope.launch {
            try {
                close(api.cancel(reservationId, current.reason.trim().ifEmpty { null }))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onCancelFailed(e)
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(reservation = api.reservation(reservationId)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if ((e as? ApiException)?.code == RESERVATION_NOT_FOUND) _state.update { it.copy(gone = true) }
                else _state.update { it.copy(error = messages.forError(e)) }
            }
        }
    }

    // Guarda la reserva y cierra el diálogo
    private fun close(reservation: Reservation) =
        _state.update { it.copy(reservation = reservation, asking = false, reason = "", cancelling = false, cancelError = null) }

    private suspend fun onCancelFailed(error: Exception) {
        val code = (error as? ApiException)?.code
        when {
            code == RESERVATION_NOT_FOUND -> _state.update { it.copy(cancelling = false, gone = true) }
            // La reserva ya no está confirmada: se muestra como está y el diálogo se cierra
            code == RESERVATION_NOT_CANCELLABLE -> {
                val read = readOrNull()
                _state.update {
                    it.copy(reservation = read ?: it.reservation, asking = false, reason = "", cancelling = false, notice = messages.forError(error))
                }
            }
            // Con un conflicto o un corte la cancelación pudo llegar: se lee la reserva antes de dejar que repita
            else -> {
                val read = if (mayHaveChanged(error)) readOrNull() else null
                if (read?.status == CANCELLED) close(read)
                else _state.update { it.copy(cancelling = false, cancelError = messages.forError(error)) }
            }
        }
    }

    private suspend fun readOrNull(): Reservation? =
        try {
            api.reservation(reservationId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }

    // Un 503 no cambia la reserva (el contrato lo dice); un conflicto o un corte, sí pudo cambiarla
    private fun mayHaveChanged(error: Exception) = error !is ApiException || error.code == RESERVATION_CONFLICT

    companion object {
        const val MAX_REASON = 500
        private const val CANCELLED = "CANCELLED"
        private const val RESERVATION_NOT_FOUND = "RESERVATION_NOT_FOUND"
        private const val RESERVATION_NOT_CANCELLABLE = "RESERVATION_NOT_CANCELLABLE"
        private const val RESERVATION_CONFLICT = "RESERVATION_CONFLICT"
    }
}
