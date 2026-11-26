package ar.edu.um.prog2.fzapata.cliente.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.um.prog2.fzapata.cliente.network.UserApi
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.model.LoginRequest
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
    // Un aviso que no es un error: la sesión venció
    val notice: String? = null
)

class LoginViewModel(
    private val api: UserApi,
    private val session: SessionManager,
    private val messages: ErrorMessages
) : ViewModel() {

    // Se lee una vez, al abrirse el login: la app vuelve acá justo después de vaciar la sesión
    private val _state = MutableStateFlow(LoginState(notice = if (session.expired) SESSION_EXPIRED else null))
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onUsernameChange(username: String) = _state.update { it.copy(username = username) }

    fun onPasswordChange(password: String) = _state.update { it.copy(password = password) }

    fun onRememberMeChange(rememberMe: Boolean) = _state.update { it.copy(rememberMe = rememberMe) }

    // Al guardar el token la app cambia de pantalla: no navega este ViewModel
    fun submit() {
        val current = _state.value
        if (current.loading) return

        if (current.username.isBlank() || current.password.isEmpty()) {
            _state.update { it.copy(error = MISSING_DATA) }
            return
        }

        _state.update { it.copy(loading = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val token = api.authenticate(LoginRequest(current.username.trim(), current.password, current.rememberMe))
                session.save(token)
                // La contraseña no queda en el estado de una pantalla que ya no se ve
                _state.update { it.copy(loading = false, password = "") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = messages.forError(e)) }
            }
        }
    }

    companion object {
        const val SESSION_EXPIRED = "La sesión venció: iniciá sesión de nuevo."
        const val MISSING_DATA = "Hay que completar el usuario y la contraseña."
    }
}
