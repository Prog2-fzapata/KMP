package ar.edu.um.prog2.fzapata.cliente.session

import kotlinx.coroutines.flow.MutableStateFlow

// Conoce la sesión e interactúa con el disco
class SessionManager(private val store: SessionStore) {

    // Crea una "caja" con el token al arrancar la app
    private val currentToken = MutableStateFlow(store.load())

    // Lee el token
    fun token(): String? = currentToken.value

    // Actualiza el token
    fun save(token: String) {
        store.save(token) // Guarda en disco
        currentToken.value = token // Actualiza la sesión en memoria
    }
    // Expira el token
    fun expire() {
        store.clear()
        currentToken.value = null
    }
}
