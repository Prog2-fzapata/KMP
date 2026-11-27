package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Al abrir la app con sesión busca una reserva en curso para volver a ella: quien se fue a mitad de la reserva, o a quien le venció
// la sesión, la sigue donde quedó. El servicio la sigue procesando mientras tanto.
class ResumeViewModel(private val api: TurnosApi) : ViewModel() {

    // La reserva a abrir, o null si no hay ninguna
    private val _reservationId = MutableStateFlow<Long?>(null)
    val reservationId: StateFlow<Long?> = _reservationId.asStateFlow()

    init {
        viewModelScope.launch { _reservationId.value = findInProgress() }
    }

    // La pantalla ya la abrió: al volver a la lista no se abre otra vez
    fun onResumed() {
        _reservationId.value = null
    }

    // La primera en curso de la primera página de reservas. Si no se puede consultar no pasa nada grave: el usuario sigue en la lista
    internal suspend fun findInProgress(): Long? =
        try {
            api.reservations().items.firstOrNull { !ReservationStep.of(it).finished }?.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
