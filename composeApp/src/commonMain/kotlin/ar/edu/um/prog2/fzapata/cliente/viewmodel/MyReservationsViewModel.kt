package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.ReservationStream
import ar.edu.um.prog2.fzapata.cliente.network.StreamEvent
import ar.edu.um.prog2.fzapata.cliente.network.TurnosApi
import ar.edu.um.prog2.fzapata.cliente.network.model.Reservation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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

data class MyReservationsState(
    // El filtro: un estado, o null para todas
    val status: String? = null,
    // En el orden del servicio: de la más lejana a la más antigua
    val reservations: List<Reservation> = emptyList(),
    val total: Int = 0,
    val today: LocalDate,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null
) {
    val hasMore: Boolean get() = reservations.size < total

    val sections: ReservationSections get() = sectionsOf(reservations, today)

    // Con el orden del servicio, las que vienen están todas antes de la primera anterior: hasta no leerla (o no haber más) la sección
    // de las que vienen puede estar incompleta y su orden cambiaría al llegar la página siguiente
    val upcomingComplete: Boolean get() = !hasMore || reservations.any { LocalDate.parse(it.appointmentDate) < today }
}

// Las reservas del usuario, en dos secciones, con filtro por estado. Mientras se mira, el stream la mantiene al día.
@OptIn(ExperimentalTime::class)
class MyReservationsViewModel(
    private val api: TurnosApi,
    private val stream: ReservationStream,
    private val messages: ErrorMessages,
    today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
) : ViewModel() {

    private val _state = MutableStateFlow(MyReservationsState(today = today))
    val state: StateFlow<MyReservationsState> = _state.asStateFlow()

    // La lectura en curso: al cambiar el filtro o llegar un aviso se cancela, para que una respuesta vieja no pise a una nueva
    private var job: Job? = null
    private var nextPage = 0

    init {
        reload(null)
    }

    fun onStatusSelected(status: String?) = reload(status)

    // La pantalla lo pide al llegar al final de la lista. Con un error pendiente no pide solo: espera «Reintentar»
    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.error != null || !current.hasMore) return
        _state.update { it.copy(loadingMore = true) }
        read { fetchPage() }
    }

    fun retry() {
        // Con las que vienen todavía incompletas se empieza de nuevo, para no mostrarlas en un orden que cambiaría
        if (_state.value.reservations.isEmpty() || !_state.value.upcomingComplete) reload(_state.value.status) else {
            _state.update { it.copy(error = null, loadingMore = true) }
            read { fetchPage() }
        }
    }

    // La pantalla lo corre mientras está abierta: cada aviso, y cada reconexión, vuelve a leer lo que ya se cargó
    suspend fun watch() {
        stream.events().collect { event ->
            when (event) {
                // Con una lectura en curso (la primera, por ejemplo) no hace falta otra
                StreamEvent.Connected -> if (job?.isActive != true) reconcile()
                is StreamEvent.Updated -> reconcile()
            }
        }
    }

    // Empieza de nuevo desde la primera página
    private fun reload(status: String?) {
        nextPage = 0
        _state.update { it.copy(status = status, reservations = emptyList(), total = 0, loading = true, loadingMore = false, error = null) }
        read { fillUpcoming() }
    }

    // Cuando se reconecta para volver a leer las páginas que ya se cargaron y reemplaza lo que había
    private fun reconcile() = read(silent = true) {
        val loaded = maxOf(nextPage, 1)
        val pages = (0 until loaded).map { api.reservations(_state.value.status, it) }
        nextPage = loaded
        _state.update { it.copy(reservations = pages.flatMap { page -> page.items }, total = pages.last().total, loadingMore = false, error = null) }
        fillUpcoming(first = false)
    }

    // Una lectura silenciosa que falla deja la lista como estaba y no muestra un error; el stream vuelve a intentarlo al reconectar
    private fun read(silent: Boolean = false, block: suspend () -> Unit) {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                block()
                _state.update { it.copy(loading = false, loadingMore = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, loadingMore = false, error = if (silent && it.reservations.isNotEmpty()) it.error else messages.forError(e))
                }
            }
        }
    }

    // Lee páginas hasta tener todas las que vienen, para mostrarlas en orden desde el principio
    private suspend fun fillUpcoming(first: Boolean = true) {
        if (first) fetchPage()
        while (!_state.value.upcomingComplete && fetchPage()) Unit
    }

    // Devuelve si la página trajo algo: con una vacía no se sigue, aunque el total diga que hay más
    private suspend fun fetchPage(): Boolean {
        val page = api.reservations(_state.value.status, nextPage)
        nextPage++
        _state.update { it.copy(reservations = it.reservations + page.items, total = page.total) }
        return page.items.isNotEmpty()
    }
}
