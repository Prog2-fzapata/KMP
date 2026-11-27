package ar.edu.um.prog2.fzapata.cliente.session

// Dónde se guarda el JWT entre ejecuciones; cada plataforma decide cómo protegerlo
interface SessionStore {

    fun load(): String?

    fun save(token: String)

    fun clear()
}
