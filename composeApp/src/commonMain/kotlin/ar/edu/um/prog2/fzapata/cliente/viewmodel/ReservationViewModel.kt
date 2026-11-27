package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.ApiException
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.ReservationStream
import ar.edu.um.prog2.fzapata.cliente.network.StreamEvent
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.PhoneValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

// Adónde ir al salir: a los horarios del profesional, con un aviso si hace falta
data class Leave(
    val notice: String?
)

data class ReservationState(
    // La última lectura de la reserva; null hasta que llega la primera
    val reservation: Reservation? = null,
    // Un error de la primera lectura, con «Reintentar»
    val error: String? = null,
    val phone: String = "",
    // Un error del envío o el rechazo de la cátedra, debajo del campo
    val phoneError: String? = null,
    // true desde que se envía el teléfono hasta que llega la respuesta, sirve para deshabilitar los botones
    val sending: Boolean = false,
    // Pasó el plazo de espera sin novedades de la cátedra
    val slow: Boolean = false,
    // Cuando llega, la pantalla vuelve a los horarios
    val leave: Leave? = null
) {
    // En qué paso está a partir del estado de la reserva
    val step: ReservationStep? get() = reservation?.let(ReservationStep::of)

    // Confirmada o terminada: ya no hace falta escuchar cambios
    val finished: Boolean get() = step?.finished == true
}

// Sigue una reserva hasta que se confirma: espera el pedido de teléfono de la cátedra, lo envía y espera el resultado
class ReservationViewModel(
    private val api: TurnosApi,
    private val reservationId: Long,
    private val stream: ReservationStream,
    private val messages: ErrorMessages,
    private val validator: PhoneValidator = PhoneValidator()
) : ViewModel() {

    private val _state = MutableStateFlow(ReservationState())
    val state: StateFlow<ReservationState> = _state.asStateFlow()

    // La lectura en curso: ante un aviso nuevo se cancela la anterior, para que una respuesta vieja no pise a una nueva
    private var refreshJob: Job? = null

    // El plazo de espera: si pasa sin que la cátedra haga lo que se espera, la pantalla ofrece «Reintentar»
    private var deadlineJob: Job? = null

    init {
        refresh()
    }

    // La pantalla lo corre mientras está abierta: cada aviso de esta reserva, y cada reconexión, vuelve a leerla
    suspend fun watch() {
        stream.events().collect { event ->
            when (event) {
                // Con una lectura en curso (la primera, por ejemplo) no hace falta otra
                StreamEvent.Connected -> if (refreshJob?.isActive != true) refresh()
                is StreamEvent.Updated -> if (event.reservationId == reservationId) refresh()
            }
        }
    }

    // Actualiza el estado del teléfono a medida que escribe para verlo en el campo
    fun onPhoneChange(phone: String) = _state.update { it.copy(phone = phone, phoneError = null) }

    // Con un envío en curso se ignora, y no se envía antes de que la cátedra lo pida: el servicio lo rechazaría
    fun sendPhone() {
        val current = _state.value
        val reservation = current.reservation ?: return
        if (current.sending || ReservationStep.of(reservation) != ReservationStep.ASKING_PHONE) return

        val errors = validator.validate(current.phone)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(phoneError = errors["phoneNumber"]) }
            return
        }

        _state.update { it.copy(sending = true, phoneError = null) }
        viewModelScope.launch {
            try {
                val queued = api.sendPhone(reservationId, validator.normalize(current.phone))
                // La respuesta del envío es lo último que se sabe: una lectura anterior todavía en vuelo no debe pisarla
                refreshJob?.cancel()
                _state.update { it.copy(sending = false) }
                apply(queued)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onSendFailed(e)
            }
        }
    }

    // Cancelar el ingreso vuelve a los horarios; el bloqueo vence solo
    fun onPhoneCancelled() {
        if (_state.value.sending) return
        _state.update { it.copy(leave = Leave(notice = null)) }
    }

    // Vuelve a leer la reserva y empieza de nuevo el plazo; nunca reenvía el teléfono
    fun retry() {
        _state.update { it.copy(error = null, slow = false) }
        if (_state.value.reservation != null) startDeadline()
        refresh()
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                apply(api.reservation(reservationId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Con la reserva ya leída la pantalla sigue con lo último que se sabe: si no hay novedades, lo avisa el plazo de espera
                if (_state.value.reservation == null) _state.update { it.copy(error = messages.forError(e)) }
            }
        }
    }

    // Guarda la lectura. Si estaba esperando el resultado y vuelve a pedir el teléfono, la cátedra rechazó el número
    private fun apply(reservation: Reservation) {
        val previous = _state.value.reservation?.let(ReservationStep::of)
        val step = ReservationStep.of(reservation)
        val rejected = previous == ReservationStep.WAITING_FOR_RESULT && step == ReservationStep.ASKING_PHONE

        _state.update {
            it.copy(
                reservation = reservation,
                error = null,
                phoneError = if (rejected) NUMBER_REJECTED else it.phoneError,
                leave = leaveFor(reservation) ?: it.leave
            )
        }
        updateDeadline(previous, step)
    }

    // Si la reserva terminó sin confirmarse no hay nada más que hacer: se vuelve a los horarios con el motivo
    private fun leaveFor(reservation: Reservation): Leave? = when (reservation.status) {
        "EXPIRED" -> Leave("${messages.forCode(RESERVATION_EXPIRED, HTTP_CONFLICT)} $CHOOSE_AGAIN")
        "FAILED" -> Leave("${messages.forFailure(reservation.failureReason)} $CHOOSE_AGAIN")
        "CANCELLED" -> Leave("La reserva se canceló.")
        else -> null
    }

    // El plazo empieza al pasar a esperar a la cátedra y se corta al dejar de esperarla; una lectura que no cambia nada no lo reinicia
    private fun updateDeadline(previous: ReservationStep?, step: ReservationStep) {
        val waiting = step == ReservationStep.WAITING_FOR_REQUEST || step == ReservationStep.WAITING_FOR_RESULT
        val wasWaiting = previous == ReservationStep.WAITING_FOR_REQUEST || previous == ReservationStep.WAITING_FOR_RESULT
        when {
            !waiting -> {
                deadlineJob?.cancel()
                _state.update { it.copy(slow = false) }
            }
            !wasWaiting -> startDeadline()
        }
    }

    private fun startDeadline() {
        deadlineJob?.cancel()
        deadlineJob = viewModelScope.launch {
            delay(WAIT_LIMIT)
            _state.update { it.copy(slow = true) }
        }
    }

    private fun onSendFailed(error: Exception) {
        if ((error as? ApiException)?.code == RESERVATION_EXPIRED) {
            _state.update { it.copy(sending = false, leave = Leave("${messages.forError(error)} $CHOOSE_AGAIN")) }
            return
        }
        _state.update { it.copy(sending = false, phoneError = messages.forError(error)) }
        // Con un conflicto, un corte o un servicio caído el número pudo llegar: se lee la reserva antes de dejar que repita
        if (mayHaveChanged(error)) refresh()
    }

    private fun mayHaveChanged(error: Exception) =
        error !is ApiException || error.code == INVALID_RESERVATION_STATE || error.code == RESERVATION_CONFLICT || error.status == SERVICE_UNAVAILABLE

    private companion object {
        const val RESERVATION_EXPIRED = "RESERVATION_EXPIRED"
        const val INVALID_RESERVATION_STATE = "INVALID_RESERVATION_STATE"
        const val RESERVATION_CONFLICT = "RESERVATION_CONFLICT"
        const val HTTP_CONFLICT = 409
        const val SERVICE_UNAVAILABLE = 503
        const val NUMBER_REJECTED = "La cátedra no aceptó el número. Probá con otro."
        const val CHOOSE_AGAIN = "Elegí el horario de nuevo."

        // La cátedra suele tardar unos segundos; pasado esto conviene ofrecer «Reintentar»
        val WAIT_LIMIT = 42.seconds
    }
}
