package ar.edu.um.prog2.fzapata.cliente

import android.app.Application
import ar.edu.um.prog2.fzapata.cliente.config.AppConfig
import ar.edu.um.prog2.fzapata.cliente.session.KeystoreSessionStore

// Vive tanto como el proceso: el cliente y la sesión no se recrean al rotar la pantalla
class ClienteApplication : Application() {

    val container: AppContainer by lazy {
        AppContainer(
            AppConfig(BuildConfig.CATALOG_URL, BuildConfig.TURNOS_URL),
            KeystoreSessionStore(this)
        )
    }
}
