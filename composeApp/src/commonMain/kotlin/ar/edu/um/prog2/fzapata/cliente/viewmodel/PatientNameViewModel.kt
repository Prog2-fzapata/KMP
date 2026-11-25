package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.UserApi
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.PatientNameValidator
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.fieldErrorsOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PatientNameState(
    val firstName: String = "",
    val lastName: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val error: String? = null
)

// El nombre y el apellido que turnos necesita para anotar el turno
class PatientNameViewModel(
    private val api: UserApi,
    private val messages: ErrorMessages,
    private val validator: PatientNameValidator = PatientNameValidator()
) : ViewModel() {

    private val _state = MutableStateFlow(PatientNameState())
    val state: StateFlow<PatientNameState> = _state.asStateFlow()

    // Al escribir se borran los errores: se vuelven a calcular al guardar
    fun onFirstNameChange(firstName: String) = _state.update { it.copy(firstName = firstName, fieldErrors = emptyMap(), error = null) }

    fun onLastNameChange(lastName: String) = _state.update { it.copy(lastName = lastName, fieldErrors = emptyMap(), error = null) }

    // Guarda en la cuenta y avisa para que la pantalla repita la reserva
    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (current.saving) return

        val localErrors = validator.validate(current.firstName, current.lastName)
        if (localErrors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = localErrors) }
            return
        }

        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                api.updateName(current.firstName.trim(), current.lastName.trim())
                _state.update { it.copy(saving = false) }
                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val fieldErrors = fieldErrorsOf(e, messages)
                _state.update {
                    it.copy(saving = false, fieldErrors = fieldErrors, error = if (fieldErrors.isEmpty()) messages.forError(e) else null)
                }
            }
        }
    }
}
