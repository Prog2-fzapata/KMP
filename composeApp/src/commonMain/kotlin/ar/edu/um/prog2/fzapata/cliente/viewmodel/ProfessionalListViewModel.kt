package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.CatalogApi
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.model.Professional
import ar.edu.um.prog2.fzapata.cliente.network.model.ProfessionalCategory
import ar.edu.um.prog2.fzapata.cliente.network.model.ProfessionalFilters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfessionalListState(
    val filters: ProfessionalFilters = ProfessionalFilters(),
    // Lo que se escribe en el buscador; recién se aplica al tocar «Buscar»
    val nameInput: String = "",
    val categories: List<ProfessionalCategory> = emptyList(),
    val professionals: List<Professional> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = false,
    val loadingMore: Boolean = false,
    val error: String? = null
) {
    val hasMore: Boolean get() = professionals.size < total
}

class ProfessionalListViewModel(
    private val api: CatalogApi,
    private val messages: ErrorMessages
) : ViewModel() {

    private val _state = MutableStateFlow(ProfessionalListState())
    val state: StateFlow<ProfessionalListState> = _state.asStateFlow()

    // El pedido en curso: al cambiar un filtro se cancela, para que una respuesta lenta y vieja no pise la nueva
    private var job: Job? = null
    private var nextPage = 0

    init {
        reload(ProfessionalFilters())
    }

    fun onNameChange(name: String) = _state.update { it.copy(nameInput = name) }

    fun onSearch() = reload(_state.value.filters.copy(name = _state.value.nameInput))

    fun onCategorySelected(categoryId: Long?) = reload(_state.value.filters.copy(categoryId = categoryId))

    fun onOnlyEnabledChange(onlyEnabled: Boolean) = reload(_state.value.filters.copy(onlyEnabled = onlyEnabled))

    fun onOnlyAvailableChange(onlyAvailable: Boolean) = reload(_state.value.filters.copy(onlyAvailable = onlyAvailable))

    // La pantalla lo pide al llegar al final de la lista. Con un error pendiente no pide solo: espera «Reintentar»
    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.error != null || !current.hasMore) return
        _state.update { it.copy(loadingMore = true) }
        fetch(current.filters)
    }

    fun retry() {
        if (_state.value.professionals.isEmpty()) reload(_state.value.filters) else {
            _state.update { it.copy(error = null, loadingMore = true) }
            fetch(_state.value.filters)
        }
    }

    // Empieza de nuevo desde la primera página
    private fun reload(filters: ProfessionalFilters) {
        job?.cancel()
        nextPage = 0
        _state.update {
            it.copy(filters = filters, professionals = emptyList(), total = 0, loading = true, loadingMore = false, error = null)
        }
        fetch(filters)
    }

    private fun fetch(filters: ProfessionalFilters) {
        job = viewModelScope.launch {
            try {
                // Las categorías se piden una vez, junto con la primera lista
                val categories = _state.value.categories.ifEmpty { api.categories() }
                val page = api.professionals(filters, nextPage)
                nextPage++
                _state.update {
                    it.copy(
                        categories = categories,
                        professionals = it.professionals + page.items,
                        total = page.total,
                        loading = false,
                        loadingMore = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadingMore = false, error = messages.forError(e)) }
            }
        }
    }
}
