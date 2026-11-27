package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.UserApi
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.model.LoginRequest
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.RegisterValidator
import ar.edu.um.prog2.fzapata.cliente.viewmodel.validation.fieldErrorsOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterState(
    val form: RegisterForm = RegisterForm(),
    val fieldErrors: Map<String, String> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null
)

class RegisterViewModel(
    private val api: UserApi,
    private val session: SessionManager,
    private val messages: ErrorMessages,
    private val validator: RegisterValidator = RegisterValidator()
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()

    // Al escribir se borran los errores: se vuelven a calcular al tocar «Registrarme»
    fun onFormChange(form: RegisterForm) {
        _state.update { it.copy(form = form, fieldErrors = emptyMap(), error = null) }
    }

    // Registra y, si sale bien, inicia sesión con las mismas credenciales: al guardar el token la app cambia de pantalla
    fun submit() {
        val current = _state.value
        if (current.loading) return

        val localErrors = validator.validate(current.form)
        if (localErrors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = localErrors, error = null) }
            return
        }

        _state.update { it.copy(loading = true, fieldErrors = emptyMap(), error = null) }
        viewModelScope.launch {
            var registered = false
            try {
                api.register(current.form.toRequest())
                registered = true
                val token = api.authenticate(LoginRequest(current.form.login.trim(), current.form.password, rememberMe = false))
                session.save(token)
                // La contraseña no queda en el estado de una pantalla que ya no se ve
                _state.update { it.copy(loading = false, form = it.form.copy(password = "")) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val fieldErrors = fieldErrorsOf(e, messages)
                _state.update {
                    it.copy(
                        loading = false,
                        fieldErrors = fieldErrors,
                        error = if (registered) ACCOUNT_CREATED_BUT_NOT_LOGGED_IN else if (fieldErrors.isEmpty()) messages.forError(e) else null
                    )
                }
            }
        }
    }

    companion object {
        // Reintentar el registro daría LOGIN_ALREADY_USED: hay que decir que la cuenta ya existe
        const val ACCOUNT_CREATED_BUT_NOT_LOGGED_IN =
            "La cuenta se creó, pero no se pudo iniciar sesión. Volvé al inicio e ingresá con tus datos."
    }
}
