package ar.edu.um.prog2.fzapata.cliente

import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.network.ErrorMessages
import ar.edu.um.prog2.fzapata.cliente.network.HttpClientFactory
import ar.edu.um.prog2.fzapata.cliente.session.SessionManager
import ar.edu.um.prog2.fzapata.cliente.session.SessionStore
import io.ktor.client.HttpClient

// Arma las piezas compartidas a mano: una sola vez por proceso y sin librería de inyección
// val en el constructor hace que se guarde también como propiedad pública
class AppContainer(val config: AppConfig, sessionStore: SessionStore) {

    // El orden importa, primero la session
    val session = SessionManager(sessionStore)
    // Arma el cliente que puede leer y expirar el token de la session
    val httpClient: HttpClient = HttpClientFactory(session).create()
    // ErrorMessages para poder traducir
    val errorMessages = ErrorMessages()
}
