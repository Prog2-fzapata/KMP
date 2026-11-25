package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import ar.edu.um.prog2.fzapata.cliente.network.model.Slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    val error: String? = null,
    // El horario que tocó el usuario y todavía no confirmó, o el que se está reservando
    val selected: Slot? = null,
    // true desde que confirma hasta que llega la respuesta, sirve para ignorar toques y deshabilitar botones
    val reserving: Boolean = false,
    // Un aviso sobre la última reserva; se borra al tocar otro horario
    val notice: String? = null,
    // true cuando responde PATIENT_NAME_REQUIRED
    val needsPatientName: Boolean = false,
    // Cuando llega, la pantalla abre la reserva
    val reservation: Reservation? = null
)

class AvailabilityViewModel(
    private val api: TurnosApi,
    private val professionalId: Long,
    private val messages: ErrorMessages,
    private val starter: ReservationStarter = ReservationStarter(api, messages),
    // Un aviso para mostrar desde el principio, por ejemplo cuando se vuelve de una reserva que venció
    notice: String? = null
) : ViewModel() {

    // Estado inicial con loading = true
    private val _state = MutableStateFlow(AvailabilityState(loading = true, notice = notice))
    val state: StateFlow<AvailabilityState> = _state.asStateFlow()

    // El reloj de Kotlin 2.2 todavía es experimental: se acepta a propósito
    @OptIn(ExperimentalTime::class)
    private val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    // Primer día de la próxima ventana a pedir y último día que se ofrece
    private var windowStart: LocalDate = today
    private val lastDay: LocalDate = today.plus(BOOKING_HORIZON_DAYS - 1, DateTimeUnit.DAY)

    // El pedido de horarios en curso: al recargar la grilla se cancela, para que una respuesta vieja no se mezcle con la nueva
    private var fetchJob: Job? = null

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

    // Tocar un horario pide confirmación
    fun onSlotSelected(slot: Slot) {
        if (_state.value.reserving) return // con una reserva en curso se ignora
        _state.update { it.copy(selected = slot, notice = null) }
    }

    fun onReservationCancelled() {
        if (_state.value.reserving) return
        _state.update { it.copy(selected = null) }
    }

    // Con un pedido en curso se ignora: así un segundo toque no crea otra reserva
    fun confirmReservation() {
        val current = _state.value
        val slot = current.selected ?: return // Si es null, aborta
        if (current.reserving) return // Si ya hay un pedido en curso, aborta
        _state.update { it.copy(reserving = true) }
        startReservation(slot)
    }

    // Con el nombre guardado se repite la misma reserva: PATIENT_NAME_REQUIRED sale antes de que el servicio cree nada
    fun onPatientNameSaved() {
        val slot = _state.value.selected ?: return
        _state.update { it.copy(needsPatientName = false, reserving = true) }
        startReservation(slot)
    }

    fun onPatientNameCancelled() {
        _state.update { it.copy(needsPatientName = false, selected = null) }
    }

    // Pide ventanas hasta encontrar horarios o llegar al último día. El día de inicio solo avanza si la respuesta llegó,
    // así que «Reintentar» sigue donde falló
    private fun fetch() {
        fetchJob = viewModelScope.launch {
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

    // Cierra la reserva con un aviso y vuelve a pedir los horarios desde hoy, todo en un solo cambio de estado
    private fun reloadSlots(notice: String) {
        fetchJob?.cancel()
        windowStart = today
        _state.update {
            it.copy(
                slots = emptyList(),
                hasMore = true,
                loading = true,
                loadingMore = false,
                error = null,
                selected = null,
                reserving = false,
                notice = notice
            )
        }
        fetch()
    }
    // Usa el starter para empezar la reserva y traduce los results
    private fun startReservation(slot: Slot) {
        viewModelScope.launch {
            when (val result = starter.start(professionalId, slot)) {
                is StartResult.Created -> _state.update { it.copy(reservation = result.reservation, reserving = false, selected = null) }
                is StartResult.SlotGone -> reloadSlots("${result.message} Se actualizaron los horarios.")
                StartResult.NeedsName -> _state.update { it.copy(reserving = false, needsPatientName = true) }
                is StartResult.Failed -> _state.update { it.copy(selected = null, reserving = false, notice = result.message) }
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
