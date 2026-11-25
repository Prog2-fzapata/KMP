package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.UserApi
import ar.edu.um.prog2.fzapata.cliente.network.model.Account
import ar.edu.um.prog2.fzapata.cliente.network.model.UpdateAccountRequest
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.EmailValidator
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.PatientNameValidator
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.fieldErrorsOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileState(
    // La cuenta como está guardada; null hasta que llega. Los campos de abajo son lo que se escribe.
    val saved: Account? = null,
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val loading: Boolean = true,
    val loadError: String? = null,
    val saving: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val justSaved: Boolean = false
) {
    // Se mira lo guardado y no lo escrito: el aviso sigue hasta que el nombre queda en la cuenta
    val missingName: Boolean get() = saved != null && (saved.firstName.isNullOrBlank() || saved.lastName.isNullOrBlank())
}

// Los datos de la cuenta, para verlos y modificarlos
class ProfileViewModel(
    private val api: UserApi,
    private val messages: ErrorMessages,
    private val nameValidator: PatientNameValidator = PatientNameValidator(),
    private val emailValidator: EmailValidator = EmailValidator()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun onFirstNameChange(firstName: String) = edit { it.copy(firstName = firstName) }

    fun onLastNameChange(lastName: String) = edit { it.copy(lastName = lastName) }

    fun onEmailChange(email: String) = edit { it.copy(email = email) }

    // Al escribir se borran los errores y el aviso de guardado: se vuelven a calcular al guardar
    private fun edit(change: (ProfileState) -> ProfileState) =
        _state.update { change(it).copy(fieldErrors = emptyMap(), error = null, justSaved = false) }

    fun save() {
        val current = _state.value
        val saved = current.saved
        if (current.saving || saved == null) return

        val localErrors = nameValidator.validate(current.firstName, current.lastName) + emailValidator.validate(current.email)
        if (localErrors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = localErrors) }
            return
        }

        val firstName = current.firstName.trim()
        val lastName = current.lastName.trim()
        val email = current.email.trim()
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                api.updateAccount(UpdateAccountRequest(firstName, lastName, email))
                _state.update {
                    it.copy(
                        saved = saved.copy(firstName = firstName, lastName = lastName, email = email),
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        saving = false,
                        justSaved = true
                    )
                }
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

    private fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            try {
                val account = api.account()
                _state.update {
                    it.copy(
                        saved = account,
                        firstName = account.firstName.orEmpty(),
                        lastName = account.lastName.orEmpty(),
                        email = account.email,
                        loading = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, loadError = messages.forError(e)) }
            }
        }
    }
}
