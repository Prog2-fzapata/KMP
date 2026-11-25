package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class AvailabilityState(
    val slots: List<Slot> = emptyList(),
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    // Deja de haber más al llegar al último día que se ofrece
    val hasMore: Boolean = true,
    val error: String? = null
)

class AvailabilityViewModel(
    private val professionalId: Long,
    private val api: TurnosApi,
    private val messages: ErrorMessages
) : ViewModel() {

    // Estado inicial con loading = true
    private val _state = MutableStateFlow(AvailabilityState(loading = true))
    val state: StateFlow<AvailabilityState> = _state.asStateFlow()

    // El reloj de Kotlin 2.2 todavía es experimental: se acepta a propósito
    @OptIn(ExperimentalTime::class)
    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Primer día de la próxima ventana a pedir y último día que se ofrece
    private var windowStart: LocalDate = today
    private val lastDay: LocalDate = today.plus(BOOKING_HORIZON_DAYS - 1, DateTimeUnit.DAY)

    // Al crearse el ViewModel hace fetch
    init {
        fetch()
    }

    // La pantalla lo pide al llegar al final de la lista. Con un error pendiente no pide solo: espera «Reintentar»
    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.error != null || !current.hasMore) return
        _state.update { it.copy(loadingMore = true) }
        fetch()
    }

    fun retry() {
        _state.update { if (it.slots.isEmpty()) it.copy(loading = true, error = null) else it.copy(loadingMore = true, error = null) }
        fetch()
    }

    // Pide ventanas hasta encontrar horarios o llegar al último día. El día de inicio solo avanza si la respuesta llegó,
    // así que «Reintentar» sigue donde falló
    private fun fetch() {
        viewModelScope.launch {
            try {
                var slots = emptyList<Slot>()
                while (slots.isEmpty() && windowStart <= lastDay) {
                    val to = minOf(windowStart.plus(WINDOW_DAYS - 1, DateTimeUnit.DAY), lastDay)
                    slots = api.availability(professionalId, windowStart, to)
                    // Actualiza windowStart para la próxima pasada
                    windowStart = to.plus(1, DateTimeUnit.DAY)
                }
                _state.update {
                    it.copy(slots = it.slots + slots, loading = false, loadingMore = false, hasMore = windowStart <= lastDay)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadingMore = false, error = messages.forError(e)) }
            }
        }
    }

    companion object {
        // Cada pedido cubre 30 días; el servicio admite hasta 31 contando ambos extremos
        const val WINDOW_DAYS = 30

        // Hasta dónde se ofrecen turnos: el servicio no tiene tope y para un profesional con agenda siempre hay más
        const val BOOKING_HORIZON_DAYS = 365
    }
}
